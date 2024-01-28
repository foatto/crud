package foatto.ts.spring.services

import foatto.core_server.app.server.UserConfig
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigBase
import foatto.ts.core_ts.sensor.config.SensorConfigSetup
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.spring.repositories.ObjectRepository
import foatto.util.AdvancedLogger
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ObjectService(
    private val objectRepository: ObjectRepository,
) {

    fun getAllObjects() = objectRepository.findAll()

    fun getObjectConfig(
        hmUserFullNames: Map<Int, String>,
        hmUserShortNames: Map<Int, String>,
        userConfig: UserConfig,
        objectId: Int,
    ): ObjectConfig {
        val objectEntity = objectRepository.findByIdOrNull(objectId) ?: "ObjectConfig not exist for object_id = $objectId".let {
            AdvancedLogger.error(it)
            throw Exception(it)
        }

        //--- add the name of the object with its short login name
        val shortUserName = hmUserShortNames[objectEntity.userId]

        val objectConfig = ObjectConfig(
            objectId = objectId,
            userId = objectEntity.userId,
            name = objectEntity.name + if (shortUserName.isNullOrBlank()) "" else " ($shortUserName)",
            model = objectEntity.model,
        )

        //--- fill report title
        objectConfig.alTitleName.add("Владелец:")
        objectConfig.alTitleValue.add(hmUserFullNames[objectConfig.userId] ?: "(неизвестно)")
        objectConfig.alTitleName.add("Наименование:")
        objectConfig.alTitleValue.add(objectConfig.name)
        objectConfig.alTitleName.add("Модель:")
        objectConfig.alTitleValue.add(objectConfig.model)

        //--- sensor configuration loading
        //--- (notes: "serial no" and "start date of operation" will be ignored)
        objectEntity.sensors.forEach { sensorEntity ->

            val sensorType = sensorEntity.sensorType
            val portNum = sensorEntity.portNum

            val hmSC = objectConfig.hmSensorConfig.getOrPut(sensorType) { mutableMapOf() }
            when (sensorType) {
                SensorConfig.SENSOR_NEXT_CLEAN_DATETIME -> {
                    hmSC[portNum] = SensorConfig(
                        id = sensorEntity.id,
                        name = sensorEntity.name,
                        group = sensorEntity.group,
                        descr = sensorEntity.descr,
                        portNum = portNum,
                        sensorType = sensorType,
                    )
                }

                SensorConfig.SENSOR_STATE -> {
                    hmSC[portNum] = SensorConfigState(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                    )
                }

                SensorConfig.SENSOR_DEPTH, SensorConfig.SENSOR_SPEED, SensorConfig.SENSOR_LOAD,
                SensorConfig.SENSOR_TEMPERATURE_IN, SensorConfig.SENSOR_TEMPERATURE_OUT,
                SensorConfig.SENSOR_SIGNAL_LEVEL -> {
                    hmSC[portNum] = SensorConfigAnalogue(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        minGraphicView = sensorEntity.minGraphicView ?: 0.0,
                        maxGraphicView = sensorEntity.maxGraphicView ?: 0.0,
                        minGraphicLimit = sensorEntity.minGraphicLimit ?: 0.0,
                        maxGraphicLimit = sensorEntity.maxGraphicLimit ?: 0.0,
                        minStateView = sensorEntity.minStateView ?: 0.0,
                        maxStateView = sensorEntity.maxStateView ?: 0.0,
                    )
                }

                SensorConfig.SENSOR_SETUP -> {
                    hmSC[portNum] = SensorConfigSetup(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        showPos = sensorEntity.showPos ?: 0,
                        valueType = sensorEntity.valueType ?: SensorConfigSetup.VALUE_TYPE_NUMBER,
                        prec = sensorEntity.prec ?: 0,
                    )
                }

                else -> {
                    AdvancedLogger.error("Unknown sensorType = $sensorType for sensorId = ${sensorEntity.id}")
                }
            }

            hmSC[portNum]?.let { sensorConfig ->
                if (sensorConfig is SensorConfigBase) {
                    sensorEntity.calibration
                        //--- sorting set for interpolation
                        .sortedBy { sensorConfigCalibrationEntity -> sensorConfigCalibrationEntity.sensorValue }
                        .forEach { sensorConfigCalibrationEntity ->
                            sensorConfig.alValueSensor.add(sensorConfigCalibrationEntity.sensorValue)
                            sensorConfig.alValueData.add(sensorConfigCalibrationEntity.sensorData)
                        }
                }
            }

        }

        return objectConfig
    }

}