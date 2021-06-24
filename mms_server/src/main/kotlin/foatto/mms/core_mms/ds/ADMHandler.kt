package foatto.mms.core_mms.ds

import foatto.core.app.xy.XyProjection
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.sql.SQLBatch
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.charset.Charset
import java.time.ZoneId
import java.util.*

class ADMHandler : MMSHandler() {

    private var packetHeader = 0
    private var packetSize = 0

    private var replyMode = 0

    private var isCoordOk = false
    private var wgsX = 0
    private var wgsY = 0
    private var isParking = false
    private var speed: Short = 0
    private var absoluteRun = 0

    private var powerVoltage = 0
    private var accumVoltage = 0

    private val tmUniversalSensor = TreeMap<Int, Int>()
    private val tmRS485Fuel = TreeMap<Int, Int>()
    private val tmRS485Temp = TreeMap<Int, Int>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aDataServer: CoreDataServer, aSelectionKey: SelectionKey) {
        deviceType = DEVICE_TYPE_ADM

        super.init(aDataServer, aSelectionKey)
    }

    override fun oneWork(dataWorker: CoreDataWorker): Boolean {
        if (packetHeader == 0) {
            if (bbIn.remaining() < 2 + 1) {
                bbIn.compact()
                return true
            }
            packetHeader = bbIn.getShort().toInt() // номер устройства - не используется
            packetSize = bbIn.getByte().toInt() and 0xFF
            AdvancedLogger.debug("packetHeader = $packetHeader")
            AdvancedLogger.debug("packetSize = $packetSize")
        }

        //--- ждём основные данные - 3 байта (в packetSize входят также 2 байта заголовка и сам байт размера пакета)
        if (bbIn.remaining() < packetSize - 3) {
            bbIn.compact()
            return true
        }

        var pointTime = 0
        val sqlBatchData = SQLBatch()

        //--- обработка данных
        //        while( bbIn.remaining() > 0 ) {
        //AdvancedLogger.debug( "remaining = " + bbIn.remaining() );

        //--- беcтиповой пакет специального размера - ответ терминала на команду от сервера
        if (packetSize == 0x84) {
            val arrAnswer = ByteArray(129)
            bbIn.get(arrAnswer)
            val answer = String(arrAnswer, Charset.forName("UTF-8"))
            AdvancedLogger.debug("answer = " + answer)
        } else {
            //--- тип пакета
            val packetType = bbIn.getByte().toInt() and 0xFF
            AdvancedLogger.debug("packetType = " + Integer.toHexString(packetType))

            if (packetType == 0x03) {
                val arrIMEI = ByteArray(15)
                bbIn.get(arrIMEI)
                val imei = String(arrIMEI, Charset.forName("UTF-8"))

                deviceID = Integer.parseInt(imei.substring(imei.length - 7))
                AdvancedLogger.debug("deviceID = " + deviceID)

                if (!loadDeviceConfig(dataWorker)) return false

                //--- недокументированная хрень
                val hwType = bbIn.getByte().toInt() and 0xFF
                AdvancedLogger.debug("hwType = " + hwType)

                replyMode = bbIn.getByte().toInt() and 0xFF
                AdvancedLogger.debug("replyMode = " + replyMode)
                if (replyMode >= 0x02) {
                    AdvancedLogger.error("deviceID = $deviceID\n unsupported reply mode = 0x${Integer.toHexString(packetType)}")
                    return false
                }

                bbIn.skip(44)
                bbIn.getByte() // skip CRC
            } else if (packetType == 0x01) {
                AdvancedLogger.error("deviceID = $deviceID\n unsupported ADM-5 packetType = 0x${Integer.toHexString(packetType)}")
                return false
            } else if (packetType == 0x0A) {
                AdvancedLogger.error("deviceID = $deviceID\n unsupported photo data packetType = 0x${Integer.toHexString(packetType)}")
                return false
            } else {
                fwVersion = bbIn.getByte().toInt() and 0xFF
                AdvancedLogger.debug("firmware version = " + fwVersion)

                bbIn.getShort()    // SKIP номер пакета

                val deviceStatus = bbIn.getShort().toInt() and 0xFFFF

                isCoordOk = deviceStatus and 0x20 == 0
                isParking = deviceStatus and 0x40 != 0
                val isGPSAntennaBreak = deviceStatus and 0x0200 != 0
                val isGPSAntennaShortCircuit = deviceStatus and 0x0400 != 0
                AdvancedLogger.debug("isCoordOk = " + isCoordOk)
                AdvancedLogger.debug("isParking = " + isParking)
                AdvancedLogger.debug("isGPSAntennaBreak = " + isGPSAntennaBreak)
                AdvancedLogger.debug("isGPSAntennaShortCircuit = " + isGPSAntennaShortCircuit)

                wgsY = Math.round(bbIn.getFloat() * XyProjection.WGS_KOEF_i)
                wgsX = Math.round(bbIn.getFloat() * XyProjection.WGS_KOEF_i)
                AdvancedLogger.debug("wgsY = " + wgsY)
                AdvancedLogger.debug("wgsX = " + wgsX)

                bbIn.getShort()    // SKIP course
                speed = roundSpeed((bbIn.getShort().toInt() and 0xFFFF) / 10.0)
                AdvancedLogger.debug("speed = " + speed)

                bbIn.getByte()     // SKIP acceleration
                bbIn.getShort()    // SKIP height
                bbIn.getByte()     // SKIP HDOP

                var satCount = bbIn.getByte().toInt()
                //--- старшие 4 бита - кол-во ГЛОНАСС-спутников, младшие 4 бита - кол-во GPS-спутников, берём максимальное значение
                satCount = Math.max(satCount and 0xF0 shr 4, satCount and 0x0F)
                AdvancedLogger.debug("satCount = " + satCount)
                pointTime = bbIn.getInt()
                //--- ERRATA
                if (pointTime > getCurrentTimeInt()) pointTime -= 2_678_400
                AdvancedLogger.debug("pointTime = " + DateTime_YMDHMS(ZoneId.systemDefault(), pointTime))

                powerVoltage = bbIn.getShort().toInt() and 0xFFFF
                accumVoltage = bbIn.getShort().toInt() and 0xFFFF
                AdvancedLogger.debug("powerVoltage = " + powerVoltage)
                AdvancedLogger.debug("accumVoltage = " + accumVoltage)

                if (packetType and 0x04 != 0) {
                    bbIn.getByte()     // SKIP vibration level
                    bbIn.getByte()     // SKIP vibration count
                    bbIn.getByte()     // SKIP out status
                    bbIn.getByte()     // SKIP in event status
                }

                //--- аналоговые входы
                if (packetType and 0x08 != 0) for (i in 0..5) tmUniversalSensor.put(i, bbIn.getShort().toInt() and 0xFFFF)

                //--- импульсные/счётные входы
                if (packetType and 0x10 != 0) for (i in 0..1) tmUniversalSensor.put(6 + i, bbIn.getInt())

                //--- уровнемеры
                if (packetType and 0x20 != 0) {
                    for (i in 0..2) tmRS485Fuel.put(i, bbIn.getShort().toInt() and 0xFFFF)
                    for (i in 0..2) tmRS485Temp.put(i, bbIn.getByte().toInt())
                }

                //--- CAN-шина
                if (packetType and 0x40 != 0) {
                    AdvancedLogger.error("deviceID = $deviceID\n unsupported CAN-data packetType = 0x${Integer.toHexString(packetType)}")
                    return false
                }

                //--- виртуальный одометр
                if (packetType and 0x80 != 0) absoluteRun = bbIn.getInt()
            }
        }

        //AdvancedLogger.debug( "remaining = " + bbIn.remaining() );
        sbStatus.append("DataRead;")

        //--- здесь имеет смысл сохранить данные по последней точке, если таковая была считана
        savePoint(dataWorker, pointTime, sqlBatchData)

        sqlBatchData.execute(dataWorker.stm)

        //        sendAccept( crc );

        //--- данные успешно переданы - теперь можно завершить транзакцию
        sbStatus.append("Ok;")
        errorText = null
        writeSession(dataWorker.conn, dataWorker.stm, true)

        //--- для возможного режима постоянного/длительного соединения
        bbIn.compact()     // нельзя .clear(), т.к. копятся данные следующего пакета

        begTime = 0
        sbStatus.setLength(0)
        dataCount = 0
        dataCountAll = 0
        firstPointTime = 0
        lastPointTime = 0

        packetHeader = 0
        return true
    }

    private fun savePoint(dataWorker: CoreDataWorker, pointTime: Int, sqlBatchData: SQLBatch) {
        val curTime = getCurrentTimeInt()
        if (pointTime > curTime - MAX_PAST_TIME && pointTime < curTime + MAX_FUTURE_TIME) {
            val bbData = AdvancedByteBuffer(dataWorker.conn.dialect.textFieldMaxSize / 2)

            //--- напряжения основного и резервного питаний
            putSensorData(8, 2, powerVoltage, bbData)
            putSensorData(9, 2, accumVoltage, bbData)
            //--- универсальные входы (аналоговые/частотные/счётные)
            putDigitalSensor(tmUniversalSensor, 10, 2, bbData)
            //--- гео-данные
            putSensorPortNumAndDataSize(SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData)
            bbData.putInt(if (isCoordOk) wgsX else 0).putInt(if (isCoordOk) wgsY else 0).putShort(if (isCoordOk && !isParking) speed else 0).putInt(if (isCoordOk) absoluteRun else 0)

            //--- 16 RS485-датчиков уровня топлива, по 2 байта
            putDigitalSensor(tmRS485Fuel, 20, 2, bbData)
            //--- 16 RS485-датчиков температуры, по 4 байта - пишем как int,
            //--- чтобы при чтении не потерялся +- температуры
            putDigitalSensor(tmRS485Temp, 40, 4, bbData)

            addPoint(dataWorker.stm, pointTime, bbData, sqlBatchData)
            dataCount++
        }
        dataCountAll++
        if (firstPointTime == 0) firstPointTime = pointTime
        lastPointTime = pointTime
        //--- массивы данных по датчикам очищаем независимо от записываемости точек
        clearSensorArrays()
    }

    //    private void sendAccept( short crc ) throws Throwable {
    //        if( replyMode == 0x02 ) {
    //            //--- буфер для ответа - достаточно 3 байт, но кеширование работает начиная с 4 байт
    //            AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 4, getByteOrder() );
    //
    //            bbOut.putByte( 0x02 );
    //            bbOut.putShort( crc );
    //
    //            bbOut.flip();
    //
    //            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
    //            while( bbOut.hasRemaining() ) socketChannel.write( bbOut.getBuffer() );
    //        }
    //    }

    private fun clearSensorArrays() {
        isCoordOk = false
        wgsX = 0
        wgsY = 0
        isParking = false
        speed = 0
        absoluteRun = 0

        powerVoltage = 0
        accumVoltage = 0
        //        controllerTemperature = 0;

        tmUniversalSensor.clear()
        tmRS485Fuel.clear()
        tmRS485Temp.clear()
    }

}
