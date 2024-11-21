package foatto.server.controller

import foatto.core.util.getCurrentTimeInt
import foatto.server.ds.CoreTelematicFunction
import foatto.server.ds.CoreTelematicFunction.MAX_PORT_PER_DEVICE
import foatto.server.ds.DeviceConfig
import foatto.server.ds.GalileoFunction
import foatto.server.ds.MMSTelematicFunction
import foatto.server.ds.PulsarData
import foatto.server.entity.SensorEntity
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.sql.CoreAdvancedConnection
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.AdvancedLogger
import jakarta.persistence.EntityManager
import jakarta.servlet.http.HttpServletRequest
import kotlinx.datetime.TimeZone
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
class MMSPulsarDataController(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
) {

    private val METHOD_NAME = "MMSPulsarDataController.getPulsarData"
    private val BLOCK_ID = "PulsarMeasure"
    private val ID_PREFIX = "ID"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Value("\${mms_log_session}")
    val configSessionLogPath: String = ""

    @Value("\${mms_log_journal}")
    val configJournalLogPath: String = ""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- по 16 типизированных датчиков от юриковского радиоудлиннителя
    private val tmLLSLevel = sortedMapOf<Int, Double>()
    private val tmGalileoVoltage = sortedMapOf<Int, Double>()
    private val tmGalileoCount = sortedMapOf<Int, Double>()

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

    private val tmEnergoTransformKoefCurrent = sortedMapOf<Int, Double>()
    private val tmEnergoTransformKoefVoltage = sortedMapOf<Int, Double>()

    //--- датчики ЭМИС/Эльметро
    private val tmEmisDensity = sortedMapOf<Int, Double>()              // плотность
    private val tmEmisTemperature = sortedMapOf<Int, Double>()          // температура
    private val tmEmisMassFlow = sortedMapOf<Int, Double>()             // массовый расход
    private val tmEmisVolumeFlow = sortedMapOf<Int, Double>()           // объёмный расход
    private val tmEmisAccumulatedMass = sortedMapOf<Int, Double>()      // накопленная масса
    private val tmEmisAccumulatedVolume = sortedMapOf<Int, Double>()    // накопленный объём

    //--- датчики ПМП-201/218
    private val tmPMPLevel = sortedMapOf<Int, Double>()         // уровень [м]
    private val tmPMPTemperature = sortedMapOf<Int, Double>()   // температура
    private val tmPMPVolume = sortedMapOf<Int, Double>()        // (накопленный) объём [куб.м]
    private val tmPMPMass = sortedMapOf<Int, Double>()          // (накопленная) масса [тонна]
    private val tmPMPDensity = sortedMapOf<Int, Double>()       // плотность [г/куб.см == тонна/куб.м]

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- количество считанных и записанных блоков данных (например, точек)
    var dataCount = 0

    //--- время первого и последнего блока данных (например, точки)
    var firstPointTime = 0
    var lastPointTime = 0

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- /{fileName} - на время Юриной отладки
    @PostMapping(value = ["/data/pulsar/{fileName}"])
    @Transactional
    fun getPulsarData(
        @PathVariable("fileName")
        fileName: String,
        @RequestBody
        arrData: Array<PulsarData>,
        request: HttpServletRequest,
    ) {
        val dirSessionLog = File(configSessionLogPath)
        val dirJournalLog = File(configJournalLogPath)

        if (arrData.isEmpty()) {
            outDeviceParseError(dirJournalLog, request.remoteAddr, "Empty data array")
            return
        }

        //--- время начала сессии
        val begTime = getCurrentTimeInt()
        //--- запись состояния сессии
        var status = "Init; Start;"

        //--- номер версии прошивки
        val fwVersion = "1"

        //--- first initial row in data packet array
        val headerData = arrData[0]
        if (headerData.blockID != BLOCK_ID) {
            outDeviceParseError(dirJournalLog, request.remoteAddr, "Wrong first blockID == '${headerData.blockID}'")
            return
        }
        if (headerData.deviceID.isNullOrBlank()) {
            outDeviceParseError(dirJournalLog, request.remoteAddr, "Empty deviceID")
            return
        }

        val serialNo: String = headerData.deviceID!!

        var dc: DeviceConfig? = null
        ApplicationService.withConnection(entityManager) { conn ->
            dc = DeviceConfig.getDeviceConfig(conn, serialNo)
        }

        val deviceConfig: DeviceConfig = dc?.let {
            status += " ID;"
            dc
        } ?: run {
            outDeviceParseError(dirJournalLog, request.remoteAddr, "Unlinked object or unknown serialNo = '$serialNo'")
            return
        }

        val sensorConfigs = mutableMapOf<Int, MutableList<SensorEntity>>()                 // portNum to sensorEntities
        val sensorCalibrations = mutableMapOf<Int, MutableList<Pair<Double, Double>>>()    // sensorId to list of <sensorValue to dataValue> pairs

        objectRepository.findByIdOrNull(deviceConfig.objectId)?.let { objectEntity ->
            sensorRepository.findByObj(objectEntity).forEach { sensorEntity ->
                //--- fill sensor datas
                sensorEntity.portNum?.let { portNum ->
                    val sensorEntitiesByPortNum = sensorConfigs.getOrPut(portNum) { mutableListOf() }
                    sensorEntitiesByPortNum += sensorEntity
                }
                //--- fill sensors calibration datas
                val sensorCalibrationPairs = mutableListOf<Pair<Double, Double>>()
                sensorCalibrationRepository.findBySensorOrderBySensorValue(sensorEntity).forEach { sensorCalibrationEntity ->
                    sensorCalibrationEntity.sensorValue?.let { sensorValue ->
                        sensorCalibrationEntity.dataValue?.let { dataValue ->
                            sensorCalibrationPairs += sensorValue to dataValue
                        }
                    }
                }
                sensorCalibrations[sensorEntity.id] = sensorCalibrationPairs
            }
        }

        for (i in 1..arrData.lastIndex) {
            val pulsarData = arrData[i]
            pulsarData.dateTime?.epochSeconds?.toInt()?.let { pointTime ->
                val curTime = getCurrentTimeInt()
                if (pointTime > curTime - CoreTelematicFunction.MAX_PAST_TIME && pointTime < curTime + CoreTelematicFunction.MAX_FUTURE_TIME) {
                    pulsarData.vals?.forEach { hmData ->
                        hmData.forEach { (sId, value) ->
                            if (sId.startsWith(ID_PREFIX)) {
                                sId.substring(ID_PREFIX.length).toIntOrNull(16)?.let { id ->
                                    when (id) {
                                        in 0x0100..0x010F -> tmLLSLevel[id - 0x0100] = value
                                        in 0x0140..0x014F -> tmGalileoVoltage[id - 0x0140] = value
                                        in 0x0180..0x018F -> tmGalileoCount[id - 0x0180] = value

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

                                        in 0x0541..0x0544 -> tmEmisMassFlow[id - 0x0541] = value

                                        in 0x0550..0x0553 -> tmEnergoTransformKoefCurrent[id - 0x0550] = value
                                        in 0x0560..0x0563 -> tmEnergoTransformKoefVoltage[id - 0x0560] = value

                                        in 0x0581..0x0584 -> tmEmisDensity[id - 0x0581] = value
                                        in 0x05C1..0x05C4 -> tmEmisTemperature[id - 0x05C1] = value
                                        in 0x0601..0x0604 -> tmEmisVolumeFlow[id - 0x0601] = value
                                        in 0x0641..0x0644 -> tmEmisAccumulatedMass[id - 0x0641] = value
                                        in 0x0681..0x0684 -> tmEmisAccumulatedVolume[id - 0x0681] = value

                                        in 0x0700..0x0703 -> tmEnergoPowerActiveABC[id - 0x0700] = value
                                        in 0x0710..0x0713 -> tmEnergoPowerReactiveABC[id - 0x0710] = value
                                        in 0x0720..0x0723 -> tmEnergoPowerFullABC[id - 0x0720] = value

                                        in 0x0800..0x080F -> tmPMPLevel[id - 0x0800] = value
                                        in 0x0810..0x081F -> tmPMPTemperature[id - 0x0810] = value
                                        in 0x0820..0x082F -> tmPMPVolume[id - 0x0820] = value
                                        in 0x0830..0x083F -> tmPMPMass[id - 0x0830] = value
                                        in 0x0840..0x084F -> tmPMPDensity[id - 0x0840] = value

                                        else -> outDataParseError(serialNo, "Unknown ID value == '$sId'")
                                    }
                                } ?: run {
                                    outDataParseError(serialNo, "Wrong ID value == '$sId'")
                                }
                            } else {
                                outDataParseError(serialNo, "Wrong ID format == '$sId'")
                            }
                        }
                    } ?: run {
                        outDataParseError(serialNo, "Vals is null")
                    }
                    ApplicationService.withConnection(entityManager) { conn ->
                        savePoint(conn, deviceConfig, sensorConfigs, sensorCalibrations, pointTime)
                    }
                } else {
                    outDataParseError(serialNo, "DateTime is very old or in future")
                }
            } ?: run {
                outDataParseError(serialNo, "DateTime is null")
            }
        }

        ApplicationService.withConnection(entityManager) { conn ->
            status += " DataRead; Ok."

            //--- данные успешно переданы - теперь можно завершить транзакцию
            MMSTelematicFunction.writeSession(
                conn = conn,
                dirSessionLog = dirSessionLog,
                timeZone = timeZone,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = request.remoteAddr,
                status = status,
                errorText = "",
                dataCount = dataCount,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
                isOk = true,
            )
        }

        dataCount = 0
        firstPointTime = 0
        lastPointTime = 0
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun savePoint(
        conn: CoreAdvancedConnection,
        deviceConfig: DeviceConfig,
        sensorConfigs: Map<Int, List<SensorEntity>>,               // portNum to sensorEntities
        sensorCalibrations: Map<Int, List<Pair<Double, Double>>>,  // sensorId to list of <sensorValue to dataValue> pairs
        pointTime: Int,
    ) {
        val bbData = AdvancedByteBuffer(MAX_PORT_PER_DEVICE * 8)

        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmGalileoCount, GalileoFunction.PORT_NUM_GALILEO_COUNT, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmLLSLevel, GalileoFunction.PORT_NUM_LLS_LEVEL, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmGalileoVoltage, GalileoFunction.PORT_NUM_GALILEO_VOLTAGE, bbData)

        //--- данные по электросчётчику ---

        //--- значения счётчиков от последнего сброса (активная/реактивная прямая/обратная)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountActiveDirect, GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_DIRECT, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountActiveReverse, GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_REVERSE, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountReactiveDirect, GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_DIRECT, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCountReactiveReverse, GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_REVERSE, bbData)

        //--- напряжение по фазам A1..4, B1..4, C1..4
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoVoltageA, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_A, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoVoltageB, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_B, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoVoltageC, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_C, bbData)

        //--- ток по фазам A1..4, B1..4, C1..4
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCurrentA, GalileoFunction.PORT_NUM_MERCURY_CURRENT_A, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCurrentB, GalileoFunction.PORT_NUM_MERCURY_CURRENT_B, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoCurrentC, GalileoFunction.PORT_NUM_MERCURY_CURRENT_C, bbData)

        //--- коэффициент мощности по фазам A1..4, B1..4, C1..4
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerKoefA, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_A, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerKoefB, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_B, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerKoefC, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_C, bbData)

        //--- активная мощность по фазам A1..4, B1..4, C1..4
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveA, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_A, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveB, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_B, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveC, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_C, bbData)

        //--- реактивная мощность по фазам A1..4, B1..4, C1..4
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveA, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_A, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveB, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_B, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveC, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_C, bbData)

        //--- полная мощность по фазам A1..4, B1..4, C1..4
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullA, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_A, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullB, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_B, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullC, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_C, bbData)

        //--- мощность по трём фазам: активная, реактивная, суммарная
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerActiveABC, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_ABC, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerReactiveABC, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_ABC, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoPowerFullABC, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_ABC, bbData)

        //--- коэффициент трансформации по току и напряжению
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoTransformKoefCurrent, GalileoFunction.PORT_NUM_MERCURY_TRANSFORM_KOEF_CURRENT, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEnergoTransformKoefVoltage, GalileoFunction.PORT_NUM_MERCURY_TRANSFORM_KOEF_VOLTAGE, bbData)

        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisMassFlow, GalileoFunction.PORT_NUM_EMIS_MASS_FLOW, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisDensity, GalileoFunction.PORT_NUM_EMIS_DENSITY, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisTemperature, GalileoFunction.PORT_NUM_EMIS_TEMPERATURE, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisVolumeFlow, GalileoFunction.PORT_NUM_EMIS_VOLUME_FLOW, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisAccumulatedMass, GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_MASS, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmEmisAccumulatedVolume, GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_VOLUME, bbData)

        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPLevel, GalileoFunction.PORT_NUM_PMP_LEVEL, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPTemperature, GalileoFunction.PORT_NUM_PMP_TEMPERATURE, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPVolume, GalileoFunction.PORT_NUM_PMP_VOLUME, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPMass, GalileoFunction.PORT_NUM_PMP_MASS, bbData)
        MMSTelematicFunction.saveSensorData(conn, deviceConfig.deviceIndex, sensorConfigs, sensorCalibrations, pointTime, tmPMPDensity, GalileoFunction.PORT_NUM_PMP_DENSITY, bbData)

        ApplicationService.withConnection(entityManager) { conn ->
            MMSTelematicFunction.addPoint(conn, deviceConfig, pointTime, bbData)
        }
        dataCount++

        if (firstPointTime == 0) {
            firstPointTime = pointTime
        }
        lastPointTime = pointTime
        //--- массивы данных по датчикам очищаем независимо от записываемости точек
        clearSensorArrays()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun clearSensorArrays() {
        tmLLSLevel.clear()
        tmGalileoVoltage.clear()
        tmGalileoCount.clear()

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

        tmEnergoTransformKoefCurrent.clear()
        tmEnergoTransformKoefVoltage.clear()

        tmEmisMassFlow.clear()
        tmEmisDensity.clear()
        tmEmisTemperature.clear()
        tmEmisVolumeFlow.clear()
        tmEmisAccumulatedMass.clear()
        tmEmisAccumulatedVolume.clear()

        tmPMPLevel.clear()
        tmPMPTemperature.clear()
        tmPMPVolume.clear()
        tmPMPMass.clear()
        tmPMPDensity.clear()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun outDeviceParseError(dirJournalLog: File, address: String, e: String) {
        CoreTelematicFunction.writeJournal(
            dirJournalLog = dirJournalLog,
            timeZone = timeZone,
            address = address,
            errorText = e,
        )
        AdvancedLogger.error("$METHOD_NAME: $e")
    }

    private fun outDataParseError(serialNo: String?, e: String) {
        AdvancedLogger.error("$METHOD_NAME: serialNo = $serialNo: $e")
    }
}
