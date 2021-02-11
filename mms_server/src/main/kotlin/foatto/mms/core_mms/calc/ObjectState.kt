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
import java.util.*

class ObjectState {

    var lastDataTime: Int? = null

    var lastGeoTime: Int? = null
    var pixPoint: XyPoint? = null
    var speed: Int? = null
    var angle: Double? = null

    var tmSignalTime = TreeMap<String, Int>()
    var tmSignalState = TreeMap<String, Boolean>()

    var tmWorkTime = TreeMap<String, Int>()
    var tmWorkState = TreeMap<String, Boolean>()

    var tmLiquidTime = TreeMap<String, Int>()
    var tmLiquidError = TreeMap<String, String>()
    var tmLiquidLevel = TreeMap<String, Double>()

    var tmLiquidUsingCounterTime = TreeMap<String, Int>()
    var tmLiquidUsingCounterState = TreeMap<String, String>()

    companion object {

//        //--- если нормальных точек не найдётся, то пусть время будет из далёкого будущего,
//        //--- чтобы неопределённое положение на карте не светилось
//        //--- ( а чтобы не вылезала ошибка переполнения целого
//        //--- при задании начального времени как Long.MAX_VALUE / 1000,
//        //--- сделаем максимум как Integer.MAX_VALUE )
//        private const val MAX_TIME = Integer.MAX_VALUE

        //--- the beginning of the time interval - no more than a year
        private const val DEFAULT_VIEW_PERIOD = 365 * 24 * 60 * 60

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
                            //result.tmLiquidError[sca.descr] = null
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

//        //--- работаем только с активной базой
//        fun getState(stm: CoreAdvancedStatement, oc: ObjectConfig): ObjectState {
//
//            val result = ObjectState()
//
//            //--- датчики сигналов
//            val hmSCS = oc.hmSensorConfig[SensorConfig.SENSOR_SIGNAL]
//            //--- датчики работы оборудования
//            val hmSCW = oc.hmSensorConfig[SensorConfig.SENSOR_WORK]
//            //--- датчики уровня жидкости
//            val hmSCLL = oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]
//
//            //--- будем ползти назад по времени увеличивающимся шагом LIMIT,
//            //--- пока не найдём хоть какое-то последнее положение
//            var lastTime = Integer.MAX_VALUE
//            //--- 2 последние точки могут быть одинаковыми с петролайн-приборов ( линейная точка+CUR_COORDS ),
//            //--- поэтому начнём запрашивать сразу с 4-х последних точек
//            var lastLimit = 4
//            while (true) {
//                //--- флаг наличия данных
//                var isDataExist = false
//                val inRs = stm.executeQuery(
//                    " SELECT ${stm.getPreLimit(lastLimit)} ontime , sensor_data " +
//                        " FROM MMS_data_${oc.objectId} " +
//                        " WHERE ontime < $lastTime ${stm.getMidLimit(lastLimit)} " +
//                        " ORDER BY ontime DESC ${stm.getPostLimit(lastLimit)} "
//                )
//                while (inRs.next()) {
//                    //--- данные вообще есть, ещё есть смысл крутиться дальше
//                    isDataExist = true
//
//                    lastTime = inRs.getInt(1)
//                    val bbSensor = inRs.getByteBuffer(2, ByteOrder.BIG_ENDIAN)
//
//                    //--- один раз запоминаем последнее время
//                    if (result.lastDataTime == MAX_TIME) result.lastDataTime = lastTime
//
//                    //--- если прописан гео-датчик и ещё не все данные определены
//                    if (oc.scg != null && (result.pixPoint == null || result.objectAngle == null)) {
//                        val gd = AbstractObjectStateCalc.getGeoData(oc.scg!!, bbSensor)
//                        //--- самих геоданных в этой строке может и не оказаться
//                        if (gd != null) {
//                            //--- сбор данных по последней/основной точке
//                            if (result.pixPoint == null) {
//                                result.pixPoint = XyProjection.wgs_pix(gd.wgs)
//                                if (oc.scg!!.isUseSpeed) result.speed = gd.speed
//                            }
//                            //--- вычисление недостающего угла поворота а/м
//                            //--- ( только после того, как будет найдена предыдущая последняя точка )
//                            else if (result.objectAngle == null) {
//                                val prjPointPrev = XyProjection.wgs_pix(gd.wgs)
//                                //--- текущий угол меняем если только координаты полностью сменились,
//                                //--- иначе при совпадающих старых/новых координатах ( т.е. стоянии на месте )
//                                //--- получим всегда 0-й угол, что некрасиво
//                                if (result.pixPoint!!.x != prjPointPrev.x || result.pixPoint!!.y != prjPointPrev.y)
//                                //--- угол меняет знак, т.к. на экране ось Y идет сверху вниз
//                                    result.objectAngle = if (result.pixPoint!!.x == prjPointPrev.x) (if (result.pixPoint!!.y > prjPointPrev.y) 90.0 else -90.0)
//                                    else getAngle((result.pixPoint!!.x - prjPointPrev.x).toDouble(), (result.pixPoint!!.y - prjPointPrev.y).toDouble())
//                            }
//                        }
//                    }
//                    //--- если прописаны датчики сигналов
//                    if (hmSCS != null && hmSCS.size != result.tmSignalState.size) {
//                        for (portNum in hmSCS.keys) {
//                            val scs = hmSCS[portNum] as SensorConfigSignal
//                            if (result.tmSignalState[scs.descr] == null) {
//                                val sensorData = AbstractObjectStateCalc.getSensorData(scs.portNum, bbSensor)?.toDouble()
//                                result.tmSignalState[scs.descr] = getSignalSensorValue(scs, sensorData)
//                            }
//                        }
//                    }
//                    //--- если прописаны датчики работы оборудования
//                    if (hmSCW != null && hmSCW.size != result.tmWorkState.size) {
//                        for (portNum in hmSCW.keys) {
//                            val scw = hmSCW[portNum] as SensorConfigWork
//                            if (result.tmWorkState[scw.descr] == null) {
//                                val sensorData = AbstractObjectStateCalc.getSensorData(scw.portNum, bbSensor)?.toDouble()
//                                result.tmWorkState[scw.descr] = getWorkSensorValue(scw, sensorData)
//                            }
//                        }
//                    }
//                    //--- если прописаны датчики уровня жидкости
//                    if (hmSCLL != null && hmSCLL.size != result.tmLiquidLevel.size) {
//                        for (portNum in hmSCLL.keys) {
//                            val sca = hmSCLL[portNum] as SensorConfigLiquidLevel
//                            if (result.tmLiquidLevel[sca.descr] == null) {
//                                //--- ручной разбор сырых данных
//                                val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bbSensor)?.toDouble() ?: continue
//                                //--- проверка на ошибку
//                                val troubleDescr = SensorConfigLiquidLevel.hmLLErrorCodeDescr[sensorData.toInt()]
//                                //--- если есть ошибка и она уже достаточное время
//                                if (troubleDescr != null && getCurrentTimeInt() - result.lastDataTime > SensorConfigLiquidLevel.hmLLMinSensorErrorTime[sensorData.toInt()]!!) {
//                                    result.tmLiquidError[sca.descr] = troubleDescr
//                                    //--- значение не важно, ибо ошибка, лишь бы что-то было
//                                    result.tmLiquidLevel[sca.descr] = 0.0
//                                    result.tmLiquidDim[sca.descr] = "-"
//                                } else if (!isIgnoreSensorData(sca, sensorData)) {
//                                    result.tmLiquidLevel[sca.descr] = AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData)
//                                    result.tmLiquidDim[sca.descr] = "-"
//                                }//--- вручную игнорируем заграничные значения
//                            }
//                        }
//                    }
//                }
//                inRs.close()
//
//                //--- если данных вообще больше нет или нашлись все последние требуемые данные, то выходим
//                if (!isDataExist) break
//                if ((oc.scg == null || result.pixPoint != null && result.objectAngle != null) &&
//                    (hmSCS == null || hmSCS.size == result.tmSignalState.size) &&
//                    (hmSCW == null || hmSCW.size == result.tmWorkState.size) &&
//                    (hmSCLL == null || hmSCLL.size == result.tmLiquidLevel.size)
//                )
//
//                    break
//
//                //--- иначе продолжаем со следующей увеличенной порцией LIMIT
//                lastLimit *= 2
//            }
//
//            return result
//        }
    }
}
