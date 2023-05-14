package foatto.mms.core_mms.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.crc16_modbus
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.CoreNioServer
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.mms.core_mms.ds.nio.MMSNioHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.sql.CoreAdvancedConnection
import foatto.sql.SQLBatch
import io.ktor.util.network.*
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.time.ZoneId
import kotlin.math.roundToInt

open class GalileoHandler : MMSNioHandler() {

    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var packetHeader: Byte = 0
    private var packetSize = 0

    //--- нужны только для отправки команд терминалу
    private lateinit var arrIMEI: ByteArray
    private var terminalId = 0

    private var isCoordOk = false
    private var wgsX = 0
    private var wgsY = 0
    private var isParking = false
    private var speed: Short = 0
    private var absoluteRun = 0

    private var powerVoltage = 0
    private var accumVoltage = 0
    private var controllerTemperature = 0

    private var canFuelLevel = 0
    private var canCoolantTemperature = 0
    private var canEngineRPM = 0

    //--- 8 универсальных датчиков самого галилео
    private val tmUniversalSensor = sortedMapOf<Int, Int>()
    private val tmRS485Fuel = sortedMapOf<Int, Int>()
    private val tmRS485Temp = sortedMapOf<Int, Int>()

    //--- пользовательские данные одиночными значениями
    private val tmUserData = sortedMapOf<Int, Int>()

    //--- по 16 типизированных датчиков от юриковского радиоудлиннителя
    private val tmLevelSensor = sortedMapOf<Int, Int>()
    private val tmVoltageSensor = sortedMapOf<Int, Int>()
    private val tmCountSensor = sortedMapOf<Int, Int>()

    //--- 4 вида счётчиков энергии от сброса (активная прямая, активная обратная, реактивная прямая, реактивная обратная)
    private val tmEnergoCountActiveDirect = sortedMapOf<Int, Int>()
    private val tmEnergoCountActiveReverse = sortedMapOf<Int, Int>()
    private val tmEnergoCountReactiveDirect = sortedMapOf<Int, Int>()
    private val tmEnergoCountReactiveReverse = sortedMapOf<Int, Int>()

    //--- напряжение по фазам
    private val tmEnergoVoltageA = sortedMapOf<Int, Int>()
    private val tmEnergoVoltageB = sortedMapOf<Int, Int>()
    private val tmEnergoVoltageC = sortedMapOf<Int, Int>()

    //--- ток по фазам
    private val tmEnergoCurrentA = sortedMapOf<Int, Int>()
    private val tmEnergoCurrentB = sortedMapOf<Int, Int>()
    private val tmEnergoCurrentC = sortedMapOf<Int, Int>()

    //--- коэффициент мощности по фазам
    private val tmEnergoPowerKoefA = sortedMapOf<Int, Int>()
    private val tmEnergoPowerKoefB = sortedMapOf<Int, Int>()
    private val tmEnergoPowerKoefC = sortedMapOf<Int, Int>()

    //--- energy power (active, reactive, full/summary) by phase by 4 indicators
    private val tmEnergoPowerActiveA = sortedMapOf<Int, Int>()
    private val tmEnergoPowerActiveB = sortedMapOf<Int, Int>()
    private val tmEnergoPowerActiveC = sortedMapOf<Int, Int>()
    private val tmEnergoPowerReactiveA = sortedMapOf<Int, Int>()
    private val tmEnergoPowerReactiveB = sortedMapOf<Int, Int>()
    private val tmEnergoPowerReactiveC = sortedMapOf<Int, Int>()
    private val tmEnergoPowerFullA = sortedMapOf<Int, Int>()
    private val tmEnergoPowerFullB = sortedMapOf<Int, Int>()
    private val tmEnergoPowerFullC = sortedMapOf<Int, Int>()
    private val tmEnergoPowerActiveABC = sortedMapOf<Int, Int>()
    private val tmEnergoPowerReactiveABC = sortedMapOf<Int, Int>()
    private val tmEnergoPowerFullABC = sortedMapOf<Int, Int>()

    //--- массомер ЭМИС
    private val tmEMISDensity = sortedMapOf<Int, Double>()
    private val tmEMISTemperature = sortedMapOf<Int, Double>()
    private val tmEMISMassFlow = sortedMapOf<Int, Double>()
    private val tmEMISVolumeFlow = sortedMapOf<Int, Double>()
    private val tmEMISAccumulatedMass = sortedMapOf<Int, Double>()
    private val tmEMISAccumulatedVolume = sortedMapOf<Int, Double>()

    //--- датчик EuroSense Delta (4 датчика)
    private val tmESDStatus = sortedMapOf<Int, Int>()
    private val tmESDVolume = sortedMapOf<Int, Double>()
    private val tmESDFlow = sortedMapOf<Int, Double>()
    private val tmESDCameraVolume = sortedMapOf<Int, Double>()
    private val tmESDCameraFlow = sortedMapOf<Int, Double>()
    private val tmESDCameraTemperature = sortedMapOf<Int, Int>()
    private val tmESDReverseCameraVolume = sortedMapOf<Int, Double>()
    private val tmESDReverseCameraFlow = sortedMapOf<Int, Double>()
    private val tmESDReverseCameraTemperature = sortedMapOf<Int, Int>()

    //--- датчик УСС
    private val tmUSSAccumulatedVolume = sortedMapOf<Int, Double>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aDataServer: CoreNioServer, aSelectionKey: SelectionKey) {
        deviceType = MMSTelematicFunction.DEVICE_TYPE_GALILEO

        super.init(aDataServer, aSelectionKey)
    }

    override fun oneWork(dataWorker: CoreNioWorker): Boolean {
        //--- магический байт-заголовок
        if (packetHeader.toInt() == 0) {
            if (bbIn.remaining() < 1 + 2) {
                bbIn.compact()
                return true
            }
            packetHeader = bbIn.getByte()
            packetSize = bbIn.getShort().toInt() and 0xFFFF

            //--- старший бит - признак наличия данных в архиве
            //boolean isDataReady = ( packetSize & 0x8000 ) != 0; // накой надо? непонятно...
            //--- длина пакета - остальные/младшие 15 бит
            packetSize = packetSize and 0x7FFF

            if (packetHeader.toInt() != 0x01) {
                MMSTelematicFunction.writeError(
                    conn = dataWorker.conn,
                    dirSessionLog = dirSessionLog,
                    zoneId = zoneId,
                    deviceConfig = deviceConfig,
                    fwVersion = fwVersion,
                    begTime = begTime,
                    address = (selectionKey!!.channel() as SocketChannel).localAddress.hostname,
                    status = status,
                    errorText = "Wrong packet header = $packetHeader",
                    dataCount = dataCount,
                    dataCountAll = dataCountAll,
                    firstPointTime = firstPointTime,
                    lastPointTime = lastPointTime,
                )
                return false
            }
        }

        //--- ждём основные данные + 2 байта CRC
        if (bbIn.remaining() < packetSize + 2) {
            bbIn.compact()
            return true
        }

        var pointTime = 0
        val sqlBatchData = SQLBatch()

        //--- обработка данных, кроме последних 2 байт CRC
        while (bbIn.remaining() > 2) {
            AdvancedLogger.debug("serialNo = $serialNo, remaining = " + bbIn.remaining())
            //--- тег данных
            val tag = bbIn.getByte().toInt() and 0xFF
            AdvancedLogger.debug("tag = ${tag.toString(16)}")
            when (tag) {

                //--- версия прибора/железа
                0x01 -> {
                    val hwVersion = bbIn.getByte().toInt() and 0xFF // хрен знает, что с ней делать
                    AdvancedLogger.debug("hardware version = $hwVersion")
                }

                //--- версия прошивки
                0x02 -> {
                    fwVersion = (bbIn.getByte().toInt() and 0xFF).toString()
                    AdvancedLogger.debug("firmware version = $fwVersion")
                }

                //--- IMEI
                0x03 -> {
                    arrIMEI = ByteArray(15)
                    bbIn.get(arrIMEI)
                    val imei = String(arrIMEI)

                    //--- двойное преобразование подстрока - число - строка, чтобы убрать стартовые нули
                    serialNo = imei.substring(imei.length - 7).toIntOrNull().toString()
                    AdvancedLogger.debug("serialNo = $serialNo")

                    if (!loadDeviceConfig(dataWorker.conn)) {
                        return false
                    }
                }

                //--- нужен только для отправки команды терминалу, обычно он одинаков у всех приборов
                0x04 -> terminalId = bbIn.getShort().toInt() and 0xFFFF

                //--- record No - будем игнорировать, т.к. может и не приходить, а дата/время точки должно приходить по-любому
                0x10 -> bbIn.getShort()    // SKIP record No

                //--- date time
                0x20 -> {
                    //--- если была предыдущая точка, то запишем её
                    if (pointTime != 0) {
                        savePoint(dataWorker.conn, pointTime, sqlBatchData)
                    }
                    //--- даже с учётом игнорирования/обнуления старшего/знакового бита, этого нам хватит еще до 2038 года
                    pointTime = (bbIn.getInt() and 0x7F_FF_FF_FF)
                }

                //--- coords
                0x30 -> {
                    isCoordOk = bbIn.getByte().toInt() and 0xF0 == 0
                    //--- галиеевские int-координаты с точностью 6 цифр после запятой переводим
                    //--- в наши int-координаты с точностью 7 цифр после запятой
                    wgsY = bbIn.getInt() * 10
                    wgsX = bbIn.getInt() * 10
                }

                //--- speed & angle
                0x33 -> {
                    speed = roundSpeed((bbIn.getShort().toInt() and 0xFFFF) / 10.0)
                    bbIn.getShort()    // SKIP angle
                }

                //--- altitude
                0x34 -> bbIn.getShort()    // SKIP altitude

                //--- HDOP
                0x35 -> bbIn.getByte() // SKIP HDOP

                //--- device status - из всех статусов нам пока интересен только статус парковки по уровню вибрации
                0x40 -> isParking = bbIn.getShort().toInt() and 0x0001 == 0

                //--- power voltage
                0x41 -> powerVoltage = bbIn.getShort().toInt() and 0xFFFF

                //--- accum voltage
                0x42 -> accumVoltage = bbIn.getShort().toInt() and 0xFFFF

                //--- controller temperature
                0x43 -> controllerTemperature = bbIn.getByte().toInt()

                //--- acceleration
                0x44 -> bbIn.getInt()

                //--- out status
                0x45 -> bbIn.getShort()

                //--- in status - не совсем понятно - дискретных входов как бы нет, а приходит аж 16 бит
                0x46 -> bbIn.getShort()// & 0xFFFF;

                //--- EcoDrive
                0x47 -> bbIn.getInt()  // SKIP EcoDrive

                //--- Расширенный статус терминала
                0x48 -> bbIn.getShort()

                //--- in voltage / impulse count / impulse frequency
                in 0x50..0x57 -> tmUniversalSensor[tag - 0x50] = bbIn.getShort().toInt() and 0xFFFF

                //--- RS-232
                0x58, 0x59 -> bbIn.getShort()

                //--- показатель счётчика электроэнергии РЭП-500
                0x5A -> bbIn.getInt()  // SKIP

                //--- данные рефрижераторной установки
                0x5B -> {
                    AdvancedLogger.error("serialNo = $serialNo\n unsupported tag = 0x${tag.toString(16)}\n disable refrigerator data, please")
                    return false
                }

                //--- система контроля давления в шинах PressurePro, 34 датчика
                0x5C -> {
                    for (i in 0..33) {
                        bbIn.getShort()
                    }
                }

                //--- Данные дозиметра ДБГ-С11Д
                0x5D -> {
                    bbIn.getShort()
                    bbIn.getByte()
                }

                //--- RS-485 основные/типовые (0..2)
                in 0x60..0x62 -> tmRS485Fuel[tag - 0x60] = bbIn.getShort().toInt() and 0xFFFF

                //--- RS-485 дополнительные/расширенные (с показаниями температуры) (3..7)
                //--- RS-485 дополнительные/расширенные (с показаниями температуры) (8..15)
                in 0x63..0x6F -> {
                    tmRS485Fuel[tag - 0x60] = bbIn.getShort().toInt() and 0xFFFF
                    tmRS485Temp[tag - 0x60] = bbIn.getByte().toInt()
                }

                //--- thermometer
                in 0x70..0x77 -> {
                    bbIn.getByte()  //.toInt() and 0xFF - thermometerID
                    bbIn.getByte()  //.toInt() - value
                }

                //--- Значение на входе 8
                in 0x78..0x79 -> {
                    bbIn.getShort()
                }

                //--- датчик DS1923 (температура и влажность)
                in 0x80..0x87 -> {
                    bbIn.getByte()  //.toInt() and 0xFF - DS_ID
                    bbIn.getByte()  //.toInt() - temp
                    bbIn.getByte()  //.toInt() and 0xFF) * 100 / 255 - humidity
                }

                //--- Расширенные данные RS232[0/1].
                //--- В зависимости от настройки один из вариантов:
                //1. Температура ДУТ, подключенного к нулевому/первому порту RS232, °С.
                //2. Вес, полученный от весового индикатора.
                in 0x88..0x89 -> bbIn.getByte()

                //--- RS-485 основные/типовые (0..2) - показания температуры
                0x8A, 0x8B, 0x8C -> tmRS485Temp[tag - 0x8A] = bbIn.getByte().toInt()

                //--- iButton 0
                0x90 -> bbIn.getInt()

                //--- CAN8BITR16..CAN8BITR31
                in 0xA0..0xAF -> bbIn.getByte()

                //--- CAN16BITR6..CAN16BITR15
                in 0xB0..0xB9 -> bbIn.getShort()

                //--- FMS-Standart: fuel
                0xC0 -> bbIn.getInt()   // / 2).toLong()   //!!! не учитывается, что беззнаковое целое

                //--- CAN
                0xC1 -> {
                    canFuelLevel = ((bbIn.getByte().toInt() and 0xFF) * 0.4f).roundToInt()
                    canCoolantTemperature = (bbIn.getByte().toInt() and 0xFF) - 40
                    canEngineRPM = ((bbIn.getShort().toInt() and 0xFFFF) * 0.125f).roundToInt()
                }

                //--- FMS-Standart: run
                0xC2 -> bbIn.getInt()   // * 5L    //!!! не учитывается, что беззнаковое целое

                //--- CAN_B1
                0xC3 -> bbIn.getInt()

                //--- CAN8BITR0..7 или CAN-LOG, зависит от настроек
                in 0xC4..0xD2 -> bbIn.getByte()

                //--- iButton 1
                0xD3 -> bbIn.getInt()

                //--- absolute/summary run - не учитывается, что беззнаковое целое, однако машине больше 2 млн. км всё равно не пробежать
                0xD4 -> absoluteRun = bbIn.getInt()

                //--- iButton status
                0xD5 -> bbIn.getByte()  //.toInt() and 0xFF

                //--- CAN16BITR0..4 или CAN-LOG, зависит от настроек
                0xD6, 0xD7, 0xD8, 0xD9, 0xDA -> bbIn.getShort()

                //--- CAN32BITR0..4 или CAN-LOG, зависит от настроек
                0xDB, 0xDC, 0xDD, 0xDE, 0xDF -> bbIn.getInt()

                //--- номер команды, на которую пришёл ответ
                0xE0 -> bbIn.getInt()

                //--- ответ на команду
                0xE1 -> {
                    val answerLen = bbIn.getByte().toInt() and 0xFF
                    val arrAnswer = ByteArray(answerLen)
                    bbIn.get(arrAnswer)
                    val answer = String(arrAnswer)
                    status += " AnswerReceive=$answer;"
                    AdvancedLogger.debug("Answer")
                    AdvancedLogger.debug("serialNo = $serialNo\n Answer = $answer")
                }

                //--- пользовательские данные в виде одиночных значений
                in 0xE2..0xE9 -> tmUserData[tag - 0xE2] = bbIn.getInt()

                //--- пользовательские данные
                0xEA -> {
                    //AdvancedLogger.debug( "deviceID = " + deviceID + "\n user data time = " + StringFunction.DateTime_YMDHMS( timeZone, pointTime ) );

                    val userDataSize = bbIn.getByte().toInt() and 0xFF // размер данных
                    AdvancedLogger.debug("serialNo = $serialNo\n userDataSize = $userDataSize")

                    //                StringBuilder sbHex = new StringBuilder( " 0xEA =" );
                    //                for( int i = 0; i < userDataSize; i++ ) {
                    //                    String hex = Integer.toHexString( bbIn.getByte() & 0xFF );
                    //                    sbHex.append( ' ' ).append( hex );  //hex.substring( hex.length() - 2 ) );
                    //                }
                    //                AdvancedLogger.error( sbHex );

                    val userDataType = bbIn.getByte().toInt() and 0xFF  // тип пользовательских данных
                    //AdvancedLogger.debug( "deviceID = " + deviceID + "\n userDataType = " + userDataType );
                    //--- данные от электрического счетчика "Меркурий"
                    if (userDataType == 0x02) {
                        AdvancedLogger.error("serialNo = $serialNo\n Меркурий: электросчётчик напрямую прибором Galileo больше не поддерживается. Используте модуль сбора данных.")
                        bbIn.skip(userDataSize - 1)
                    } else if (userDataType == 0x03) {
                        val dataVersion = bbIn.getByte().toInt() and 0xFF
                        when (dataVersion) {
                            2 -> {
                                //--- далее кусками по 6 байт
                                for (idi in 0 until (userDataSize - 2) / 6) {
                                    //--- данные идут в BigEndian, в отличие от остальных галилео-данных.
                                    //--- чтобы не переключать BigEndian-режим из-за этих данных, проще переставить байты вручную
                                    var b1 = bbIn.getByte()
                                    var b2 = bbIn.getByte()
                                    val id = (b1.toInt() and 0xFF shl 8) or (b2.toInt() and 0xFF)
                                    //int id = bbIn.getShort() & 0xFFFF;

                                    b1 = bbIn.getByte()
                                    b2 = bbIn.getByte()
                                    val b3 = bbIn.getByte()
                                    val b4 = bbIn.getByte()
                                    val value = (b1.toInt() and 0xFF shl 24) or (b2.toInt() and 0xFF shl 16) or (b3.toInt() and 0xFF shl 8) or (b4.toInt() and 0xFF)
                                    //int value = bbIn.getInt();

                                    val double = Float.fromBits(value).toDouble()

                                    when (id) {
                                        0 -> {
                                        } // молча пропускаем пустой 0-й id

                                        in 0x0100..0x010F -> tmLevelSensor[id - 0x0100] = value
                                        in 0x0140..0x014F -> tmVoltageSensor[id - 0x0140] = value
                                        in 0x0180..0x018F -> tmCountSensor[id - 0x0180] = value

                                        in 0x01C1..0x01C4 -> tmEnergoCountActiveDirect[id - 0x01C1] = value
                                        in 0x0201..0x0204 -> tmEnergoCountActiveReverse[id - 0x0201] = value
                                        in 0x0241..0x0244 -> tmEnergoCountReactiveDirect[id - 0x0241] = value
                                        in 0x0281..0x0284 -> tmEnergoCountReactiveReverse[id - 0x0281] = value

                                        in 0x02C1..0x02C4 -> tmEnergoVoltageA[id - 0x02C1] = value
                                        in 0x0301..0x0304 -> tmEnergoVoltageB[id - 0x0301] = value
                                        in 0x0341..0x0344 -> tmEnergoVoltageC[id - 0x0341] = value

                                        in 0x0381..0x0384 -> tmEnergoCurrentA[id - 0x0381] = value
                                        in 0x03C1..0x03C4 -> tmEnergoCurrentB[id - 0x03C1] = value
                                        in 0x0401..0x0404 -> tmEnergoCurrentC[id - 0x0401] = value

                                        in 0x0441..0x0444 -> tmEnergoPowerKoefA[id - 0x0441] = value
                                        in 0x0481..0x0484 -> tmEnergoPowerKoefB[id - 0x0481] = value
                                        in 0x0501..0x0504 -> tmEnergoPowerKoefC[id - 0x0501] = value

                                        in 0x0510..0x0513 -> tmEnergoPowerActiveA[id - 0x0510] = value
                                        in 0x0514..0x0517 -> tmEnergoPowerActiveB[id - 0x0514] = value
                                        in 0x0518..0x051B -> tmEnergoPowerActiveC[id - 0x0518] = value

                                        in 0x051C..0x051F -> tmEnergoPowerReactiveA[id - 0x051C] = value
                                        in 0x0520..0x0523 -> tmEnergoPowerReactiveB[id - 0x0520] = value
                                        in 0x0524..0x0527 -> tmEnergoPowerReactiveC[id - 0x0524] = value

                                        in 0x0528..0x052B -> tmEnergoPowerFullA[id - 0x0528] = value
                                        in 0x052C..0x052F -> tmEnergoPowerFullB[id - 0x052C] = value
                                        in 0x0530..0x0533 -> tmEnergoPowerFullC[id - 0x0530] = value

                                        in 0x0541..0x0544 -> tmEMISMassFlow[id - 0x0541] = double
                                        in 0x0581..0x0584 -> tmEMISDensity[id - 0x0581] = double
                                        in 0x05C1..0x05C4 -> tmEMISTemperature[id - 0x05C1] = double
                                        in 0x0601..0x0604 -> tmEMISVolumeFlow[id - 0x0601] = double
                                        in 0x0641..0x0644 -> tmEMISAccumulatedMass[id - 0x0641] = double
                                        in 0x0681..0x0684 -> tmEMISAccumulatedVolume[id - 0x0681] = double

                                        in 0x06C1..0x06C4 -> tmUSSAccumulatedVolume[id - 0x06C1] = double

                                        in 0x0700..0x0703 -> tmEnergoPowerActiveABC[id - 0x0700] = value
                                        in 0x0710..0x0713 -> tmEnergoPowerReactiveABC[id - 0x0710] = value
                                        in 0x0720..0x0723 -> tmEnergoPowerFullABC[id - 0x0720] = value

                                        else -> AdvancedLogger.error("serialNo = $serialNo\n модуль сбора данных: неизвестный id = ${id.toString(16)}.")
                                    }
                                }
                            }

                            else -> {
                                AdvancedLogger.error("serialNo = $serialNo\n модуль сбора данных: версия $dataVersion больше не поддерживается. Используйте свежий скрипт/прошивку.")
                                return false
                            }
                        }
                    } else if (userDataType == 0x07) {
                        //--- Eurosens Delta
                        for (groupIndex in 0..3) {
                            val esdStatus = bbIn.getByte().toInt() and 0xFF
                            //--- transform EuroSens Delta status bits to universal counter sensor status codes
                            tmESDStatus[groupIndex] = if (esdStatus and 0x20 != 0) {
                                SensorConfigCounter.STATUS_INTERVENTION
                            } else if (esdStatus and 0x10 != 0) {
                                SensorConfigCounter.STATUS_REVERSE
                            } else if (esdStatus and 0x08 != 0) {
                                SensorConfigCounter.STATUS_CHEAT
                            } else if (esdStatus and 0x04 != 0) {
                                SensorConfigCounter.STATUS_OVERLOAD
                            } else if (esdStatus and 0x02 != 0) {
                                SensorConfigCounter.STATUS_NORMAL
                            } else if (esdStatus and 0x01 != 0) {
                                SensorConfigCounter.STATUS_IDLE
                            } else {
                                SensorConfigCounter.STATUS_UNKNOWN
                            }

                            if (esdStatus != 0) {
                                tmESDVolume[groupIndex] = bbIn.getInt() * 0.01                      // litres
                                tmESDFlow[groupIndex] = bbIn.getInt() * 0.1                         // litres/hour
                                tmESDCameraVolume[groupIndex] = bbIn.getInt() * 0.01                // litres
                                tmESDCameraFlow[groupIndex] = bbIn.getInt() * 0.1                   // litres/hour
                                tmESDCameraTemperature[groupIndex] = bbIn.getByte().toInt()         // C
                                tmESDReverseCameraVolume[groupIndex] = bbIn.getInt() * 0.01         // litres
                                tmESDReverseCameraFlow[groupIndex] = bbIn.getInt() * 0.1            // litres/hour
                                tmESDReverseCameraTemperature[groupIndex] = bbIn.getByte().toInt()  // C
                            }
                        }
                    }
                    //--- неизвестные данные, пропускаем их
                    else {
                        AdvancedLogger.error("serialNo = $serialNo\n Неизвестный тип пользовательских данных = $userDataType")
                        bbIn.skip(userDataSize - 1)
                    }
                }

                //--- данные в ответ на команду
                0xEB -> {
                    val answerLen = bbIn.getByte().toInt() and 0xFF
                    val arrAnswer = ByteArray(answerLen)
                    bbIn.get(arrAnswer)
                }

                //--- CAN32BITR6..CAN32BITR15
                in 0xF0..0xF9 -> bbIn.getInt()

                //--- Расширенные теги
                0xFE -> {
                    var extTagDataLen = bbIn.getShort().toInt() and 0xFFFF
                    AdvancedLogger.debug("serialNo = $serialNo\n start extTagDataLen = $extTagDataLen")
                    while (extTagDataLen > 0) {
                        val extTag = bbIn.getShort().toInt() and 0xFFFF
                        extTagDataLen -= 2

                        AdvancedLogger.debug("serialNo = $serialNo\n extTag = 0x${extTag.toString(16)}")
                        AdvancedLogger.debug("serialNo = $serialNo\n middle extTagDataLen = $extTagDataLen")
                        when (extTag) {

                            //--- ModBus 0..31
                            in 0x01..0x20 -> {
                                bbIn.getInt()
                                extTagDataLen -= 4
                            }

                            //--- Bluetooth 0..63
                            in 0x21..0x60 -> {
                                bbIn.getInt()
                                extTagDataLen -= 4
                            }

                            //--- ModBus 32..63
                            in 0x61..0x80 -> {
                                bbIn.getInt()
                                extTagDataLen -= 4
                            }

                            //--- CID, LAC, MCC, MNC
                            in 0x81..0x84 -> {
                                bbIn.getShort()
                                extTagDataLen -= 2
                            }

                            //--- RSSI
                            0x85 -> {
                                bbIn.getByte()
                                extTagDataLen -= 1
                            }

                            //--- Тег расширенного значения датчика температуры
                            in 0x86..0x8D -> {
                                bbIn.getInt()
                                extTagDataLen -= 4
                            }

                            //--- Тег информации о спутниках системы GPS/GLONASS/BAIDOU/GALILEO
                            in 0x8E..0x91 -> {
                                bbIn.getInt()
                                extTagDataLen -= 4
                            }

                            //--- IMSI
                            0x92 -> {
                                val arrIMSI = ByteArray(15)
                                bbIn.get(arrIMSI)
                                extTagDataLen -= 15
                            }

                            //--- Тег номера активной SIM-карты
                            0x93 -> {
                                bbIn.getByte()
                                extTagDataLen -= 1
                            }

                            //--- CCID
                            0x94 -> {
                                val arrCCID = ByteArray(20)
                                bbIn.get(arrCCID)
                                extTagDataLen -= 20
                            }

                            else -> {
                                AdvancedLogger.error("serialNo = $serialNo\n unknown extended tag = 0x${extTag.toString(16)}")
                                return false
                            }
                        }
                        AdvancedLogger.debug("serialNo = $serialNo\n end extTagDataLen = $extTagDataLen")
                    }
                }

                else -> {
                    AdvancedLogger.error("serialNo = $serialNo\n unknown tag = 0x${tag.toString(16)}")
                    return false
                }
            }
        }
        AdvancedLogger.debug("serialNo = $serialNo, before CRC remaining = " + bbIn.remaining())
        val crc = bbIn.getShort()
        AdvancedLogger.debug("serialNo = $serialNo, after CRC remaining = " + bbIn.remaining())

        status += " DataRead;"

        //--- здесь имеет смысл сохранить данные по последней точке, если таковая была считана
        if (pointTime != 0) {
            savePoint(dataWorker.conn, pointTime, sqlBatchData)
        }

        sqlBatchData.execute(dataWorker.conn)

        sendAccept(crc)

        //--- проверка на наличие команды терминалу

        deviceConfig?.let { dc ->
            val (cmdID, cmdStr) = MMSTelematicFunction.getCommand(dataWorker.conn, dc.deviceId)

            //--- команда есть
            if (cmdStr != null) {
                //--- и она не пустая
                if (cmdStr.isNotEmpty()) {
                    val dataSize = 1 + 15 + 1 + 2 + 1 + 4 + 1 + 1 + cmdStr.length

                    //!!! точно BigEndian???
                    val bbOut = AdvancedByteBuffer(64)  // 64 байта в большинстве случаев хватает

                    bbOut.putByte(0x01)
                    bbOut.putShort(dataSize)

                    bbOut.putByte(0x03)
                    bbOut.put(arrIMEI)

                    bbOut.putByte(0x04)
                    bbOut.putShort(terminalId)

                    bbOut.putByte(0xE0)
                    bbOut.putInt(0)

                    bbOut.putByte(0xE1)
                    bbOut.putByte(cmdStr.length)
                    bbOut.put(cmdStr.toByteArray())

                    //--- кто бы мог подумать: CRC отправляется в big-endian, хотя сами данные приходят в little-endian
                    bbOut.putShort(crc16_modbus(bbOut.array(), bbOut.arrayOffset(), dataSize + 3, true))

                    outBuf(bbOut)
                }
                //--- отметим успешную отправку команды
                MMSTelematicFunction.setCommandSended(dataWorker.conn, cmdID)
                status += " CommandSend;"
            }
        }

        //--- данные успешно переданы - теперь можно завершить транзакцию
        status += " Ok;"
        errorText = ""
        deviceConfig?.let { dc ->
            MMSTelematicFunction.writeSession(
                conn = dataWorker.conn,
                dirSessionLog = dirSessionLog,
                zoneId = zoneId,
                deviceConfig = dc,
                fwVersion = fwVersion,
                begTime = begTime,
                address = selectionKey?.let { sk ->
                    (sk.channel() as SocketChannel).remoteAddress.toString() + " -> " + (sk.channel() as SocketChannel).localAddress.toString()
                } ?: "(unknown remote address)",
                status = status,
                errorText = errorText,
                dataCount = dataCount,
                dataCountAll = dataCountAll,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
                isOk = true,
            )
        }
        //--- для возможного режима постоянного/длительного соединения
        bbIn.clear()   // других данных быть не должно, именно .clear(), а не .compact()
        begTime = 0
        status = ""
        dataCount = 0
        dataCountAll = 0
        firstPointTime = 0
        lastPointTime = 0

        packetHeader = 0
        return true
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun savePoint(conn: CoreAdvancedConnection, pointTime: Int, sqlBatchData: SQLBatch) {
        val curTime = getCurrentTimeInt()
        AdvancedLogger.debug("pointTime = ${DateTime_YMDHMS(ZoneId.systemDefault(), pointTime)}")
        if (pointTime > curTime - CoreTelematicFunction.MAX_PAST_TIME && pointTime < curTime + CoreTelematicFunction.MAX_FUTURE_TIME) {
            val bbData = AdvancedByteBuffer(conn.dialect.textFieldMaxSize / 2)

            //--- напряжения основного и резервного питаний
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 8, 2, powerVoltage, bbData)
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 9, 2, accumVoltage, bbData)
            //--- универсальные входы (аналоговые/частотные/счётные)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmUniversalSensor, 10, 2, bbData)
            //--- температура контроллера
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 18, 1, controllerTemperature, bbData)
            //--- гео-данные
            CoreTelematicFunction.putSensorPortNumAndDataSize(deviceConfig!!.index, SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData)
            bbData.putInt(if (isCoordOk) wgsX else 0).putInt(if (isCoordOk) wgsY else 0)
                .putShort(if (isCoordOk && !isParking) speed else 0).putInt(if (isCoordOk) absoluteRun else 0)

            //--- 16 RS485-датчиков уровня топлива, по 2 байта
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmRS485Fuel, 20, 2, bbData)

            //--- CAN: уровень топлива в %
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 36, 1, canFuelLevel, bbData)
            //--- CAN: температура охлаждающей жидкости - сохраняется в виде 4 байт,
            //--- чтобы сохранить знак числа, не попадая под переделку в unsigned short в виде & 0xFFFF
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 37, 4, canCoolantTemperature, bbData)
            //--- CAN: обороты двигателя, об/мин
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 38, 2, canEngineRPM, bbData)

            //--- 39-й порт пока свободен

            //--- 16 RS485-датчиков температуры, по 4 байта - пишем как int,
            //--- чтобы при чтении не потерялся +- температуры
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmRS485Temp, 40, 4, bbData)

            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmUserData, 100, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmCountSensor, 110, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmLevelSensor, 120, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmVoltageSensor, 140, 4, bbData)

            //--- данные по электросчётчику ---

            //--- значения счётчиков от последнего сброса (активная/реактивная прямая/обратная)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoCountActiveDirect, GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_DIRECT, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoCountActiveReverse, GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_REVERSE, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoCountReactiveDirect, GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_DIRECT, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoCountReactiveReverse, GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_REVERSE, 4, bbData)

            //--- напряжение по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoVoltageA, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_A, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoVoltageB, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_B, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoVoltageC, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_C, 4, bbData)

            //--- ток по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoCurrentA, GalileoFunction.PORT_NUM_MERCURY_CURRENT_A, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoCurrentB, GalileoFunction.PORT_NUM_MERCURY_CURRENT_B, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoCurrentC, GalileoFunction.PORT_NUM_MERCURY_CURRENT_C, 4, bbData)

            //--- коэффициент мощности по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerKoefA, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_A, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerKoefB, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_B, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerKoefC, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_C, 4, bbData)

            //--- активная мощность по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerActiveA, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_A, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerActiveB, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_B, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerActiveC, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_C, 4, bbData)

            //--- реактивная мощность по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerReactiveA, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_A, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerReactiveB, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_B, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerReactiveC, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_C, 4, bbData)

            //--- полная мощность по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerFullA, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_A, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerFullB, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_B, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerFullC, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_C, 4, bbData)

            //--- ЭМИС
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEMISMassFlow, GalileoFunction.PORT_NUM_EMIS_MASS_FLOW, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEMISDensity, GalileoFunction.PORT_NUM_EMIS_DENSITY, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEMISTemperature, GalileoFunction.PORT_NUM_EMIS_TEMPERATURE, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEMISVolumeFlow, GalileoFunction.PORT_NUM_EMIS_VOLUME_FLOW, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEMISAccumulatedMass, GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_MASS, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEMISAccumulatedVolume, GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_VOLUME, bbData)

            //--- мощность по трём фазам: активная, реактивная, суммарная
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerActiveABC, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_ABC, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerReactiveABC, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_ABC, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmEnergoPowerFullABC, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_ABC, 4, bbData)

            //--- УСС
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmUSSAccumulatedVolume, GalileoFunction.PORT_NUM_USS_ACCUMULATED_VOLUME, bbData)

            //--- EuroSens Delta
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDStatus, GalileoFunction.PORT_NUM_ESD_STATUS, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDVolume, GalileoFunction.PORT_NUM_ESD_VOLUME, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDFlow, GalileoFunction.PORT_NUM_ESD_FLOW, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDCameraVolume, GalileoFunction.PORT_NUM_ESD_CAMERA_VOLUME, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDCameraFlow, GalileoFunction.PORT_NUM_ESD_CAMERA_FLOW, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDCameraTemperature, GalileoFunction.PORT_NUM_ESD_CAMERA_TEMPERATURE, 4, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDReverseCameraVolume, GalileoFunction.PORT_NUM_ESD_REVERSE_CAMERA_VOLUME, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDReverseCameraFlow, GalileoFunction.PORT_NUM_ESD_REVERSE_CAMERA_FLOW, bbData)
            CoreTelematicFunction.putDigitalSensor(deviceConfig!!.index, tmESDReverseCameraTemperature, GalileoFunction.PORT_NUM_ESD_REVERSE_CAMERA_TEMPERATURE, 4, bbData)

            MMSTelematicFunction.addPoint(conn, deviceConfig!!, pointTime, bbData, sqlBatchData)
            dataCount++
        }
        dataCountAll++
        if (firstPointTime == 0) {
            firstPointTime = pointTime
        }
        lastPointTime = pointTime
        //--- массивы данных по датчикам очищаем независимо от записываемости точек
        clearSensorArrays()
    }

    private fun sendAccept(crc: Short) {
        //--- буфер для ответа - достаточно 3 байт, но кеширование работает начиная с 4 байт
        val bbOut = AdvancedByteBuffer(4, byteOrder)

        bbOut.putByte(0x02)
        bbOut.putShort(crc)

        outBuf(bbOut)
    }

    private fun clearSensorArrays() {
        isCoordOk = false
        wgsX = 0
        wgsY = 0
        isParking = false
        speed = 0
        absoluteRun = 0

        powerVoltage = 0
        accumVoltage = 0
        controllerTemperature = 0

        canFuelLevel = 0
        canCoolantTemperature = 0
        canEngineRPM = 0

        tmUniversalSensor.clear()
        tmRS485Fuel.clear()
        tmRS485Temp.clear()

        tmUserData.clear()

        tmLevelSensor.clear()
        tmVoltageSensor.clear()
        tmCountSensor.clear()

        tmEnergoCountActiveDirect.clear()
        tmEnergoCountActiveReverse.clear()
        tmEnergoCountReactiveDirect.clear()
        tmEnergoCountReactiveReverse.clear()

        tmEnergoVoltageA.clear()
        tmEnergoVoltageB.clear()
        tmEnergoVoltageC.clear()

        tmEnergoCurrentA.clear()
        tmEnergoCurrentB.clear()
        tmEnergoCurrentC.clear()

        tmEnergoPowerKoefA.clear()
        tmEnergoPowerKoefB.clear()
        tmEnergoPowerKoefC.clear()

        tmEnergoPowerActiveA.clear()
        tmEnergoPowerActiveB.clear()
        tmEnergoPowerActiveC.clear()
        tmEnergoPowerReactiveA.clear()
        tmEnergoPowerReactiveB.clear()
        tmEnergoPowerReactiveC.clear()
        tmEnergoPowerFullA.clear()
        tmEnergoPowerFullB.clear()
        tmEnergoPowerFullC.clear()
        tmEnergoPowerActiveABC.clear()
        tmEnergoPowerReactiveABC.clear()
        tmEnergoPowerFullABC.clear()

        tmEMISMassFlow.clear()
        tmEMISDensity.clear()
        tmEMISTemperature.clear()
        tmEMISVolumeFlow.clear()
        tmEMISAccumulatedMass.clear()
        tmEMISAccumulatedVolume.clear()

        tmESDStatus.clear()
        tmESDVolume.clear()
        tmESDFlow.clear()
        tmESDCameraVolume.clear()
        tmESDCameraFlow.clear()
        tmESDCameraTemperature.clear()
        tmESDReverseCameraVolume.clear()
        tmESDReverseCameraFlow.clear()
        tmESDReverseCameraTemperature.clear()

        tmUSSAccumulatedVolume.clear()
    }

}
