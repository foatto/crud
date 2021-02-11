package foatto.mms.core_mms.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.crc16_modbus
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.sql.SQLBatch
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt

open class GalileoHandler : MMSHandler() {

    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var packetHeader: Byte = 0
    private var packetSize = 0

    //--- нужны только для отправки команд терминалу
    private lateinit var arrIMEI: ByteArray
    private var terminalID = 0

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
    private val tmUniversalSensor = TreeMap<Int, Int>()
    private val tmRS485Fuel = TreeMap<Int, Int>()
    private val tmRS485Temp = TreeMap<Int, Int>()

    //--- пользовательские данные одиночными значениями
    private val tmUserData = TreeMap<Int, Int>()

    //--- по 16 типизированных датчиков от юриковского радиоудлиннителя
    private val tmLevelSensor = TreeMap<Int, Int>()
    private val tmVoltageSensor = TreeMap<Int, Int>()
    private val tmCountSensor = TreeMap<Int, Int>()

    //--- 4 вида счётчиков энергии от сброса (активная прямая, активная обратная, реактивная прямая, реактивная обратная)
    private val tmEnergoCountActiveDirect = TreeMap<Int, Int>()
    private val tmEnergoCountActiveReverse = TreeMap<Int, Int>()
    private val tmEnergoCountReactiveDirect = TreeMap<Int, Int>()
    private val tmEnergoCountReactiveReverse = TreeMap<Int, Int>()

    //--- напряжение по фазам
    private val tmEnergoVoltageA = TreeMap<Int, Int>()
    private val tmEnergoVoltageB = TreeMap<Int, Int>()
    private val tmEnergoVoltageC = TreeMap<Int, Int>()

    //--- ток по фазам
    private val tmEnergoCurrentA = TreeMap<Int, Int>()
    private val tmEnergoCurrentB = TreeMap<Int, Int>()
    private val tmEnergoCurrentC = TreeMap<Int, Int>()

    //--- коэффициент мощности по фазам
    private val tmEnergoPowerKoefA = TreeMap<Int, Int>()
    private val tmEnergoPowerKoefB = TreeMap<Int, Int>()
    private val tmEnergoPowerKoefC = TreeMap<Int, Int>()

    //--- energy power (active, reactive, full/summary) by phase by 4 indicators
    private val tmEnergoPowerActiveA = TreeMap<Int, Int>()
    private val tmEnergoPowerActiveB = TreeMap<Int, Int>()
    private val tmEnergoPowerActiveC = TreeMap<Int, Int>()
    private val tmEnergoPowerReactiveA = TreeMap<Int, Int>()
    private val tmEnergoPowerReactiveB = TreeMap<Int, Int>()
    private val tmEnergoPowerReactiveC = TreeMap<Int, Int>()
    private val tmEnergoPowerFullA = TreeMap<Int, Int>()
    private val tmEnergoPowerFullB = TreeMap<Int, Int>()
    private val tmEnergoPowerFullC = TreeMap<Int, Int>()
    private val tmEnergoPowerActiveABC = TreeMap<Int, Int>()
    private val tmEnergoPowerReactiveABC = TreeMap<Int, Int>()
    private val tmEnergoPowerFullABC = TreeMap<Int, Int>()

    //--- плотность
    private val tmDensity = TreeMap<Int, Double>()

    //--- температура
    private val tmTemperature = TreeMap<Int, Double>()

    //--- массовый расход
    private val tmMassFlow = TreeMap<Int, Double>()

    //--- объёмный расход
    private val tmVolumeFlow = TreeMap<Int, Double>()

    //--- накопленная масса
    private val tmAccumulatedMass = TreeMap<Int, Double>()

    //--- накопленный объём
    private val tmAccumulatedVolume = TreeMap<Int, Double>()

    //--- датчик EuroSense Delta (4 датчика)
    private val tmESDStatus = TreeMap<Int, Int>()
    private val tmESDVolume = TreeMap<Int, Double>()
    private val tmESDFlow = TreeMap<Int, Double>()
    private val tmESDCameraVolume = TreeMap<Int, Double>()
    private val tmESDCameraFlow = TreeMap<Int, Double>()
    private val tmESDCameraTemperature = TreeMap<Int, Int>()
    private val tmESDReverseCameraVolume = TreeMap<Int, Double>()
    private val tmESDReverseCameraFlow = TreeMap<Int, Double>()
    private val tmESDReverseCameraTemperature = TreeMap<Int, Int>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected open val isIridium: Boolean = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aDataServer: CoreDataServer, aSelectionKey: SelectionKey) {
        deviceType = DEVICE_TYPE_GALILEO

        super.init(aDataServer, aSelectionKey)
    }

    override fun oneWork(dataWorker: CoreDataWorker): Boolean {
        //--- Iridium-заголовок
        if (isIridium) {
            //--- в данном случае - версия протокола
            if (packetHeader.toInt() == 0) {
                //--- будем ждать загрузки максимально полного заголовка
                //--- (если он не полный - то "непрочитанный" остаток безопасно уйдёт в полезную нагрузку,
                //--- т.к. опциональная часть заголовка много меньше полезной нагрузки)
                if (bbIn.remaining() < 1 + 2 + 1 + 2 + 4 + 15 + 1 + 2 + 2 + 4 + 1 + 2 + 1 + 1 + 2 + 1 + 2 + 4 + 1 + 2) {
                    bbIn.compact()
                    return true
                }
                packetHeader = bbIn.getByte()  // версия протокола
                bbIn.getShort()    // общая длина пакета
                bbIn.getByte()     // Тэг 0х01
                bbIn.getShort()    // размер данных тэга
                bbIn.getInt()      // ID пакета
                val arrIridiumIMEI = ByteArray(15)
                bbIn.get(arrIridiumIMEI)    // IMEI
                val iridiumIMEI = String(arrIridiumIMEI)
                val iridiumStatusSession = bbIn.getByte().toInt()
                bbIn.getShort()    // номер пакета
                bbIn.getShort()    // пустое поле
                bbIn.getInt()      // время отправки пакета

                if (packetHeader.toInt() != 0x01) {
                    writeError(dataWorker.alConn, dataWorker.alStm[0], "Wrong Iridium protocol version = $packetHeader for IMEI = $iridiumIMEI")
                    return false
                }
                if (iridiumStatusSession < 0 || iridiumStatusSession > 2) {
                    writeError(dataWorker.alConn, dataWorker.alStm[0], "Wrong Iridium session status = $iridiumStatusSession for IMEI = $iridiumIMEI")
                    return false
                }

                var tag = bbIn.getByte().toInt()
                //--- опциональный тэг
                if (tag == 0x03) {
                    bbIn.getShort()    // размер данных тэга
                    bbIn.getByte()     // флаги
                    bbIn.getByte()     // широта - градусы
                    bbIn.getShort()    // широта - минуты с точностью до тысячных
                    bbIn.getByte()     // долгота - градусы
                    bbIn.getShort()    // долгота - минуты с точностью до тысячных
                    bbIn.getInt()      // CEP радиус

                    tag = bbIn.getByte().toInt()
                }
                //--- основной тэг
                if (tag == 0x02) {
                    //--- данные в Iridium-заголовке идут в BidEndian, в отличие от остальных галилео-данных.
                    //--- чтобы не переключать BidEndian-режим из-за одного только размера пакета,
                    //--- проще у него самого байты переставить
                    val b1 = bbIn.getByte()
                    val b2 = bbIn.getByte()
                    packetSize = (b1.toInt() and 0xFF shl 8) or (b2.toInt() and 0xFF)
                } else {
                    writeError(dataWorker.alConn, dataWorker.alStm[0], "Unknown Iridium tag = $tag for IMEI = $iridiumIMEI")
                    return false
                }
            }
        }
        //--- Стандартный заголовок
        else {
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
                    writeError(dataWorker.alConn, dataWorker.alStm[0], "Wrong packet header = $packetHeader for device ID = $deviceID")
                    return false
                }
            }
        }

        //--- ждём основные данные + 2 байта CRC (0, если данные передавались через iridium, там не передается CRC)
        if (bbIn.remaining() < packetSize + (if (isIridium) 0 else 2)) {
            bbIn.compact()
            return true
        }

        var pointTime = 0
        val sqlBatchData = SQLBatch()

        //        //??? очищать после каждой записи точки
        //        int[] arrIButton = new int[ 2 ];

        //--- обработка данных, кроме последних 2 байт CRC
        //--- (0, если данные передавались через iridium, там не передается CRC)
        while (bbIn.remaining() > if (isIridium) 0 else 2) {
            //AdvancedLogger.debug( "remaining = " + bbIn.remaining() );
            //--- тег данных
            val tag = bbIn.getByte().toInt() and 0xFF
            //AdvancedLogger.debug( "tag = " + Integer.toHexString( tag ) );
            when (tag) {

                //--- версия прибора/железа
                0x01 -> {
                    val hwVersion = bbIn.getByte().toInt() and 0xFF // хрен знает, что с ней делать
                    AdvancedLogger.debug("hardware version = " + hwVersion)
                }

                //--- версия прошивки
                0x02 -> {
                    fwVersion = bbIn.getByte().toInt() and 0xFF
                    AdvancedLogger.debug("firmware version = " + fwVersion)
                }

                //--- IMEI
                0x03 -> {
                    arrIMEI = ByteArray(15)
                    bbIn.get(arrIMEI)
                    val imei = String(arrIMEI)

                    deviceID = Integer.parseInt(imei.substring(imei.length - 7))
                    AdvancedLogger.debug("deviceID = " + deviceID)

                    if (!loadDeviceConfig(dataWorker)) return false
                }

                //--- нужен только для отправки команды терминалу, обычно он одинаков у всех приборов
                0x04 -> terminalID = bbIn.getShort().toInt() and 0xFFFF

                //--- record No - будем игнорировать, т.к. может и не приходить, а дата/время точки должно приходить по-любому
                0x10 -> bbIn.getShort()    // SKIP record No

                //--- date time
                0x20 -> {
                    //--- если была предыдущая точка, то запишем её
                    if (pointTime != 0) savePoint(dataWorker, pointTime, sqlBatchData)
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

                //--- in voltage / impulse count / impulse frequency
                in 0x50..0x57 -> tmUniversalSensor[tag - 0x50] = bbIn.getShort().toInt() and 0xFFFF

                //--- RS-232
                0x58, 0x59 -> bbIn.getShort()

                //--- показатель счётчика электроэнергии РЭП-500
                0x5A -> bbIn.getInt()  // SKIP

                //--- данные рефрижераторной установки
                0x5B -> {
                    AdvancedLogger.error("deviceID = $deviceID\n unsupported tag = 0x${Integer.toHexString(tag)}\n disable refrigerator data, please")
                    return false
                }

                //--- система контроля давления в шинах PressurePro, 34 датчика
                0x5C -> for (i in 0..33) bbIn.getShort()

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

                //--- датчик DS1923 (температура и влажность)
                in 0x80..0x87 -> {
                    bbIn.getByte()  //.toInt() and 0xFF - DS_ID
                    bbIn.getByte()  //.toInt() - temp
                    bbIn.getByte()  //.toInt() and 0xFF) * 100 / 255 - humidity
                }

                //--- Температура ДУТ, подключенного к нулевому порту RS232, С
                0x88 -> bbIn.getByte()

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
                    sbStatus.append("AnswerReceive=").append(answer).append(';')
                    AdvancedLogger.debug("Answer")
                    AdvancedLogger.debug("deviceID = $deviceID\n Answer = $answer")
                }

                //--- пользовательские данные в виде одиночных значений
                in 0xE2..0xE9 -> tmUserData[tag - 0xE2] = bbIn.getInt()

                //--- пользовательские данные
                0xEA -> {
                    //AdvancedLogger.debug( "deviceID = " + deviceID + "\n user data time = " + StringFunction.DateTime_YMDHMS( timeZone, pointTime ) );

                    val userDataSize = bbIn.getByte().toInt() and 0xFF // размер данных
                    AdvancedLogger.debug("deviceID = $deviceID\n userDataSize = $userDataSize")

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
                        AdvancedLogger.error("deviceID = $deviceID\n Меркурий: электросчётчик напрямую прибором Galileo больше не поддерживается. Используте модуль сбора данных.")
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

                                    AdvancedLogger.error("id = ${id.toString(16)}, value = $value / 0x${value.toString(16)}, float = $double")

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

                                        in 0x0541..0x0544 -> tmMassFlow[id - 0x0541] = double
                                        in 0x0581..0x0584 -> tmDensity[id - 0x0581] = double
                                        in 0x05C1..0x05C4 -> tmTemperature[id - 0x05C1] = double
                                        in 0x0601..0x0604 -> tmVolumeFlow[id - 0x0601] = double
                                        in 0x0641..0x0644 -> tmAccumulatedMass[id - 0x0641] = double
                                        in 0x0681..0x0684 -> tmAccumulatedVolume[id - 0x0681] = double

                                        in 0x0700..0x0703 -> tmEnergoPowerActiveABC[id - 0x0700] = value
                                        in 0x0710..0x0713 -> tmEnergoPowerReactiveABC[id - 0x0710] = value
                                        in 0x0720..0x0723 -> tmEnergoPowerFullABC[id - 0x0720] = value

                                        else -> AdvancedLogger.error("deviceID = $deviceID\n модуль сбора данных: неизвестный id = ${id.toString(16)}.")
                                    }
                                }
                            }
                            else -> {
                                AdvancedLogger.error("deviceID = $deviceID\n модуль сбора данных: версия $dataVersion больше не поддерживается. Используйте свежий скрипт/прошивку.")
                                return false
                            }
                        }
                    } else if (userDataType == 0x07) {
                        //--- Eurosens Delta
                        for (groupIndex in 0..3) {
                            val esdStatus = bbIn.getByte().toInt() and 0xFF
                            //--- transform EuroSens Delta status bits to universal counter sensor status codes
                            tmESDStatus[groupIndex] = if (esdStatus and 0x20 != 0) SensorConfigCounter.STATUS_INTERVENTION
                            else if (esdStatus and 0x10 != 0) SensorConfigCounter.STATUS_REVERSE
                            else if (esdStatus and 0x08 != 0) SensorConfigCounter.STATUS_CHEAT
                            else if (esdStatus and 0x04 != 0) SensorConfigCounter.STATUS_OVERLOAD
                            else if (esdStatus and 0x02 != 0) SensorConfigCounter.STATUS_NORMAL
                            else if (esdStatus and 0x01 != 0) SensorConfigCounter.STATUS_IDLE
                            else SensorConfigCounter.STATUS_UNKNOWN

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
                        AdvancedLogger.error("deviceID = $deviceID\n Неизвестный тип пользовательских данных = $userDataType")
                        bbIn.skip(userDataSize - 1)
                    }
                }

                //--- CAN32BITR6..CAN32BITR15
                0xF0, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9 -> bbIn.getInt()

                else -> {
                    AdvancedLogger.error("deviceID = $deviceID\n unknown tag = 0x${Integer.toHexString(tag)}")
                    return false
                }
            }
        }
        //--- при передаче через iridium crc-код возвращать не надо
        val crc = if (isIridium) 0 else bbIn.getShort()
        //AdvancedLogger.debug( "remaining = " + bbIn.remaining() );
        sbStatus.append("DataRead;")

        //--- здесь имеет смысл сохранить данные по последней точке, если таковая была считана
        if (pointTime != 0) savePoint(dataWorker, pointTime, sqlBatchData)

        for (stm in dataWorker.alStm) sqlBatchData.execute(stm)

        //--- при передаче через iridium отвечать не надо
        if (!isIridium) sendAccept(crc)

        //--- проверка на наличие команды терминалу

        val (cmdID, cmdStr) = getCommand(dataWorker.alStm[0], deviceID)

        //--- команда есть
        if (cmdStr != null) {
            //--- и она не пустая
            if (cmdStr.isNotEmpty()) {
                val dataSize = 1 + 15 + 1 + 2 + 1 + 4 + 1 + 1 + cmdStr.length

                val bbOut = AdvancedByteBuffer(64)  // 64 байта в большинстве случаев хватает

                bbOut.putByte(0x01)
                bbOut.putShort(dataSize)

                bbOut.putByte(0x03)
                bbOut.put(arrIMEI)

                bbOut.putByte(0x04)
                bbOut.putShort(terminalID)

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
            setCommandSended(dataWorker.alStm[0], cmdID)
            sbStatus.append("CommandSend;")
        }

        //--- данные успешно переданы - теперь можно завершить транзакцию
        sbStatus.append("Ok;")
        errorText = null
        writeSession(dataWorker.alConn, dataWorker.alStm[0], true)

        //--- для возможного режима постоянного/длительного соединения
        bbIn.clear()   // других данных быть не должно, именно .clear(), а не .compact()
        begTime = 0
        sbStatus.setLength(0)
        dataCount = 0
        dataCountAll = 0
        firstPointTime = 0
        lastPointTime = 0

        packetHeader = 0
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun savePoint(dataWorker: CoreDataWorker, pointTime: Int, sqlBatchData: SQLBatch) {
        val curTime = getCurrentTimeInt()
        AdvancedLogger.debug("pointTime = ${DateTime_YMDHMS(ZoneId.systemDefault(), pointTime)}")
        if (pointTime > curTime - MAX_PAST_TIME && pointTime < curTime + MAX_FUTURE_TIME) {
            val bbData = AdvancedByteBuffer(dataWorker.alConn[0].dialect.textFieldMaxSize / 2)

            //--- напряжения основного и резервного питаний
            putSensorData(8, 2, powerVoltage, bbData)
            putSensorData(9, 2, accumVoltage, bbData)
            //--- универсальные входы (аналоговые/частотные/счётные)
            putDigitalSensor(tmUniversalSensor, 10, 2, bbData)
            //--- температура контроллера
            putSensorData(18, 2, controllerTemperature, bbData)
            //--- гео-данные
            putSensorPortNumAndDataSize(SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData)
            bbData.putInt(if (isCoordOk) wgsX else 0).putInt(if (isCoordOk) wgsY else 0)
                .putShort(if (isCoordOk && !isParking) speed else 0).putInt(if (isCoordOk) absoluteRun else 0)

            //--- 16 RS485-датчиков уровня топлива, по 2 байта
            putDigitalSensor(tmRS485Fuel, 20, 2, bbData)

            //--- CAN: уровень топлива в %
            putSensorData(36, 1, canFuelLevel, bbData)
            //--- CAN: температура охлаждающей жидкости - сохраняется в виде 4 байт,
            //--- чтобы сохранить знак числа, не попадая под переделку в unsigned short в виде & 0xFFFF
            putSensorData(37, 4, canCoolantTemperature, bbData)
            //--- CAN: обороты двигателя, об/мин
            putSensorData(38, 2, canEngineRPM, bbData)

            //--- 39-й порт пока свободен

            //--- 16 RS485-датчиков температуры, по 4 байта - пишем как int,
            //--- чтобы при чтении не потерялся +- температуры
            putDigitalSensor(tmRS485Temp, 40, 4, bbData)

            putDigitalSensor(tmUserData, 100, 4, bbData)
            putDigitalSensor(tmCountSensor, 110, 4, bbData)
            putDigitalSensor(tmLevelSensor, 120, 4, bbData)
            putDigitalSensor(tmVoltageSensor, 140, 4, bbData)

            //--- данные по электросчётчику ---

            //--- значения счётчиков от последнего сброса (активная/реактивная прямая/обратная)
            putDigitalSensor(tmEnergoCountActiveDirect, 160, 4, bbData)
            putDigitalSensor(tmEnergoCountActiveReverse, 164, 4, bbData)
            putDigitalSensor(tmEnergoCountReactiveDirect, 168, 4, bbData)
            putDigitalSensor(tmEnergoCountReactiveReverse, 172, 4, bbData)

            //--- напряжение по фазам A1..4, B1..4, C1..4
            putDigitalSensor(tmEnergoVoltageA, 180, 4, bbData)
            putDigitalSensor(tmEnergoVoltageB, 184, 4, bbData)
            putDigitalSensor(tmEnergoVoltageC, 188, 4, bbData)

            //--- ток по фазам A1..4, B1..4, C1..4
            putDigitalSensor(tmEnergoCurrentA, 200, 4, bbData)
            putDigitalSensor(tmEnergoCurrentB, 204, 4, bbData)
            putDigitalSensor(tmEnergoCurrentC, 208, 4, bbData)

            //--- коэффициент мощности по фазам A1..4, B1..4, C1..4
            putDigitalSensor(tmEnergoPowerKoefA, 220, 4, bbData)
            putDigitalSensor(tmEnergoPowerKoefB, 224, 4, bbData)
            putDigitalSensor(tmEnergoPowerKoefC, 228, 4, bbData)

            //--- активная мощность по фазам A1..4, B1..4, C1..4
            putDigitalSensor(tmEnergoPowerActiveA, 232, 4, bbData)
            putDigitalSensor(tmEnergoPowerActiveB, 236, 4, bbData)
            putDigitalSensor(tmEnergoPowerActiveC, 240, 4, bbData)

            //--- реактивная мощность по фазам A1..4, B1..4, C1..4
            putDigitalSensor(tmEnergoPowerReactiveA, 244, 4, bbData)
            putDigitalSensor(tmEnergoPowerReactiveB, 248, 4, bbData)
            putDigitalSensor(tmEnergoPowerReactiveC, 252, 4, bbData)

            //--- полная мощность по фазам A1..4, B1..4, C1..4
            putDigitalSensor(tmEnergoPowerFullA, 256, 4, bbData)
            putDigitalSensor(tmEnergoPowerFullB, 260, 4, bbData)
            putDigitalSensor(tmEnergoPowerFullC, 264, 4, bbData)

            putDigitalSensor(tmMassFlow, 270, bbData)
            putDigitalSensor(tmDensity, 280, bbData)
            putDigitalSensor(tmTemperature, 290, bbData)
            putDigitalSensor(tmVolumeFlow, 300, bbData)
            putDigitalSensor(tmAccumulatedMass, 310, bbData)
            putDigitalSensor(tmAccumulatedVolume, 320, bbData)

            //--- мощность по трём фазам: активная, реактивная, суммарная
            putDigitalSensor(tmEnergoPowerActiveABC, 330, 4, bbData)
            putDigitalSensor(tmEnergoPowerReactiveABC, 340, 4, bbData)
            putDigitalSensor(tmEnergoPowerFullABC, 350, 4, bbData)

            //--- EuroSens Delta
            putDigitalSensor(tmESDStatus, 500, 4, bbData)
            putDigitalSensor(tmESDVolume, 504, bbData)
            putDigitalSensor(tmESDFlow, 508, bbData)
            putDigitalSensor(tmESDCameraVolume, 512, bbData)
            putDigitalSensor(tmESDCameraFlow, 516, bbData)
            putDigitalSensor(tmESDCameraTemperature, 520, 4, bbData)
            putDigitalSensor(tmESDReverseCameraVolume, 524, bbData)
            putDigitalSensor(tmESDReverseCameraFlow, 528, bbData)
            putDigitalSensor(tmESDReverseCameraTemperature, 532, 4,bbData)

            addPoint(dataWorker.alStm[0], pointTime, bbData, sqlBatchData)
            dataCount++
        }
        dataCountAll++
        if (firstPointTime == 0) firstPointTime = pointTime
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

        tmMassFlow.clear()
        tmDensity.clear()
        tmTemperature.clear()
        tmVolumeFlow.clear()
        tmAccumulatedMass.clear()
        tmAccumulatedVolume.clear()

        tmESDStatus.clear()
        tmESDVolume.clear()
        tmESDFlow.clear()
        tmESDCameraVolume.clear()
        tmESDCameraFlow.clear()
        tmESDCameraTemperature.clear()
        tmESDReverseCameraVolume.clear()
        tmESDReverseCameraFlow.clear()
        tmESDReverseCameraTemperature.clear()
    }

}

/*
//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- счётчик Меркурий: двухбайтовое число с обменом мест двух младших байт
    private fun getMercuryInt2(): Int = ( bbIn.getByte().toInt() shl 8 ) or ( bbIn.getByte().toInt() and 0xFF )

    //--- счётчик Меркурий: трёхбайтовое число с обменом мест двух младших байт
    private fun getMercuryInt3(): Int = ( bbIn.getByte().toInt() and 0xFF shl 16 ) or ( bbIn.getByte().toInt() and 0xFF ) or ( bbIn.getByte().toInt() and 0xFF shl 8 )

    //--- счётчик Меркурий: трёхбайтовое число
    //--- с маскированием двух старших бит в старшем байте (битовые флаги: активная/реактивная, прямая/обратная)
    //--- и обменом мест двух младших байт
    private fun getMercuryInt3Mask(): Int = ( bbIn.getByte().toInt() and 0x3F shl 16 ) or ( bbIn.getByte().toInt() and 0xFF ) or ( bbIn.getByte().toInt() and 0xFF shl 8 )

    //--- счётчик Меркурий: 4-байтовое число с обменом мест двух старших и двух младших байт
    private fun getMercuryInt4(): Int = ( bbIn.getByte().toInt() and 0xFF shl 16 ) or ( bbIn.getByte().toInt() and 0xFF shl 24 ) or
                                        ( bbIn.getByte().toInt() and 0xFF ) or ( bbIn.getByte().toInt() and 0xFF shl 8 )




                        val addr = bbIn.getByte().toInt() and 0xFF // id - 1 байт, адрес счётчика, от 201 до 205
                        //AdvancedLogger.debug( " addr = " + addr );
                        if( addr in 201..205 ) {
                            //--- 0 - удалось открыть канал, 1 - не удалось открыть канал, ошибка связи
                            if( bbIn.getByte().toInt() == 0 ) {
                                val index = addr - 201

                                if( tmEnergoCount[ index ] == null ) tmEnergoCount.put( index, IntArray( 4 ) )
                                else Arrays.fill( tmEnergoCount[ index ], 0 )
                                if( tmEnergoPower[ index ] == null ) tmEnergoPower.put( index, IntArray( 8 ) )
                                else Arrays.fill( tmEnergoPower[ index ], 0 )

                                //--- суммарная реактивная мощность
                                //--- реактивная мощность фаза 1
                                //--- реактивная мощность фаза 2
                                //--- реактивная мощность фаза 3
                                tmEnergoPower[ index ]!![ 4 ] = Math.round( getMercuryInt3Mask() / 100.0f )
                                tmEnergoPower[ index ]!![ 5 ] = Math.round( getMercuryInt3Mask() / 100.0f )
                                tmEnergoPower[ index ]!![ 6 ] = Math.round( getMercuryInt3Mask() / 100.0f )
                                tmEnergoPower[ index ]!![ 7 ] = Math.round( getMercuryInt3Mask() / 100.0f )

                                //--- суммарная активная мощность
                                //--- активная мощность фаза 1
                                //--- активная мощность фаза 2
                                //--- активная мощность фаза 3
                                tmEnergoPower[index]!![0] = Math.round( getMercuryInt3Mask() / 100.0f )
                                tmEnergoPower[index]!![1] = Math.round( getMercuryInt3Mask() / 100.0f )
                                tmEnergoPower[index]!![2] = Math.round( getMercuryInt3Mask() / 100.0f )
                                tmEnergoPower[index]!![3] = Math.round( getMercuryInt3Mask() / 100.0f )

                                val A12 = Math.round( getMercuryInt3() / 100.0f )
                                val A23 = Math.round( getMercuryInt3() / 100.0f )
                                val A13 = Math.round( getMercuryInt3() / 100.0f )

                                val U1 = Math.round( getMercuryInt3() / 100.0f )
                                val U2 = Math.round( getMercuryInt3() / 100.0f )
                                val U3 = Math.round( getMercuryInt3() / 100.0f )

                                val I1 = getMercuryInt3() / 1000.0f
                                val I2 = getMercuryInt3() / 1000.0f
                                val I3 = getMercuryInt3() / 1000.0f

                                //--- значения от 0 до 1
                                val KSs = getMercuryInt3Mask() / 1000.0f
                                val KS1 = getMercuryInt3Mask() / 1000.0f
                                val KS2 = getMercuryInt3Mask() / 1000.0f
                                val KS3 = getMercuryInt3Mask() / 1000.0f

                                val Kg1 = getMercuryInt2() / 100.0f
                                val Kg2 = getMercuryInt2() / 100.0f
                                val Kg3 = getMercuryInt2() / 100.0f

                                val F = Math.round( getMercuryInt3() / 100.0f )
                                //--- без маскирования старшего байта, чтобы получить отрицательные температуры
                                val T = Math.round( getMercuryInt2() / 10.0f )

                                //--- Энергия от сброса активная прямая (A+)
                                //--- Энергия от сброса активная обратная (A-)
                                //--- Энергия от сброса реактивная прямая (R+)
                                //--- Энергия от сброса реактивная обратная (R-)
                                tmEnergoCount[index]!![0] = getMercuryInt4()
                                tmEnergoCount[index]!![1] = getMercuryInt4()
                                tmEnergoCount[index]!![2] = getMercuryInt4()
                                tmEnergoCount[index]!![3] = getMercuryInt4()

                                val DT10 = bbIn.getByte().toInt()
                                val DT11 = bbIn.getByte().toInt()
                                val DT12 = bbIn.getByte().toInt()

                                val DT20 = bbIn.getByte().toInt()
                                val DT21 = bbIn.getByte().toInt()
                                val DT22 = bbIn.getByte().toInt()
                                val DT23 = bbIn.getByte().toInt()
                                val DT24 = bbIn.getByte().toInt()
                                val DT25 = bbIn.getByte().toInt()


                                AdvancedLogger.debug( "ENERGO deviceID = $deviceID\n адрес счётчика = $addr" +
                                    "\n суммарная реактивная мощность по сумме фаз = ${tmEnergoPower[ index ]!![ 4 ]}" +
                                    "\n суммарная реактивная мощность по фазе 1    = ${tmEnergoPower[ index ]!![ 5 ]}" +
                                    "\n суммарная реактивная мощность по фазе 2    = ${tmEnergoPower[ index ]!![ 6 ]}" +
                                    "\n суммарная реактивная мощность по фазе 3    = ${tmEnergoPower[ index ]!![ 7 ]}" +
                                    "\n суммарная активная мощность по сумме фаз = ${tmEnergoPower[ index ]!![ 0 ]}" +
                                    "\n суммарная активная мощность по фазе 1    = ${tmEnergoPower[ index ]!![ 1 ]}" +
                                    "\n суммарная активная мощность по фазе 2    = ${tmEnergoPower[ index ]!![ 2 ]}" +
                                    "\n суммарная активная мощность по фазе 3    = ${tmEnergoPower[ index ]!![ 3 ]}" +
                                    "\n угол между основными гармониками 1-2 фаз = $A12" +
                                    "\n  угол между основными гармониками 2-3 фаз = $A23" +
                                    "\n  угол между основными гармониками 1-3 фаз = $A13" +
                                    "\n  напряжение по фазе 1 = $U1" +
                                    "\n  напряжение по фазе 2 = $U2" +
                                    "\n  напряжение по фазе 3 = $U3" +
                                    "\n  ток по фазе 1 = $I1" +
                                    "\n  ток по фазе 2 = $I2" +
                                    "\n  ток по фазе 3 = $I3" +
                                    "\n  коэффициент мощности по сумме фаз = $KSs" +
                                    "\n  коэффициент мощности по фазе 1 = $KS1" +
                                    "\n  коэффициент мощности по фазе 2 = $KS2" +
                                    "\n  коэффициент мощности по фазе 3 = $KS3" +
                                    "\n  КГ по фазе 1 = $Kg1" +
                                    "\n  КГ по фазе 2 = $Kg2" +
                                    "\n  КГ по фазе 3 = $Kg3" +
                                    "\n  Частота = $F" +
                                    "\n  Температура = $T" +
                                    "\n  Энергия от сброса активная прямая (A+)     = ${tmEnergoCount[ index ]!![ 0 ]}" +
                                    "\n  Энергия от сброса активная обратная (A-)   = ${tmEnergoCount[ index ]!![ 1 ]}" +
                                    "\n  Энергия от сброса реактивная прямая (R+)   = ${tmEnergoCount[ index ]!![ 2 ]}" +
                                    "\n  Энергия от сброса реактивная обратная (R-) = ${tmEnergoCount[ index ]!![ 3 ]}" +
                                    "\n  Дата перепрограммирования = " + DT10 + " / " + DT11 + " / " + DT12 +
                                    "\n  Дата/время вскрытия = " + DT23 + " / " + DT24 + " / " + DT25 + ' ' + DT22 + " : " + DT21 + " : " + DT20 )
                            }
                            else {
                                AdvancedLogger.error( "deviceID = $deviceID\n Меркурий: адрес счётчика = $addr\n не удалось открыть канал, ошибка связи" )
                                bbIn.skip( userDataSize - 3 )
                            }
                        }
                        else {
                            AdvancedLogger.error( "deviceID = $deviceID\n Меркурий: неправильный адрес счётчика = $addr" )
                            bbIn.skip( userDataSize - 2 )
                        }




                                 //--- далее кусками по 6 байт
                                for( idi in 0 until ( userDataSize - 2 ) / 6 ) {
                                    //--- данные идут в BigEndian, в отличие от остальных галилео-данных.
                                    //--- чтобы не переключать BigEndian-режим из-за этих данных, проще переставить байты вручную
                                    var b1 = bbIn.getByte()
                                    var b2 = bbIn.getByte()
                                    val id = b1.toInt() and 0xFF shl 8 or ( b2.toInt() and 0xFF )
                                    //int id = bbIn.getShort() & 0xFFFF;

                                    b1 = bbIn.getByte()
                                    b2 = bbIn.getByte()
                                    val b3 = bbIn.getByte()
                                    val b4 = bbIn.getByte()
                                    val value = ( b1.toInt() and 0xFF shl 24 ) or ( b2.toInt() and 0xFF shl 16 ) or ( b3.toInt() and 0xFF shl 8 ) or ( b4.toInt() and 0xFF )
                                    //int value = bbIn.getInt();

                                    //AdvancedLogger.error( "id = " + Integer.toHexString( id ) + ", value = " + Integer.toHexString( value ) );

                                    if( id in 0x0400..0x043F ) tmLevelSensor[ id - 0x0400 ] = value
                                    else if( id in 0x0440..0x047F ) tmVoltageSensor[ id - 0x0440 ] = value
                                    else if( id in 0x0480..0x04BF ) tmFrequencySensor[ id - 0x0480 ] = value
                                    else if( id in 0x04C0..0x04FF ) tmTorqueSensor[ id - 0x04C0 ] = value
                                }


 */