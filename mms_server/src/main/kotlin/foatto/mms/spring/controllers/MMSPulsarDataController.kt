package foatto.mms.spring.controllers

import foatto.core_server.ds.CoreTelematicFunction
import foatto.mms.core_mms.ds.DeviceConfig
import foatto.mms.core_mms.ds.GalileoFunction
import foatto.mms.core_mms.ds.MMSTelematicFunction
import foatto.mms.core_mms.ds.PulsarData
import foatto.spring.CoreSpringApp
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import foatto.sql.SQLBatch
import foatto.util.AdvancedByteBuffer
import foatto.util.AdvancedLogger
import foatto.util.DateTime_YMDHMS
import foatto.util.getCurrentTimeInt
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.time.ZoneId
import java.util.*
import jakarta.servlet.http.HttpServletRequest

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

    private val tmEnergoTransformKoefCurrent = sortedMapOf<Int, Double>()
    private val tmEnergoTransformKoefVoltage = sortedMapOf<Int, Double>()

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
                    if (pulsarData.deviceID.isBlank()) {
                        outDeviceParseError(dirJournalLog, request.remoteAddr, "Empty deviceID")
                        break
                    }
                    serialNo = pulsarData.deviceID!!
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

                                        in 0x0550..0x0553 -> tmEnergoTransformKoefCurrent[id - 0x0550] = value
                                        in 0x0560..0x0563 -> tmEnergoTransformKoefVoltage[id - 0x0560] = value

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
            MMSTelematicFunction.writeSession(
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
        if (pointTime > curTime - CoreTelematicFunction.MAX_PAST_TIME && pointTime < curTime + CoreTelematicFunction.MAX_FUTURE_TIME) {
            val bbData = AdvancedByteBuffer(conn.dialect.textFieldMaxSize / 2)

            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmCountSensor, 110, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmLevelSensor, 120, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmVoltageSensor, 140, bbData)

            //--- данные по электросчётчику ---

            //--- значения счётчиков от последнего сброса (активная/реактивная прямая/обратная)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoCountActiveDirect, GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_DIRECT, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoCountActiveReverse, GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_REVERSE, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoCountReactiveDirect, GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_DIRECT, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoCountReactiveReverse, GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_REVERSE, bbData)

            //--- напряжение по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoVoltageA, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_A, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoVoltageB, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_B, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoVoltageC, GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_C, bbData)

            //--- ток по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoCurrentA, GalileoFunction.PORT_NUM_MERCURY_CURRENT_A, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoCurrentB, GalileoFunction.PORT_NUM_MERCURY_CURRENT_B, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoCurrentC, GalileoFunction.PORT_NUM_MERCURY_CURRENT_C, bbData)

            //--- коэффициент мощности по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerKoefA, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_A, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerKoefB, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_B, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerKoefC, GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_C, bbData)

            //--- активная мощность по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerActiveA, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_A, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerActiveB, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_B, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerActiveC, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_C, bbData)

            //--- реактивная мощность по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerReactiveA, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_A, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerReactiveB, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_B, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerReactiveC, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_C, bbData)

            //--- полная мощность по фазам A1..4, B1..4, C1..4
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerFullA, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_A, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerFullB, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_B, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerFullC, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_C, bbData)

            //--- мощность по трём фазам: активная, реактивная, суммарная
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerActiveABC, GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_ABC, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerReactiveABC, GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_ABC, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoPowerFullABC, GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_ABC, bbData)

            //--- коэффициент трансформации по току и напряжению
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoTransformKoefCurrent, GalileoFunction.PORT_NUM_MERCURY_TRANSFORM_KOEF_CURRENT, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmEnergoTransformKoefVoltage, GalileoFunction.PORT_NUM_MERCURY_TRANSFORM_KOEF_VOLTAGE, bbData)

            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmMassFlow, GalileoFunction.PORT_NUM_EMIS_MASS_FLOW, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmDensity, GalileoFunction.PORT_NUM_EMIS_DENSITY, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmTemperature, GalileoFunction.PORT_NUM_EMIS_TEMPERATURE, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmVolumeFlow, GalileoFunction.PORT_NUM_EMIS_VOLUME_FLOW, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmAccumulatedMass, GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_MASS, bbData)
            CoreTelematicFunction.putDigitalSensors(deviceConfig.index, tmAccumulatedVolume, GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_VOLUME, bbData)

            MMSTelematicFunction.addPoint(conn, deviceConfig, pointTime, bbData, sqlBatchData)
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

        tmEnergoTransformKoefCurrent.clear()
        tmEnergoTransformKoefVoltage.clear()

        tmMassFlow.clear()
        tmDensity.clear()
        tmTemperature.clear()
        tmVolumeFlow.clear()
        tmAccumulatedMass.clear()
        tmAccumulatedVolume.clear()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun outDeviceParseError(dirJournalLog: File, address: String, e: String) {
        CoreTelematicFunction.writeJournal(
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
