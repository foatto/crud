//package foatto.server.calc
//
//import foatto.core.app.xy.XyProjection
//import foatto.core.app.xy.geom.XyPoint
//import foatto.mms.core_mms.ObjectConfig
//import foatto.mms.core_mms.sensor.config.SensorConfig
//import foatto.mms.core_mms.sensor.config.SensorConfigEnergoAnalogue
//import foatto.mms.core_mms.sensor.config.SensorConfigEnergoSummary
//import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
//import foatto.mms.core_mms.sensor.config.SensorConfigSignal
//import foatto.mms.core_mms.sensor.config.SensorConfigWork
//import foatto.sql.CoreAdvancedConnection
//import foatto.util.AdvancedByteBuffer
//import foatto.util.getAngle
//import foatto.util.getCurrentTimeInt
//import java.util.*
//
//class ObjectState {
//
//    var lastDataTime: Int? = null
//
//    var lastGeoTime: Int? = null
//    var pixPoint: XyPoint? = null
//    var speed: Int? = null
//    var angle: Double? = null
//
//    val tmSignalTime = sortedMapOf<String, Int>()
//    val tmSignalState = sortedMapOf<String, Boolean>()
//
//    val tmWorkTime = sortedMapOf<String, Int>()
//    val tmWorkState = sortedMapOf<String, Boolean>()
//
//    val tmLiquidTime = sortedMapOf<String, Int>()
//    val tmLiquidError = sortedMapOf<String, String>()
//    val tmLiquidLevel = sortedMapOf<String, Double>()
//
//    val tmLiquidUsingCounterTime = sortedMapOf<String, Int>()
//    val tmLiquidUsingCounterState = sortedMapOf<String, String>()
//
//    val tmEnergoCountADTime = sortedMapOf<String, Int>()
//    val tmEnergoCountADValue = sortedMapOf<String, Double>()
//    val tmEnergoCountARTime = sortedMapOf<String, Int>()
//    val tmEnergoCountARValue = sortedMapOf<String, Double>()
//    val tmEnergoCountRDTime = sortedMapOf<String, Int>()
//    val tmEnergoCountRDValue = sortedMapOf<String, Double>()
//    val tmEnergoCountRRTime = sortedMapOf<String, Int>()
//    val tmEnergoCountRRValue = sortedMapOf<String, Double>()
//
//    // sensor_descr -> phase -> value
//    val tmEnergoVoltageTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoVoltageValue = sortedMapOf<String, SortedMap<Int, Double>>()
//    val tmEnergoCurrentTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoCurrentValue = sortedMapOf<String, SortedMap<Int, Double>>()
//    val tmEnergoPowerKoefTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoPowerKoefValue = sortedMapOf<String, SortedMap<Int, Double>>()
//    val tmEnergoPowerActiveTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoPowerActiveValue = sortedMapOf<String, SortedMap<Int, Double>>()
//    val tmEnergoPowerReactiveTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoPowerReactiveValue = sortedMapOf<String, SortedMap<Int, Double>>()
//    val tmEnergoPowerFullTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoPowerFullValue = sortedMapOf<String, SortedMap<Int, Double>>()
//    val tmEnergoTransformKoefCurrentTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoTransformKoefCurrentValue = sortedMapOf<String, SortedMap<Int, Double>>()
//    val tmEnergoTransformKoefVoltageTime = sortedMapOf<String, SortedMap<Int, Int>>()
//    val tmEnergoTransformKoefVoltageValue = sortedMapOf<String, SortedMap<Int, Double>>()
//
//    companion object {
//
//        //--- the beginning of the time interval - no more than a one month
////        private const val DEFAULT_VIEW_PERIOD = 30 * 24 * 60 * 60
//        //--- the beginning of the time interval - no more than a one hour
//        private const val DEFAULT_VIEW_PERIOD = 60 * 60
//
//        fun getState(
//            conn: CoreAdvancedConnection,
//            oc: ObjectConfig,
//            viewPeriod: Int = DEFAULT_VIEW_PERIOD,
//        ): ObjectState {
//
//            val curTime = getCurrentTimeInt()
//            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(conn, oc, curTime - viewPeriod, curTime)
//
//            //--- store last existing data time
//            val result = ObjectState()
//            result.lastDataTime = alRawTime.lastOrNull()
//
//            //--- define GPS state
//            oc.scg?.let { scg ->
//                for (i in alRawTime.size - 1 downTo 0) {
//                    val time = alRawTime[i]
//                    val bbIn = alRawData[i]
//
//                    val gd = AbstractObjectStateCalc.getGeoData(scg, bbIn)
//
//                    if (gd != null && !(gd.wgs.x == 0 && gd.wgs.y == 0)) {
//                        if (result.lastGeoTime == null) {
//                            result.lastGeoTime = time
//                            result.pixPoint = XyProjection.wgs_pix(gd.wgs)
//                            if (scg.isUseSpeed) {
//                                result.speed = gd.speed
//                            }
//                        } else if (result.angle == null) { // only after finding previous point
//                            val prjPointPrev = XyProjection.wgs_pix(gd.wgs)
//                            //--- change the current angle if only the coordinates have completely changed,
//                            //--- otherwise, with the same old / new coordinates (i.e. standing still)
//                            //--- we always get the 0th angle, which is ugly
//                            if (result.pixPoint!!.x != prjPointPrev.x || result.pixPoint!!.y != prjPointPrev.y) {
//                                //--- the angle changes sign, because on the screen, the Y axis goes from top to bottom
//                                result.angle = if (result.pixPoint!!.x == prjPointPrev.x) {
//                                    if (result.pixPoint!!.y > prjPointPrev.y) 90.0 else -90.0
//                                } else {
//                                    getAngle((result.pixPoint!!.x - prjPointPrev.x).toDouble(), (result.pixPoint!!.y - prjPointPrev.y).toDouble())
//                                }
//                                //--- one last coords and angle is enough
//                                break
//                            }
//                        }
//                    }
//                }
//            }
//
//            //--- signal sensors
//            oc.hmSensorConfig[SensorConfig.SENSOR_SIGNAL]?.values?.forEach { sc ->
//                val scs = sc as SensorConfigSignal
//                for (i in alRawTime.size - 1 downTo 0) {
//                    val time = alRawTime[i]
//                    val bbIn = alRawData[i]
//
//                    val sensorData = AbstractObjectStateCalc.getSensorData(scs.portNum, bbIn)
//                    if (sensorData != null) {
//                        result.tmSignalTime[scs.descr] = time
//                        result.tmSignalState[scs.descr] = ObjectCalc.getSignalSensorValue(scs, sensorData.toDouble())
//                        break
//                    }
//                }
//            }
//
//            //--- equipment work sensors
//            oc.hmSensorConfig[SensorConfig.SENSOR_WORK]?.values?.forEach { sc ->
//                val scw = sc as SensorConfigWork
//                for (i in alRawTime.size - 1 downTo 0) {
//                    val time = alRawTime[i]
//                    val bbIn = alRawData[i]
//
//                    val sensorData = AbstractObjectStateCalc.getSensorData(scw.portNum, bbIn)
//                    if (sensorData != null) {
//                        result.tmWorkTime[scw.descr] = time
//                        result.tmWorkState[scw.descr] = ObjectCalc.getWorkSensorValue(scw, sensorData.toDouble())
//                        break
//                    }
//                }
//            }
//
//            //--- liquid level sensors
//            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
//                val sca = sc as SensorConfigLiquidLevel
//                for (i in alRawTime.size - 1 downTo 0) {
//                    val time = alRawTime[i]
//                    val bbIn = alRawData[i]
//
//                    val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bbIn)
//                    if (sensorData != null) {
//                        val troubleDescr = SensorConfigLiquidLevel.hmLLErrorCodeDescr[sensorData.toInt()]
//
//                        if (troubleDescr != null && curTime - time > SensorConfigLiquidLevel.hmLLMinSensorErrorTime[sensorData.toInt()]!!) {
//                            result.tmLiquidTime[sca.descr] = time
//                            result.tmLiquidError[sca.descr] = troubleDescr
//                            //--- значение не важно, ибо ошибка, лишь бы что-то было
//                            result.tmLiquidLevel[sca.descr] = 0.0
//                            break
//                        } else if (!ObjectCalc.isIgnoreSensorData(sca.minIgnore, sca.maxIgnore, sensorData.toDouble())) {
//                            result.tmLiquidTime[sca.descr] = time
//                            result.tmLiquidLevel[sca.descr] = AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData.toDouble())
//                            break
//                        }
//                    }
//                }
//            }
//
////!!! временно отключим - больше мешают, чем помогают
//            //--- liquid using counter's work state sensors
////            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE]?.values?.forEach { sc ->
////                for (i in alRawTime.size - 1 downTo 0) {
////                    val time = alRawTime[i]
////                    val bbIn = alRawData[i]
////
////                    val sensorData = AbstractObjectStateCalc.getSensorData(sc.portNum, bbIn)
////                    if (sensorData != null) {
////                        result.tmLiquidUsingCounterTime[sc.descr] = time
////                        result.tmLiquidUsingCounterState[sc.descr] = SensorConfigCounter.hmStatusDescr[sensorData] ?: "(неизвестный код состояния)"
////                        break
////                    }
////                }
////            }
//
//            //--- energo count sensors
//            getStateEnergoSummary(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AD,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoCountTime = result.tmEnergoCountADTime,
//                tmEnergoCountValue = result.tmEnergoCountADValue,
//            )
//            getStateEnergoSummary(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AR,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoCountTime = result.tmEnergoCountARTime,
//                tmEnergoCountValue = result.tmEnergoCountARValue,
//            )
//            getStateEnergoSummary(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RD,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoCountTime = result.tmEnergoCountRDTime,
//                tmEnergoCountValue = result.tmEnergoCountRDValue,
//            )
//            getStateEnergoSummary(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RR,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoCountTime = result.tmEnergoCountRRTime,
//                tmEnergoCountValue = result.tmEnergoCountRRValue,
//            )
//
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoVoltageTime,
//                tmEnergoAnalogueValue = result.tmEnergoVoltageValue,
//            )
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoCurrentTime,
//                tmEnergoAnalogueValue = result.tmEnergoCurrentValue,
//            )
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoPowerKoefTime,
//                tmEnergoAnalogueValue = result.tmEnergoPowerKoefValue,
//            )
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoPowerActiveTime,
//                tmEnergoAnalogueValue = result.tmEnergoPowerActiveValue,
//            )
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoPowerReactiveTime,
//                tmEnergoAnalogueValue = result.tmEnergoPowerReactiveValue,
//            )
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoPowerFullTime,
//                tmEnergoAnalogueValue = result.tmEnergoPowerFullValue,
//            )
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoTransformKoefCurrentTime,
//                tmEnergoAnalogueValue = result.tmEnergoTransformKoefCurrentValue,
//            )
//            getStateEnergoAnalogue(
//                oc = oc,
//                sensorType = SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                tmEnergoAnalogueTime = result.tmEnergoTransformKoefVoltageTime,
//                tmEnergoAnalogueValue = result.tmEnergoTransformKoefVoltageValue,
//            )
//
//            return result
//        }
//
//        private fun getStateEnergoSummary(
//            oc: ObjectConfig,
//            sensorType: Int,
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            tmEnergoCountTime: SortedMap<String, Int>,
//            tmEnergoCountValue: SortedMap<String, Double>,
//        ) {
//            oc.hmSensorConfig[sensorType]?.values?.forEach { sc ->
//                val sces = sc as SensorConfigEnergoSummary
//                for (i in alRawTime.size - 1 downTo 0) {
//                    val time = alRawTime[i]
//                    val bbIn = alRawData[i]
//
//                    val sensorData = AbstractObjectStateCalc.getSensorData(sces.portNum, bbIn)
//                    if (sensorData != null && !ObjectCalc.isIgnoreSensorData(sces.minIgnore, sces.maxIgnore, sensorData.toDouble())) {
//                        tmEnergoCountTime[sces.descr] = time
//                        tmEnergoCountValue[sces.descr] = AbstractObjectStateCalc.getSensorValue(sces.alValueSensor, sces.alValueData, sensorData.toDouble())
//                        break
//                    }
//                }
//            }
//        }
//
//        private fun getStateEnergoAnalogue(
//            oc: ObjectConfig,
//            sensorType: Int,
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            tmEnergoAnalogueTime: SortedMap<String, SortedMap<Int, Int>>,
//            tmEnergoAnalogueValue: SortedMap<String, SortedMap<Int, Double>>,
//        ) {
//            oc.hmSensorConfig[sensorType]?.values?.forEach { sc ->
//                val sces = sc as SensorConfigEnergoAnalogue
//                for (i in alRawTime.size - 1 downTo 0) {
//                    val time = alRawTime[i]
//                    val bbIn = alRawData[i]
//
//                    val sensorData = AbstractObjectStateCalc.getSensorData(sces.portNum, bbIn)
//                    if (sensorData != null && !ObjectCalc.isIgnoreSensorData(sces.minIgnore, sces.maxIgnore, sensorData.toDouble())) {
//                        val tmEnergoCountPhaseTime = tmEnergoAnalogueTime.getOrPut(sces.descr) { sortedMapOf() }
//                        tmEnergoCountPhaseTime[sces.phase] = time
//
//                        val tmEnergoAnaloguePhaseValue = tmEnergoAnalogueValue.getOrPut(sces.descr) { sortedMapOf() }
//                        tmEnergoAnaloguePhaseValue[sces.phase] = AbstractObjectStateCalc.getSensorValue(sces.alValueSensor, sces.alValueData, sensorData.toDouble())
//                        break
//                    }
//                }
//            }
//        }
//    }
//}
