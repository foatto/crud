package foatto.mms.core_mms.ds

import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.geom.XyPoint
import foatto.sql.SQLBatch
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getZoneId
import foatto.core_server.ds.CoreDataWorker
import foatto.mms.core_mms.sensor.SensorConfig
import java.nio.ByteOrder
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class WialonIPSHandler : MMSHandler() {

    companion object {
        //    private static TimeZone tz = StringFunction.getTimeZone( 0 );

        //    //--- максимально допустимое время между точками для расчёта пробега
        //    private static final int MAX_POINT_TIME_DIFF = 5 * 60;

        //--- для основного способа расчета пробега
        private val chmLastTime = ConcurrentHashMap<Int, Int>()
        private val chmLastWGS = ConcurrentHashMap<Int, XyPoint>()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val sbData = StringBuilder()
    private var protocolVersion = 0

    private var tmLLSFuel: TreeMap<Int, Int> = TreeMap()

    //--- в общем-то всё равно, т.к. принимаются/передаются передаются строки,
    //--- но при передаче упакованных данных может пригодится
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork(dataWorker: CoreDataWorker): Boolean {
        //        //--- данных нет - ждём
        //        if( ! bbIn.hasRemaining() ) {
        //            bbIn.compact();
        //            return true;
        //        }

        val arrByte = ByteArray( bbIn.remaining() )
        bbIn.get( arrByte )
        sbData.append( String( arrByte ) )

        //--- первый символ - д.б. #
        if( sbData[ 0 ] != '#' ) {
            AdvancedLogger.error( "Wrong data = " + sbData )
            return false
        }

        //--- минимальный размер пакета = 5 символов/байт
        if( sbData.length < 5 ) {
            bbIn.compact()
            return true
        }

        //--- хотя бы один пакет собрался в общей строке данных?
        val packetEndPos = sbData.indexOf( "\r\n", 0 )
        if( packetEndPos < 0 ) {
            bbIn.compact()
            return true
        }

        AdvancedLogger.debug( "data = " + sbData )

        val sqlBatchData = SQLBatch()
        val answer: String?

        //--- разбиваем строку на параметры без начальной # и \r\n в конце строки
        val secondDelimiterPos = sbData.indexOf( "#", 1 )
        val packetType = sbData.substring( 1, secondDelimiterPos )
        val packetData = sbData.substring( secondDelimiterPos + 1, packetEndPos )
        AdvancedLogger.debug( "packetType = " + packetType )
        AdvancedLogger.debug( "packetData = " + packetData )
        //--- убираем разобранный пакет из начала общей строки данных
        sbData.delete( 0, packetEndPos + 2 )
        AdvancedLogger.debug( "rest data = " + sbData )

        //--- пинговый пакет
        if( packetType == "P" ) answer = "#AP#\r\n"
        else if( packetType == "L" ) {
            val st = StringTokenizer( packetData, ";" )
            val countTokens = st.countTokens()
            val id: String
            if( countTokens == 2 ) {
                protocolVersion = 1
                id = st.nextToken()
            }
            else if( countTokens == 4 ) {
                val sProtocolVersion = st.nextToken()
                if( sProtocolVersion == "2.0" ) {
                    protocolVersion = 2
                    id = st.nextToken()
                }
                else {
                    AdvancedLogger.error( "Wrong protocol version = " + packetData )
                    return false
                }
            }
            else {
                AdvancedLogger.error( "Wrong protocol version = " + packetData )
                return false
            }
            deviceID = Integer.parseInt( if( id.length <= 7 ) id else id.substring( id.length - 7 ) )
            AdvancedLogger.debug( "deviceID = " + deviceID )

            if( !loadDeviceConfig( dataWorker ) ) return false
            answer = "#AL#1\r\n"
        }
        else if( packetType == "SD" || packetType == "D" || packetType == "B" ) {
            val stBlackPacket = StringTokenizer( packetData, "|" )
            var pCount = 0
            while( stBlackPacket.hasMoreTokens() ) {
                pCount++   // считаем в любом случае, а то перепошлют "недостающие" точки
                val stSubPacket = StringTokenizer( stBlackPacket.nextToken(), ";" )
                val countTokens = stSubPacket.countTokens()
                //--- неполные пакеты пропускаем (в т.ч. последний пакет с CRC16 во второй версии протокола)
                if( countTokens != 10 && countTokens != 11 && countTokens != 16 && countTokens != 17 ) continue

                val date = stSubPacket.nextToken()
                val time = stSubPacket.nextToken()
                val lat = stSubPacket.nextToken()
                val latSign = stSubPacket.nextToken()
                val lon = stSubPacket.nextToken()
                val lonSign = stSubPacket.nextToken()
                val speedStr = stSubPacket.nextToken()
                //--- пока пропускаем
                stSubPacket.nextToken()   // course
                stSubPacket.nextToken()   // height
                val satCount = Integer.parseInt( stSubPacket.nextToken() )
                var paramStr: String? = null
                //--- этих данных может и не быть, предварительно проверяем наличие
                if( countTokens == 16 || countTokens == 17 ) {
                    stSubPacket.nextToken()   // hdop
                    stSubPacket.nextToken()   // inputs
                    stSubPacket.nextToken()   // outputs
                    stSubPacket.nextToken()   // adc
                    stSubPacket.nextToken()   // ibutton
                    paramStr = stSubPacket.nextToken()  // params (где и прячется LLS)
                }

                //--- если все основные данные заполнены правильно
                if( date != "NA" && time != "NA" ) {
                    val da = Integer.parseInt( date.substring( 0, 2 ) )
                    val mo = Integer.parseInt( date.substring( 2, 4 ) )
                    val ye = Integer.parseInt( date.substring( 4, 6 ) ) + 2000
                    val ho = Integer.parseInt( time.substring( 0, 2 ) )
                    val mi = Integer.parseInt( time.substring( 2, 4 ) )
                    val se = Integer.parseInt( time.substring( 4, 6 ) )
                    val pointTime = ZonedDateTime.of(ye, mo, da, ho, mi, se, 0, getZoneId(0)).toEpochSecond().toInt()

                    //--- пропускаем точки с ранее обработанным (дублирующимся) временем
                    val lastTime = chmLastTime[ deviceID ]
                    if( lastTime != null && pointTime <= lastTime ) continue
                    chmLastTime.put( deviceID, pointTime )

                    var wgsX = 0
                    var wgsY = 0
                    if( satCount > 0 && lat != "NA" && latSign != "NA" && lon != "NA" && lonSign != "NA" ) {
                        //--- предварительное преобразование
                        val tmpY = lat.toDouble()
                        val tmpX = lon.toDouble()
                        //--- вычленяем градусную часть (которая * 100)
                        //--- 5453.5542;N;05220.0107;E
                        val grY = Math.floor( tmpY / 100 )
                        val grX = Math.floor( tmpX / 100 )
                        //--- а остаток - минуты - делим на 60
                        wgsY = Math.round( ( grY + ( tmpY - grY * 100 ) / 60 ) * XyProjection.WGS_KOEF_d ).toInt() * if( latSign == "N" ) 1 else -1
                        wgsX = Math.round( ( grX + ( tmpX - grX * 100 ) / 60 ) * XyProjection.WGS_KOEF_d ).toInt() * if( lonSign == "E" ) 1 else -1
                    }
                    var speed = if( speedStr == "NA" ) 0 else speedStr.toInt()
                    //--- коррекция скорости - при неизвестных/некорректных координатах правильной скорости быть не может
                    if( wgsX == 0 && wgsY == 0 ) speed = 0
                    //--- коррекция скорости - скорость около 1 км/ч - это плясание около стоянки
                    if( speed <= 1 ) speed = 0
                    //--- самостоятельный расчёт пробега (относительного, межточечного)
                    var run = 0
                    if( wgsX != 0 && wgsY != 0 && speed > 0 ) {
                        val lastWGS = chmLastWGS[ deviceID ]
                        val newWGS = XyPoint(wgsX, wgsY)
                        if( lastWGS != null ) run = Math.round( XyProjection.distanceWGS( lastWGS, newWGS ) ).toInt()
                        chmLastWGS.put( deviceID, newWGS )
                    }

                    //--- ловим данные с уровнемера
                    tmLLSFuel.clear()
                    if( paramStr != null ) {
                        val stParams = StringTokenizer( paramStr, "," )
                        while(stParams.hasMoreTokens()) {
                            val paramPart = stParams.nextToken()
                            //--- это данные с LLS, например LLS1:1:508
                            if( paramPart.startsWith( "LLS" ) ) {
                                val stLLS = StringTokenizer( paramPart, ":" )
                                val llsIndex = Integer.parseInt( stLLS.nextToken().substring( 3 ) ) - 1
                                stLLS.nextToken()  // пропускаем тип данных - он (пока) всегда int
                                val llsData = stLLS.nextToken().toInt()

                                tmLLSFuel.put( llsIndex, llsData )
                            }
                        }
                    }
                    savePoint( dataWorker, pointTime, wgsX, wgsY, speed, run, tmLLSFuel, sqlBatchData )
                }
            }
            //--- во второй версии протокола в последнем пакете приходит CRC16 вместо данных по точке
            if( protocolVersion == 2 ) pCount--
            answer = "#A$packetType#$pCount\r\n"
        }
        else if(packetType == "M") {
            answer = "#AM#1\r\n"
        }
        else {
            AdvancedLogger.error( "deviceID = $deviceID\n unknown packetType = $packetType" )
            return false
        }//--- сообщение от/для водителя - просто пропускаем
        //--- сокращённый, полный и архивный ("чёрный ящик") пакеты с данными
        // v1.0: #SD#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats\r\n
        // v2.0: #SD#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats;crc16\r\n
        // v1.0: #D#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats;hdop;inputs;outputs;adc;ibutton;params\r\n
        // v2.0: #D#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats;hdop;inputs;outputs;adc;ibutton;params;crc16\r\n
        // v1.0: #B#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats|
        //          date;time;lat1;lat2;lon1;lon2;speed;course;height;sats|
        //          date;time;lat1;lat2;lon1;lon2;speed;course;height;sats\r\n
        // v2.0: #B#date;time;lat1;lat2;lon1;lon2;speed;course;height;sats|
        //          date;time;lat1;lat2;lon1;lon2;speed;course;height;sats|
        //          date;time;lat1;lat2;lon1;lon2;speed;course;height;sats|crc16\r\n
        //--- идентификатор прибора
        // v1.0: #L#imei;itPassword\r\n
        // v2.0: #L#protocol_version;imei;itPassword;crc16\r\n
        sbStatus.append( "DataRead;" )

        for( stm in dataWorker.alStm ) sqlBatchData.execute( stm )

        //--- отправка ответа
        val bbOut = AdvancedByteBuffer( answer.length, byteOrder )
        bbOut.put( answer.toByteArray() )
        outBuf( bbOut )

        //--- данные успешно переданы - теперь можно завершить транзакцию
        sbStatus.append( "Ok;" )
        errorText = null
        writeSession( dataWorker.alConn, dataWorker.alStm[ 0 ], true )

        //--- для возможного режима постоянного/длительного соединения
        bbIn.compact()     // нельзя .clear(), т.к. копятся данные следующего пакета

        begTime = 0
        sbStatus.setLength( 0 )
        dataCount = 0
        dataCountAll = 0
        firstPointTime = 0
        lastPointTime = 0

        //--- это накопительная строка данных, очищается в процессе разбора
        //sbData.setLength( 0 );
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun savePoint( dataWorker: CoreDataWorker, pointTime: Int, wgsX: Int, wgsY: Int, speed: Int, run: Int, aTmLLSFuel: TreeMap<Int, Int>, sqlBatchData: SQLBatch) {
        val curTime = getCurrentTimeInt()
        if( pointTime > curTime - MAX_PAST_TIME && pointTime < curTime + MAX_FUTURE_TIME ) {
            //--- два символа - один бинарный байт, поэтому getTextFieldMaxSize() / 2
            val bbData = AdvancedByteBuffer( dataWorker.alConn[ 0 ].dialect.textFieldMaxSize / 2 )

            //            putBitSensor( bitSensor, 0, 8, bbData );
            //            //--- напряжения основного и резервного питаний
            //            putSensorData( 8, 2, powerVoltage, bbData );
            //            putSensorData( 9, 2, accumVoltage, bbData );
            //            //--- универсальные входы (аналоговые/частотные/счётные)
            //            //--- в отличии от галилео, здесь 4-байтовые значения
            //            if( arrUniversalSensor != null ) putDigitalSensor( arrUniversalSensor, 10, 4, bbData );
            //--- температура контроллера - не передаётся
            //putSensorData( 18, 2, controllerTemperature, bbData );
            //--- гео-данные
            putSensorPortNumAndDataSize( SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData )
            bbData.putInt( wgsX ).putInt( wgsY ).putShort( speed ).putInt( run )

            //--- N RS485-датчиков уровня топлива, по 2 байта
            putDigitalSensor( aTmLLSFuel, 20, 2, bbData )
            //            //--- 16 RS485-датчиков температуры, по 4 байта - пишем как int,
            //            //--- чтобы при чтении не потерялся +- температуры
            //            if( arrRS485Temp != null ) putDigitalSensor( arrRS485Temp, 40, 4, bbData );

            //            //--- пока 5 счётчиков Меркурий, и различнейшие данные по ним
            //            //--- значения счётчиков от последнего сброса (активная/реактивная прямая/обратная)
            //            if( arrEnergoCount != null ) putDigitalSensor( arrEnergoCount, 80, 4, bbData );
            //            //--- значения мощности (активная/реактивная суммарная и пофазно)
            //            if( arrEnergoPower != null ) putDigitalSensor( arrEnergoPower, 100, 4, bbData );

            addPoint( dataWorker.alStm[ 0 ], pointTime, bbData, sqlBatchData )
            dataCount++
        }
        dataCountAll++
        if( firstPointTime == 0 ) firstPointTime = pointTime
        lastPointTime = pointTime
        //        //--- массивы данных по датчикам очищаем независимо от записываемости точек
        //        clearSensorArrays();
    }

}
    //    private void clearSensorArrays() throws Throwable {
    //        isCoordOk = false;
    //        wgsX = 0;
    //        wgsY = 0;
    //        isParking = false;
    //        speed = 0;
    //        run = 0;
    //
    //        powerVoltage = 0;
    //        accumVoltage = 0;
    //        //controllerTemperature = 0; - не передаётся
    //
    //        bitSensor = 0;
    //        arrUniversalSensor = null;
    //        arrRS485Fuel = null;
    //        arrRS485Temp = null;
    //
    ////        arrEnergoCount = null;
    ////        arrEnergoPower = null;
    //    }
    //
