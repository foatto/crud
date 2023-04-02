package foatto.ts.core_ts.calc

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getSplittedDouble
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigSetup
import foatto.ts.core_ts.sensor.config.SensorConfigState
import java.nio.ByteOrder
import java.util.*

class ObjectState {

    var lastDateTime: Int? = null

    val tmStateValue = sortedMapOf<String, Int>()
    val tmDepthValue = sortedMapOf<String, Double>()
    val tmSpeedValue = sortedMapOf<String, Double>()
    val tmLoadValue = sortedMapOf<String, Double>()
    val tmTemperatureInValue = sortedMapOf<String, Double>()
    val tmTemperatureOutValue = sortedMapOf<String, Double>()
    val tmSignalLevel = sortedMapOf<String, Double>()
    val tmNextCleaningDateTime = sortedMapOf<String, Int>()

    val tmSetupValue = sortedMapOf<Int, String>()

    companion object {

        //--- no more than a one month ater last date-time
        private const val MAX_VIEW_PERIOD = 1 * 30 * 24 * 60 * 60

        fun getState(
            conn: CoreAdvancedConnection,
            oc: ObjectConfig,
        ): ObjectState {
            val result = ObjectState()

            val alStateSensors = oc.hmSensorConfig[SensorConfig.SENSOR_STATE]?.values
            val alDepthSensors = oc.hmSensorConfig[SensorConfig.SENSOR_DEPTH]?.values
            val alSpeedSensors = oc.hmSensorConfig[SensorConfig.SENSOR_SPEED]?.values
            val alLoadSensors = oc.hmSensorConfig[SensorConfig.SENSOR_LOAD]?.values
            val alTemperatureInSensors = oc.hmSensorConfig[SensorConfig.SENSOR_TEMPERATURE_IN]?.values
            val alTemperatureOutSensors = oc.hmSensorConfig[SensorConfig.SENSOR_TEMPERATURE_OUT]?.values
            val alSignalLevel = oc.hmSensorConfig[SensorConfig.SENSOR_SIGNAL_LEVEL]?.values
            val alNextCleaningDateTime = oc.hmSensorConfig[SensorConfig.SENSOR_NEXT_CLEAN_DATETIME]?.values
            val alSetupSensors = oc.hmSensorConfig[SensorConfig.SENSOR_SETUP]?.values

            val hsSensorIds = mutableSetOf<Int>()
            hsSensorIds += alStateSensors?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alDepthSensors?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alSpeedSensors?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alLoadSensors?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alTemperatureInSensors?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alTemperatureOutSensors?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alSignalLevel?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alNextCleaningDateTime?.map { sc -> sc.id } ?: emptyList()
            hsSensorIds += alSetupSensors?.map { sc -> sc.id } ?: emptyList()

            //--- упрощённый вариант работы только с последней строкой данных,
            //--- т.к. точно уверены, что (пока) только один контроллер на один объект и
            //--- во всех точках есть все данные
            val sql =
                """
                    SELECT ontime , sensor_data 
                    FROM TS_data_${oc.objectId} 
                    WHERE ontime = ( SELECT MAX(ontime) FROM TS_data_${oc.objectId} ) 
                """
//            val sql =
//                """
//                    SELECT ontime , sensor_data
//                    FROM TS_data_${oc.objectId}
//                    ORDER BY ontime DESC
//                """
            val inRs = conn.executeQuery(sql)
            while (inRs.next()) {
                val curTime = inRs.getInt(1)
                val bbIn = inRs.getByteBuffer(2, ByteOrder.BIG_ENDIAN)

                if (result.lastDateTime == null) {
                    result.lastDateTime = curTime
                }

                alStateSensors?.forEach { sc ->
                    if (hsSensorIds.contains(sc.id)) {
                        val scs = sc as SensorConfigState
                        val sensorData = AbstractObjectStateCalc.getSensorData(scs.portNum, bbIn)
                        if (sensorData != null) {
                            result.tmStateValue[scs.descr] = sensorData.toInt()
                            hsSensorIds.remove(sc.id)
                        }
                    }
                }

                getState(alStateSensors, bbIn, result.tmStateValue, hsSensorIds)
                getAnalogueState(alDepthSensors, bbIn, result.tmDepthValue, hsSensorIds)
                getAnalogueState(alSpeedSensors, bbIn, result.tmSpeedValue, hsSensorIds)
                getAnalogueState(alLoadSensors, bbIn, result.tmLoadValue, hsSensorIds)
                getAnalogueState(alTemperatureInSensors, bbIn, result.tmTemperatureInValue, hsSensorIds)
                getAnalogueState(alTemperatureOutSensors, bbIn, result.tmTemperatureOutValue, hsSensorIds)
                getAnalogueState(alSignalLevel, bbIn, result.tmSignalLevel, hsSensorIds)
                getDateTimeState(alNextCleaningDateTime, bbIn, result.tmNextCleaningDateTime, hsSensorIds)
                getSetupValues(alSetupSensors, bbIn, result.tmSetupValue, hsSensorIds)

                //--- values of all sensors readed
                if (hsSensorIds.isEmpty()) {
                    break
                }
                //--- not all sensor data readed, but time is over
                if (result.lastDateTime!! - curTime > MAX_VIEW_PERIOD) {
                    break
                }
            }
            inRs.close()

            return result
        }

        private fun getState(
            alStateSensors: Collection<SensorConfig>?,
            bbIn: AdvancedByteBuffer,
            tmState: SortedMap<String, Int>,
            hsSensorIds: MutableSet<Int>,
        ) {
            alStateSensors?.forEach { sc ->
                if (hsSensorIds.contains(sc.id)) {
                    val scs = sc as SensorConfigState
                    val sensorData = AbstractObjectStateCalc.getSensorData(scs.portNum, bbIn)
                    if (sensorData != null) {
                        tmState[scs.descr] = sensorData.toInt()
                        hsSensorIds.remove(sc.id)
                    }
                }
            }
        }

        private fun getAnalogueState(
            alAnalogueSensors: Collection<SensorConfig>?,
            bbIn: AdvancedByteBuffer,
            tmValue: SortedMap<String, Double>,
            hsSensorIds: MutableSet<Int>,
        ) {
            alAnalogueSensors?.forEach { sc ->
                if (hsSensorIds.contains(sc.id)) {
                    val sca = sc as SensorConfigAnalogue
                    val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bbIn)
                    if (sensorData != null) {
                        if (!ObjectCalc.isIgnoreSensorData(sca, sensorData.toDouble())) {
                            tmValue[sca.descr] = AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData.toDouble())
                            hsSensorIds.remove(sc.id)
                        }
                    }
                }
            }
        }

        private fun getDateTimeState(
            alDateTimeSensors: Collection<SensorConfig>?,
            bbIn: AdvancedByteBuffer,
            tmValue: SortedMap<String, Int>,
            hsSensorIds: MutableSet<Int>,
        ) {
            alDateTimeSensors?.forEach { sc ->
                if (hsSensorIds.contains(sc.id)) {
                    val sensorData = AbstractObjectStateCalc.getSensorData(sc.portNum, bbIn)
                    if (sensorData != null) {
                        tmValue[sc.descr] = sensorData.toInt()
                        hsSensorIds.remove(sc.id)
                    }
                }
            }
        }

        private fun getSetupValues(
            alSetupSensors: Collection<SensorConfig>?,
            bbIn: AdvancedByteBuffer,
            tmSetupValue: SortedMap<Int, String>,
            hsSensorIds: MutableSet<Int>,
        ) {
            alSetupSensors?.forEach { sc ->
                if (hsSensorIds.contains(sc.id)) {
                    val scs = sc as SensorConfigSetup
                    val sensorData = AbstractObjectStateCalc.getSensorData(scs.portNum, bbIn)
                    if (sensorData != null) {
                        tmSetupValue[scs.showPos] = when (scs.valueType) {
                            SensorConfigSetup.VALUE_TYPE_NUMBER -> {
                                getSplittedDouble(sensorData.toDouble(), scs.prec, true, '.')
                            }

                            SensorConfigSetup.VALUE_TYPE_BOOLEAN -> {
                                (sensorData.toInt() != 0).toString()
                            }

                            else -> {
                                getSplittedDouble(sensorData.toDouble(), scs.prec, true, '.')
                            }
                        }
                        hsSensorIds.remove(sc.id)
                    }
                }
            }
        }
    }
}
