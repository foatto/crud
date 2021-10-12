package foatto.mms.core_mms.calc

import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.geom.XyPoint
import foatto.core.util.getAngle
import foatto.core.util.getCurrentTimeInt
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import foatto.mms.core_mms.sensor.config.SensorConfigSignal
import foatto.mms.core_mms.sensor.config.SensorConfigWork
import foatto.sql.CoreAdvancedStatement

class ObjectState {

    var lastDataTime: Int? = null

    var lastGeoTime: Int? = null
    var pixPoint: XyPoint? = null
    var speed: Int? = null
    var angle: Double? = null

    val tmSignalTime = sortedMapOf<String, Int>()
    val tmSignalState = sortedMapOf<String, Boolean>()

    val tmWorkTime = sortedMapOf<String, Int>()
    val tmWorkState = sortedMapOf<String, Boolean>()

    val tmLiquidTime = sortedMapOf<String, Int>()
    val tmLiquidError = sortedMapOf<String, String>()
    val tmLiquidLevel = sortedMapOf<String, Double>()

    val tmLiquidUsingCounterTime = sortedMapOf<String, Int>()
    val tmLiquidUsingCounterState = sortedMapOf<String, String>()

    companion object {

        //--- the beginning of the time interval - no more than a month
        private const val DEFAULT_VIEW_PERIOD = 30 * 24 * 60 * 60

        fun getState(
            stm: CoreAdvancedStatement,
            oc: ObjectConfig,
            viewPeriod: Int = DEFAULT_VIEW_PERIOD,
        ): ObjectState {

            val curTime = getCurrentTimeInt()
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, oc, curTime - viewPeriod, curTime)

            //--- store last existing data time
            val result = ObjectState()
            result.lastDataTime = alRawTime.lastOrNull()

            //--- define GPS state
            oc.scg?.let { scg ->
                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val gd = AbstractObjectStateCalc.getGeoData(scg, bbIn)

                    if (gd != null && !(gd.wgs.x == 0 && gd.wgs.y == 0)) {
                        if (result.lastGeoTime == null) {
                            result.lastGeoTime = time
                            result.pixPoint = XyProjection.wgs_pix(gd.wgs)
                            if (scg.isUseSpeed) {
                                result.speed = gd.speed
                            }
                        } else if (result.angle == null) { // only after finding previous point
                            val prjPointPrev = XyProjection.wgs_pix(gd.wgs)
                            //--- change the current angle if only the coordinates have completely changed,
                            //--- otherwise, with the same old / new coordinates (i.e. standing still)
                            //--- we always get the 0th angle, which is ugly
                            if (result.pixPoint!!.x != prjPointPrev.x || result.pixPoint!!.y != prjPointPrev.y) {
                                //--- the angle changes sign, because on the screen, the Y axis goes from top to bottom
                                result.angle = if (result.pixPoint!!.x == prjPointPrev.x) {
                                    if (result.pixPoint!!.y > prjPointPrev.y) 90.0 else -90.0
                                } else {
                                    getAngle((result.pixPoint!!.x - prjPointPrev.x).toDouble(), (result.pixPoint!!.y - prjPointPrev.y).toDouble())
                                }
                                //--- one last coords and angle is enough
                                break
                            }
                        }
                    }
                }
            }

            //--- signal sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_SIGNAL]?.values?.forEach { sc ->
                val scs = sc as SensorConfigSignal
                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val sensorData = AbstractObjectStateCalc.getSensorData(scs.portNum, bbIn)
                    if (sensorData != null) {
                        result.tmSignalTime[scs.descr] = time
                        result.tmSignalState[scs.descr] = ObjectCalc.getSignalSensorValue(scs, sensorData.toDouble())
                        break
                    }
                }
            }

            //--- equipment work sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_WORK]?.values?.forEach { sc ->
                val scw = sc as SensorConfigWork
                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val sensorData = AbstractObjectStateCalc.getSensorData(scw.portNum, bbIn)
                    if (sensorData != null) {
                        result.tmWorkTime[scw.descr] = time
                        result.tmWorkState[scw.descr] = ObjectCalc.getWorkSensorValue(scw, sensorData.toDouble())
                        break
                    }
                }
            }

            //--- liquid level sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
                val sca = sc as SensorConfigLiquidLevel
                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bbIn)
                    if (sensorData != null) {
                        val troubleDescr = SensorConfigLiquidLevel.hmLLErrorCodeDescr[sensorData.toInt()]

                        if (troubleDescr != null && curTime - time > SensorConfigLiquidLevel.hmLLMinSensorErrorTime[sensorData.toInt()]!!) {
                            result.tmLiquidTime[sca.descr] = time
                            result.tmLiquidError[sca.descr] = troubleDescr
                            //--- значение не важно, ибо ошибка, лишь бы что-то было
                            result.tmLiquidLevel[sca.descr] = 0.0
                            break
                        } else if (!ObjectCalc.isIgnoreSensorData(sca, sensorData.toDouble())) {
                            result.tmLiquidTime[sca.descr] = time
                            result.tmLiquidLevel[sca.descr] = AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData.toDouble())
                            break
                        }
                    }
                }
            }

            //--- liquid using counter's work state sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE]?.values?.forEach { sc ->
                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val sensorData = AbstractObjectStateCalc.getSensorData(sc.portNum, bbIn)
                    if (sensorData != null) {
                        result.tmLiquidUsingCounterTime[sc.descr] = time
                        result.tmLiquidUsingCounterState[sc.descr] = SensorConfigCounter.hmStatusDescr[sensorData] ?: "(неизвестный код состояния)"
                        break
                    }
                }
            }

            return result
        }
    }
}
