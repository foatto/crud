package foatto.mms.core_mms.ds

import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.geom.XyPoint
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.sql.SQLBatch
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ArnaviHandler : MMSHandler() {

    companion object {

        //    private static TimeZone tz = StringFunction.getTimeZone( 0 );

        //    //--- максимально допустимое время между точками для расчёта пробега
        //    private static final long MAX_POINT_TIME_DIFF = 5 * 60 * 1000;

        //--- вообще-то их 10, но во-первых они полностью никогда не используются,
        //--- во-вторых последние два значения начнут затирать температуру контроллера и гео-данные
        //--- на 18-м и 19-м портах
        //    private static final int MAX_UNIVERSAL_SENSOR_COUNT = 8;
        //    private static final int MAX_485_SENSOR_COUNT = 16;     // 16 х 485-х датчиков уровня/температуры топлива
        //    private static final int MAX_ENERGO_SENSOR_COUNT = 5;

        //--- для основного способа расчета пробега
        private val chmLastTime = ConcurrentHashMap<Int, Int>()
        private val chmLastWGS = ConcurrentHashMap<Int, XyPoint>()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var packetType = 0
    private var packetSize = 0

    private var isCoordOk = false
    private var wgsX = 0
    private var wgsY = 0
    private var isParking = false
    private var speed: Short = 0
    private var run = 0

    private var powerVoltage = 0
    private var accumVoltage = 0
    //    private int controllerTemperature = 0;

    private var bitSensor = 0
    private val tmUniversalSensor = TreeMap<Int, Int>()
    private val tmRS485Fuel = TreeMap<Int, Int>()
    private val tmRS485Temp = TreeMap<Int, Int>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init( aDataServer: CoreDataServer, aSelectionKey: SelectionKey ) {
        deviceType = DEVICE_TYPE_ARNAVI

        super.init( aDataServer, aSelectionKey )
    }

    override fun oneWork( dataWorker: CoreDataWorker ): Boolean {
        //--- тип пакета
        if( packetType == 0 ) {
            //--- ждём стартовый набор данных - тип пакета и размер пакета ( 1 + 2 byte )
            if( bbIn.remaining() < 1 + 2 ) {
                bbIn.compact()
                return true
            }
            packetType = bbIn.getByte().toInt() and 0xFF
            packetSize = bbIn.getShort().toInt() and 0xFFFF
        }

        //--- для D1/2: размер всегда 8 (только IMEI-кода), несмотря на то, что там еще 6 байт (CRC, unix-time, E1/2)
        //--- для Dx: размер начиная с первого CRC включительно и до Ex (т.е. надо ждать packetSize + Ex)
        if( bbIn.remaining() < ( if( packetType == 0xD1 ) 1 + 4 + packetSize + 1 else packetSize + 1 ) ) {
            bbIn.compact()
            return true
        }

        val sqlBatchData = SQLBatch()
        //--- минимальный ответ - 7 байт со всеми заголовками
        val bbOut = AdvancedByteBuffer( 8, byteOrder )

        AdvancedLogger.debug( "remaining at begin = " + bbIn.remaining() )
        AdvancedLogger.debug( "packetType = " + packetType )

        when( packetType ) {

        //--- стартовый пакет - IMEI
            0xD1, 0xD2 -> {
                bbIn.getByte() // skip CRC
                bbIn.getInt()  // skip unix time or itPassword
                deviceID = ( bbIn.getLong() % MMSHandler.DEVICE_ID_DIVIDER ).toInt()
                AdvancedLogger.debug( "deviceID = " + deviceID )

                //--- судя по описанию, идентификационный пакет бывает только раз в начале сессии
                deviceConfig = DeviceConfig.getDeviceConfig( dataWorker.alStm[ 0 ], deviceID )
                //--- неизвестный контроллер
                if( deviceConfig == null ) {
                    writeError( dataWorker.alConn, dataWorker.alStm[ 0 ], "Unknown device ID = "+ deviceID )
                    writeJournal()
                    return false
                }
                sbStatus.append( "ID;" )
                //--- в ответ - unix_time
                bbOut.putInt(getCurrentTimeInt())
            }

        //--- DATA_BIN
            0xD4       -> {
                while(bbIn.remaining() > 1) {
                    bbIn.getByte() // skip CRC
                    val pointTime = bbIn.getInt()

                    val blockCount = bbIn.getByte().toInt() and 0xFF
                    AdvancedLogger.debug("blockCount = " + blockCount)
                    for( i in 0 until blockCount ) {
                        val v = bbIn.getByte().toInt() and 0xFF
                        AdvancedLogger.debug( "var = " + v )
                        when( v ) {
                            1                                  -> {
                                bbIn.getByte() // пропускаем курс
                                bbIn.getByte() // пропускаем высоту
                                bbIn.getByte() // пропускаем спутники
                                speed = roundSpeed( ( bbIn.getByte().toInt() and 0xFF ) * 1.852 )
                            }
                            3                                  -> {
                                accumVoltage = bbIn.getShort().toInt() and 0xFFFF
                                powerVoltage = bbIn.getShort().toInt() and 0xFFFF
                            }
                            4, 5                               -> {
                                bitSensor = bbIn.getByte().toInt() and 0xFF
                                //--- 14 и 15 бит == 1 - кол-во спутников >= 8, что есть норма
                                isCoordOk = bbIn.getByte().toInt() and 0xC0 == 0xC0
                                isParking = bbIn.getByte().toInt() and 0x01 == 0x00
                                bbIn.getByte() // пропускаем менее точное (но всегда включенное) показание бортового напряжения
                            }
                            10                                 -> {
                                val fuelIndex = bbIn.getByte().toInt() and 0xFF  // ) % MAX_485_SENSOR_COUNT;
                                val fuelTemp = bbIn.getByte().toInt()  // без & 0xFF, т.к. температура м.б. отрицательной
                                val fuelLevel = bbIn.getShort().toInt() and 0xFFFF

                                tmRS485Fuel.put( fuelIndex, fuelLevel )
                                tmRS485Temp.put( fuelIndex, fuelTemp )
                            }
                            49                                 -> wgsY = Math.round( bbIn.getFloat() * XyProjection.WGS_KOEF_i )
                            50                                 -> wgsX = Math.round( bbIn.getFloat() * XyProjection.WGS_KOEF_i )
                            96, 97, 98, 99, 100, 101, 102, 103 -> tmUniversalSensor.put( v - 96, bbIn.getInt() )
                        //--- последние два датчика не используем,
                        //--- т.к. во-первых они полностью никогда не используются,
                        //--- во-вторых последние два значения начнут затирать температуру контроллера и гео-данные
                        //--- на 18-м и 19-м портах
                            104, 105                           -> bbIn.getInt()
                        //--- просто пропускаем как неинтересные
                            2, 9                               -> bbIn.getInt()  // skip unused data
                        //--- что-то новенькое, стоит присмотреть в логах
                            else                               -> {
                                bbIn.getInt()  // skip unknown data
                                AdvancedLogger.error( "deviceID = $deviceID\n unknown var = $v" )
                            }
                        }
                    }
                    //--- отсечение повторяющихся/уже обработанных точек и
                    //--- самостоятельный расчёт пробега (относительного, межточечного)
                    val lastTime = chmLastTime[ deviceID ]
                    if( lastTime != null && pointTime <= lastTime ) continue
                    chmLastTime.put( deviceID, pointTime )

                    run = 0
                    if( isCoordOk ) {
                        val lastWGS = chmLastWGS[ deviceID ]
                        val newWGS = XyPoint(wgsX, wgsY)
                        if( lastWGS != null ) run = Math.round( XyProjection.distanceWGS( lastWGS, newWGS ) ).toInt()
                        chmLastWGS.put( deviceID, newWGS )
                    }
                    savePoint( dataWorker, pointTime, sqlBatchData )
                }
                bbOut.putByte( 0x00 )
                bbOut.putByte( 0x01 )
            }

            else       -> {
                bbIn.skip( packetSize )
                AdvancedLogger.error( "deviceID = $deviceID\n unknown packetType = 0x${Integer.toHexString( packetType )}" )
                return false
            }
        }
        AdvancedLogger.debug( "remaining at end = " + bbIn.remaining() )
        //--- байт конца транзакции
        val ex = bbIn.getByte().toInt() and 0xFF
        AdvancedLogger.debug( "ex = 0x" + Integer.toHexString( ex ) )
        sbStatus.append( "DataRead;" )

        for( stm in dataWorker.alStm ) sqlBatchData.execute( stm )

        sendAccept( packetType - 0xD0 + 0xC0, bbOut )

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

        packetType = 0
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun savePoint( dataWorker: CoreDataWorker, pointTime: Int, sqlBatchData: SQLBatch) {
        val curTime = getCurrentTimeInt()
        if( pointTime > curTime - MAX_PAST_TIME && pointTime < curTime + MAX_FUTURE_TIME ) {
            //--- два символа - один бинарный байт, поэтому getTextFieldMaxSize() / 2
            val bbData = AdvancedByteBuffer( dataWorker.alConn[ 0 ].dialect.textFieldMaxSize / 2 )

            putBitSensor( bitSensor, 0, 8, bbData )
            //--- напряжения основного и резервного питаний
            putSensorData( 8, 2, powerVoltage, bbData )
            putSensorData( 9, 2, accumVoltage, bbData )
            //--- универсальные входы (аналоговые/частотные/счётные)
            //--- в отличии от галилео, здесь 4-байтовые значения
            putDigitalSensor( tmUniversalSensor, 10, 4, bbData )
            //--- температура контроллера - не передаётся
            //putSensorData( 18, 2, controllerTemperature, bbData );
            //--- гео-данные
            putSensorPortNumAndDataSize( SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData )
            bbData.putInt( if( isCoordOk ) wgsX else 0 ).putInt( if( isCoordOk ) wgsY else 0 ).putShort( if( isCoordOk && !isParking ) speed else 0 ).putInt( if( isCoordOk ) run else 0 )

            //--- 16 RS485-датчиков уровня топлива, по 2 байта
            putDigitalSensor( tmRS485Fuel, 20, 2, bbData )
            //--- 16 RS485-датчиков температуры, по 4 байта - пишем как int,
            //--- чтобы при чтении не потерялся +- температуры
            putDigitalSensor( tmRS485Temp, 40, 4, bbData )

            addPoint( dataWorker.alStm[ 0 ], pointTime, bbData, sqlBatchData )
            dataCount++
        }
        dataCountAll++
        if( firstPointTime == 0 ) firstPointTime = pointTime
        lastPointTime = pointTime
        //--- массивы данных по датчикам очищаем независимо от записываемости точек
        clearSensorArrays()
    }

    private fun clearSensorArrays() {
        isCoordOk = false
        wgsX = 0
        wgsY = 0
        isParking = false
        speed = 0
        run = 0

        powerVoltage = 0
        accumVoltage = 0
        //controllerTemperature = 0; - не передаётся

        bitSensor = 0
        tmUniversalSensor.clear()
        tmRS485Fuel.clear()
        tmRS485Temp.clear()
    }

    private fun sendAccept( code: Int, bbData: AdvancedByteBuffer ) {
        bbData.flip()

        val bbOut = AdvancedByteBuffer( 5 + bbData.remaining(), byteOrder )

        bbOut.putByte( code )
        bbOut.putShort( bbData.remaining() )
        bbOut.putByte( calcCRC( bbData ) )
        bbOut.put( bbData.buffer )
        bbOut.putByte( code - 0xC0 + 0xE0 )

        outBuf( bbOut )
    }

    //--- unsigned byte в Java нет, будем извращаться через int
    private fun calcCRC( bbData: AdvancedByteBuffer ): Int {
        var crc = 0

        while( bbData.hasRemaining() ) {
            crc += bbData.getByte().toInt() and 0xFF
            crc %= 256
        }

        bbData.rewind()
        return crc
    }
}

