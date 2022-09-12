package foatto.mms.spring.controllers

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.ds.AbstractTelematicHandler
import foatto.mms.core_mms.ds.DeviceConfig
import foatto.mms.core_mms.ds.MMSHandler
import foatto.mms.core_mms.ds.PulsarData
import foatto.spring.CoreSpringApp
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import foatto.sql.SQLBatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.time.ZoneId
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class MMSPulsarDataController {

    private val METHOD_NAME = "MMSPulsarDataController.getPulsarData"
    private val BLOCK_ID = "PulsarMeasure"
    private val ID_PREFIX = "ID"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val zoneId: ZoneId = ZoneId.systemDefault()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Value("\${mms_log_session}")
    val configSessionLogPath: String = ""

    @Value("\${mms_log_journal}")
    val configJournalLogPath: String = ""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- по 16 типизированных датчиков от юриковского радиоудлиннителя
    private val tmLevelSensor = sortedMapOf<Int, Double>()
    private val tmVoltageSensor = sortedMapOf<Int, Double>()
    private val tmCountSensor = sortedMapOf<Int, Double>()

    //--- 4 вида счётчиков энергии от сброса (активная прямая, активная обратная, реактивная прямая, реактивная обратная)
    private val tmEnergoCountActiveDirect = sortedMapOf<Int, Double>()
    private val tmEnergoCountActiveReverse = sortedMapOf<Int, Double>()
    private val tmEnergoCountReactiveDirect = sortedMapOf<Int, Double>()
    private val tmEnergoCountReactiveReverse = sortedMapOf<Int, Double>()

    //--- напряжение по фазам
    private val tmEnergoVoltageA = sortedMapOf<Int, Double>()
    private val tmEnergoVoltageB = sortedMapOf<Int, Double>()
    private val tmEnergoVoltageC = sortedMapOf<Int, Double>()

    //--- ток по фазам
    private val tmEnergoCurrentA = sortedMapOf<Int, Double>()
    private val tmEnergoCurrentB = sortedMapOf<Int, Double>()
    private val tmEnergoCurrentC = sortedMapOf<Int, Double>()

    //--- коэффициент мощности по фазам
    private val tmEnergoPowerKoefA = sortedMapOf<Int, Double>()
    private val tmEnergoPowerKoefB = sortedMapOf<Int, Double>()
    private val tmEnergoPowerKoefC = sortedMapOf<Int, Double>()

    //--- energy power (active, reactive, full/summary) by phase by 4 indicators
    private val tmEnergoPowerActiveA = sortedMapOf<Int, Double>()
    private val tmEnergoPowerActiveB = sortedMapOf<Int, Double>()
    private val tmEnergoPowerActiveC = sortedMapOf<Int, Double>()
    private val tmEnergoPowerReactiveA = sortedMapOf<Int, Double>()
    private val tmEnergoPowerReactiveB = sortedMapOf<Int, Double>()
    private val tmEnergoPowerReactiveC = sortedMapOf<Int, Double>()
    private val tmEnergoPowerFullA = sortedMapOf<Int, Double>()
    private val tmEnergoPowerFullB = sortedMapOf<Int, Double>()
    private val tmEnergoPowerFullC = sortedMapOf<Int, Double>()
    private val tmEnergoPowerActiveABC = sortedMapOf<Int, Double>()
    private val tmEnergoPowerReactiveABC = sortedMapOf<Int, Double>()
    private val tmEnergoPowerFullABC = sortedMapOf<Int, Double>()

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

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- количество записанных блоков данных (например, точек)
    var dataCount = 0

    //--- количество считанных блоков данных (например, точек)
    var dataCountAll = 0

    //--- время первого и последнего блока данных (например, точки)
    var firstPointTime = 0
    var lastPointTime = 0

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- /{fileName} - на время Юриной отладки
    @PostMapping(value = ["/data/pulsar/{fileName}"])
    fun getPulsarData(
        @PathVariable("fileName")
        fileName: String,
        @RequestBody
        data: Array<PulsarData>,
        request: HttpServletRequest,
    ) {
        val dirSessionLog = File(configSessionLogPath)
        val dirJournalLog = File(configJournalLogPath)

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)

        //--- время начала сессии
        val begTime = getCurrentTimeInt()
        //--- запись состояния сессии
        var status = "Init; Start;"

        var serialNo: String? = null
        //--- номер версии прошивки
        val fwVersion = "1"
        var deviceConfig: DeviceConfig? = null

        val sqlBatchData = SQLBatch()

        for (pulsarData in data) {
            //--- first initial row in data packet array
            if (pulsarData.deviceID != null) {
                if (pulsarData.blockID == BLOCK_ID) {
                    serialNo = pulsarData.deviceID
                    deviceConfig = DeviceConfig.getDeviceConfig(conn, serialNo)
                    //--- неизвестный контроллер
                    if (deviceConfig == null) {
                        outDeviceParseError(dirJournalLog, request.remoteAddr, "Unknown serialNo = $serialNo")
                        break
                    }
                    status += " ID;"
                } else {
                    outDeviceParseError(dirJournalLog, request.remoteAddr, "Unknown blockID == '${pulsarData.blockID}'")
                    break
                }
            } else {
                pulsarData.dateTime?.epochSecond?.toInt()?.let { pointTime ->
                    pulsarData.vals?.forEach { hmData ->
                        hmData.forEach { (sId, value) ->
                            if (sId.startsWith(ID_PREFIX)) {
                                sId.substring(ID_PREFIX.length).toIntOrNull(16)?.let { id ->
                                    when (id) {
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

                                        in 0x0541..0x0544 -> tmMassFlow[id - 0x0541] = value
                                        in 0x0581..0x0584 -> tmDensity[id - 0x0581] = value
                                        in 0x05C1..0x05C4 -> tmTemperature[id - 0x05C1] = value
                                        in 0x0601..0x0604 -> tmVolumeFlow[id - 0x0601] = value
                                        in 0x0641..0x0644 -> tmAccumulatedMass[id - 0x0641] = value
                                        in 0x0681..0x0684 -> tmAccumulatedVolume[id - 0x0681] = value

                                        in 0x0700..0x0703 -> tmEnergoPowerActiveABC[id - 0x0700] = value
                                        in 0x0710..0x0713 -> tmEnergoPowerReactiveABC[id - 0x0710] = value
                                        in 0x0720..0x0723 -> tmEnergoPowerFullABC[id - 0x0720] = value

                                        else -> outDataParseError(serialNo, "unknown ID value == '$sId'")
                                    }
                                } ?: run {
                                    outDataParseError(serialNo, "wrong ID value == '$sId'")
                                }

                            } else {
                                outDataParseError(serialNo, "wrong ID format == '$sId'")
                            }
                        }
                    } ?: run {
                        outDataParseError(serialNo, "vals is null")
                    }
                    savePoint(conn, deviceConfig!!, pointTime, sqlBatchData)
                } ?: run {
                    outDataParseError(serialNo, "dateTime is null")
                }
            }
        }

        deviceConfig?.let {
            status += " DataRead;"

            sqlBatchData.execute(conn)

            //--- данные успешно переданы - теперь можно завершить транзакцию
            status += " Ok;"
            MMSHandler.writeSession(
                conn = conn,
                dirSessionLog = dirSessionLog,
                zoneId = zoneId,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = request.remoteAddr,
                status = status,
                errorText = "",
                dataCount = dataCount,
                dataCountAll = dataCountAll,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
                isOk = true,
            )
        }

        dataCount = 0
        dataCountAll = 0
        firstPointTime = 0
        lastPointTime = 0

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        conn.close()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun savePoint(
        conn: CoreAdvancedConnection,
        deviceConfig: DeviceConfig,
        pointTime: Int,
        sqlBatchData: SQLBatch,
    ) {
        val curTime = getCurrentTimeInt()
        AdvancedLogger.debug("pointTime = ${DateTime_YMDHMS(ZoneId.systemDefault(), pointTime)}")
        if (pointTime > curTime - AbstractTelematicHandler.MAX_PAST_TIME && pointTime < curTime + AbstractTelematicHandler.MAX_FUTURE_TIME) {
            val bbData = AdvancedByteBuffer(conn.dialect.textFieldMaxSize / 2)

//            //--- напряжения основного и резервного питаний
//            putSensorData(8, 2, powerVoltage, bbData)
//            putSensorData(9, 2, accumVoltage, bbData)
//            //--- универсальные входы (аналоговые/частотные/счётные)
//            putDigitalSensor(tmUniversalSensor, 10, 2, bbData)
//            //--- температура контроллера
//            putSensorData(18, 2, controllerTemperature, bbData)
//            //--- гео-данные
//            putSensorPortNumAndDataSize(SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData)
//            bbData.putInt(if (isCoordOk) wgsX else 0).putInt(if (isCoordOk) wgsY else 0)
//                .putShort(if (isCoordOk && !isParking) speed else 0).putInt(if (isCoordOk) absoluteRun else 0)
//
//            //--- 16 RS485-датчиков уровня топлива, по 2 байта
//            putDigitalSensor(tmRS485Fuel, 20, 2, bbData)
//
//            //--- CAN: уровень топлива в %
//            putSensorData(36, 1, canFuelLevel, bbData)
//            //--- CAN: температура охлаждающей жидкости - сохраняется в виде 4 байт,
//            //--- чтобы сохранить знак числа, не попадая под переделку в unsigned short в виде & 0xFFFF
//            putSensorData(37, 4, canCoolantTemperature, bbData)
//            //--- CAN: обороты двигателя, об/мин
//            putSensorData(38, 2, canEngineRPM, bbData)
//
//            //--- 39-й порт пока свободен
//
//            //--- 16 RS485-датчиков температуры, по 4 байта - пишем как int,
//            //--- чтобы при чтении не потерялся +- температуры
//            putDigitalSensor(tmRS485Temp, 40, 4, bbData)
//
//            putDigitalSensor(tmUserData, 100, 4, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmCountSensor, 110, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmLevelSensor, 120, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmVoltageSensor, 140, bbData)

            //--- данные по электросчётчику ---

            //--- значения счётчиков от последнего сброса (активная/реактивная прямая/обратная)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoCountActiveDirect, 160, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoCountActiveReverse, 164, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoCountReactiveDirect, 168, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoCountReactiveReverse, 172, bbData)

            //--- напряжение по фазам A1..4, B1..4, C1..4
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoVoltageA, 180, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoVoltageB, 184, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoVoltageC, 188, bbData)

            //--- ток по фазам A1..4, B1..4, C1..4
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoCurrentA, 200, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoCurrentB, 204, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoCurrentC, 208, bbData)

            //--- коэффициент мощности по фазам A1..4, B1..4, C1..4
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerKoefA, 220, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerKoefB, 224, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerKoefC, 228, bbData)

            //--- активная мощность по фазам A1..4, B1..4, C1..4
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerActiveA, 232, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerActiveB, 236, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerActiveC, 240, bbData)

            //--- реактивная мощность по фазам A1..4, B1..4, C1..4
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerReactiveA, 244, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerReactiveB, 248, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerReactiveC, 252, bbData)

            //--- полная мощность по фазам A1..4, B1..4, C1..4
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerFullA, 256, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerFullB, 260, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerFullC, 264, bbData)

            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmMassFlow, 270, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmDensity, 280, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmTemperature, 290, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmVolumeFlow, 300, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmAccumulatedMass, 310, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmAccumulatedVolume, 320, bbData)

            //--- мощность по трём фазам: активная, реактивная, суммарная
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerActiveABC, 330, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerReactiveABC, 340, bbData)
            AbstractTelematicHandler.putDigitalSensor(deviceConfig.index, tmEnergoPowerFullABC, 350, bbData)

//            //--- EuroSens Delta
//            putDigitalSensor(tmESDStatus, 500, bbData)
//            putDigitalSensor(tmESDVolume, 504, bbData)
//            putDigitalSensor(tmESDFlow, 508, bbData)
//            putDigitalSensor(tmESDCameraVolume, 512, bbData)
//            putDigitalSensor(tmESDCameraFlow, 516, bbData)
//            putDigitalSensor(tmESDCameraTemperature, 520, bbData)
//            putDigitalSensor(tmESDReverseCameraVolume, 524, bbData)
//            putDigitalSensor(tmESDReverseCameraFlow, 528, bbData)
//            putDigitalSensor(tmESDReverseCameraTemperature, 532, bbData)

            MMSHandler.addPoint(conn, deviceConfig, pointTime, bbData, sqlBatchData)
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

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun clearSensorArrays() {
//        isCoordOk = false
//        wgsX = 0
//        wgsY = 0
//        isParking = false
//        speed = 0
//        absoluteRun = 0
//
//        powerVoltage = 0
//        accumVoltage = 0
//        controllerTemperature = 0
//
//        canFuelLevel = 0
//        canCoolantTemperature = 0
//        canEngineRPM = 0
//
//        tmUniversalSensor.clear()
//        tmRS485Fuel.clear()
//        tmRS485Temp.clear()
//
//        tmUserData.clear()

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

//        tmESDStatus.clear()
//        tmESDVolume.clear()
//        tmESDFlow.clear()
//        tmESDCameraVolume.clear()
//        tmESDCameraFlow.clear()
//        tmESDCameraTemperature.clear()
//        tmESDReverseCameraVolume.clear()
//        tmESDReverseCameraFlow.clear()
//        tmESDReverseCameraTemperature.clear()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun outDeviceParseError(dirJournalLog: File, address: String, e: String) {
        AbstractTelematicHandler.writeJournal(
            dirJournalLog = dirJournalLog,
            zoneId = zoneId,
            address = address,
            errorText = e,
        )
        AdvancedLogger.error("$METHOD_NAME: $e")
    }

    private fun outDataParseError(serialNo: String?, e: String) {
        AdvancedLogger.error("$METHOD_NAME: serialNo = $serialNo: $e")
    }
}
