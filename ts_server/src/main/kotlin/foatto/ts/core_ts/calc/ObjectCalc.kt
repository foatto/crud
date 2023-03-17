package foatto.ts.core_ts.calc

import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.graphic.server.graphic_handler.iGraphicHandler
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigBase
import foatto.ts.core_ts.sensor.config.SensorConfigState
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

class ObjectCalc(val objectConfig: ObjectConfig) {

    companion object {

        //--- the maximum allowable time interval between points
        const val MAX_WORK_TIME_INTERVAL = 10 * 60

        fun calcObject(conn: CoreAdvancedConnection, userConfig: UserConfig, oc: ObjectConfig, begTime: Int, endTime: Int): ObjectCalc {

            val result = ObjectCalc(oc)

//            val zoneId = getZoneId(userConfig.getUserProperty(UP_TIME_OFFSET)?.toIntOrNull())
//            val (alRawTime, alRawData) = loadAllSensorData(stm, oc, begTime, endTime)

            //--- some analogue sensors
//            oc.hmSensorConfig[SensorConfig.SENSOR_TEMPERATURE]?.values?.forEach { sc ->
//                val sca = sc as SensorConfigAnalogue
//                val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 2)
//                getSmoothAnalogGraphicData(
//                    alRawTime = alRawTime,
//                    alRawData = alRawData,
//                    scg = oc.scg,
//                    scsc = sca,
//                    begTime = begTime,
//                    endTime = endTime,
//                    xScale = 0,
//                    yScale = 0.0,
//                    aMinLimit = null,
//                    aMaxLimit = null,
//                    aPoint = null,
//                    aLine = aLine,
//                    gh = AnalogGraphicHandler()
//                )
//                result.tmTemperature[sc.descr] = aLine
//            }
//
//            oc.hmSensorConfig[SensorConfig.SENSOR_DENSITY]?.values?.forEach { sc ->
//                val sca = sc as SensorConfigAnalogue
//                val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 2)
//                getSmoothAnalogGraphicData(
//                    alRawTime = alRawTime,
//                    alRawData = alRawData,
//                    scg = oc.scg,
//                    scsc = sca,
//                    begTime = begTime,
//                    endTime = endTime,
//                    xScale = 0,
//                    yScale = 0.0,
//                    aMinLimit = null,
//                    aMaxLimit = null,
//                    aPoint = null,
//                    aLine = aLine,
//                    gh = AnalogGraphicHandler()
//                )
//                result.tmDensity[sc.descr] = aLine
//            }

            return result
        }

        fun loadAllSensorData(conn: CoreAdvancedConnection, oc: ObjectConfig, begTime: Int, endTime: Int): Pair<List<Int>, List<AdvancedByteBuffer>> {
            var maxSmoothTime = 0
            oc.hmSensorConfig.values.forEach { hmSC ->
                hmSC.values.forEach { sensorConfig ->
                    if (sensorConfig is SensorConfigBase) {
                        maxSmoothTime = max(maxSmoothTime, sensorConfig.smoothTime)
                    }
                }
            }

            //--- collect raw data
            val alRawTime = mutableListOf<Int>()
            val alRawData = mutableListOf<AdvancedByteBuffer>()

            //--- so that start / end points do not disappear in periods
            //--- and so that the final levels of one period and the initial levels of the next one coincide -
            //--- take ranges with a margin for smoothing
            val sql =
                """
                    SELECT ontime , sensor_data FROM TS_data_${oc.objectId} 
                    WHERE ontime >= ${begTime - maxSmoothTime} AND ontime <= ${endTime + maxSmoothTime} 
                    ORDER BY ontime
                """

            val inRs = conn.executeQuery(sql)
            while (inRs.next()) {
                alRawTime.add(inRs.getInt(1))
                alRawData.add(inRs.getByteBuffer(2, ByteOrder.BIG_ENDIAN))
            }
            inRs.close()

            return Pair(alRawTime, alRawData)
        }

        fun calcStateSensor(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scs: SensorConfigState,
            begTime: Int,
            endTime: Int,
        ): List<AbstractPeriodData> {

            val alResult = mutableListOf<AbstractPeriodData>()

            var curState = -1
            var stateBeginTime = 0

            var curTime = 0
            for (i in alRawTime.indices) {
                curTime = alRawTime[i]
                if (curTime < begTime) {
                    continue
                }
                if (curTime > endTime) {
                    if (i > 0) {
                        curTime = alRawTime[i - 1]
                    }
                    break
                }

                val newState = AbstractObjectStateCalc.getSensorData(scs.portNum, alRawData[i])?.toInt() ?: continue

                //--- new state
                if (newState != curState) {
                    //--- record of previous state
                    if (curState != -1) {
                        alResult.add(StatePeriodData(stateBeginTime, curTime, curState))
                    }
                    curState = newState
                    stateBeginTime = curTime
                }
            }

            //--- record of the last unclosed state
            if (curState != -1) {
                alResult.add(StatePeriodData(stateBeginTime, curTime, curState))
            }

//            //--- merging of work / downtime periods according to minimum durations
//            mergePeriods(alResult, scs.minOnTime, scs.minOffTime)

            return alResult
        }

        //--- smoothing analog value graph
        fun getSmoothAnalogGraphicData(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            sca: SensorConfigAnalogue,
            begTime: Int,
            endTime: Int,
            xScale: Int,
            yScale: Double,
            axisIndex: Int,
            aMinLimit: GraphicDataContainer?,
            aMaxLimit: GraphicDataContainer?,
            aLine: GraphicDataContainer?,
            graphicHandler: iGraphicHandler,
        ) {
            val isStaticMinLimit = graphicHandler.isStaticMinLimit(sca)
            val isStaticMaxLimit = graphicHandler.isStaticMaxLimit(sca)
            val isDynamicMinLimit = graphicHandler.isDynamicMinLimit(sca)
            val isDynamicMaxLimit = graphicHandler.isDynamicMaxLimit(sca)

            //--- immediately add / set static (permanent) constraints if required / supported
            if (graphicHandler.isStaticMinLimit(sca)) graphicHandler.setStaticMinLimit(sca, begTime, endTime, aMinLimit)
            if (graphicHandler.isStaticMaxLimit(sca)) graphicHandler.setStaticMaxLimit(sca, begTime, endTime, aMaxLimit)

            //--- for smoothing, you may need data before and after the time of the current point,
            //--- therefore, we overload / translate data in advance
            val alSensorData = mutableListOf<Double?>()
            for (bb in alRawData) {
                alSensorData.add(graphicHandler.getRawData(sca, bb))
            }

            val alGLD = aLine?.alGLD?.toMutableList() ?: mutableListOf()

            //--- raw data processing -----------------------------------------------------------------------------------------

            for (pos in alRawTime.indices) {
                val rawTime = alRawTime[pos]
                //--- immediately skip outrageous points loaded for seamless smoothing between adjacent ranges
                if (rawTime < begTime) {
                    continue
                }
                if (rawTime > endTime) {
                    break
                }

                //--- insert the first and last pseudo-points for seamless connection between periods

                //--- sensor value mb. == null, for example, ignored below / above the specified value boundaries (noise filtering)
                val rawData = alSensorData[pos] ?: continue

                //--- adding dynamic boundaries / value limits
                if (isDynamicMinLimit) {
                    //--- if the line point is far enough from the previous one (or just the first one :)
                    if (aMinLimit!!.alGLD.isEmpty() || rawTime - aMinLimit.alGLD[aMinLimit.alGLD.size - 1].x > xScale) {
                        graphicHandler.addDynamicMinLimit(rawTime, graphicHandler.getDynamicMinLimit(sca, rawTime, rawData), aMinLimit)
                    }
                }
                if (isDynamicMaxLimit) {
                    //--- if the line point is far enough from the previous one (or just the first one :)
                    if (aMaxLimit!!.alGLD.isEmpty() || rawTime - aMaxLimit.alGLD[aMaxLimit.alGLD.size - 1].x > xScale) {
                        graphicHandler.addDynamicMaxLimit(rawTime, graphicHandler.getDynamicMaxLimit(sca, rawTime, rawData), aMaxLimit)
                    }
                }

                //--- if lines are shown
                aLine?.let {
                    //--- finding the left border of the smoothing range
                    var pos1 = pos - 1
                    while (pos1 >= 0) {
                        if (rawTime - alRawTime[pos1] > sca.smoothTime) {
                            break
                        }
                        pos1--
                    }
                    //--- finding the right border of the smoothing range
                    var pos2 = pos + 1
                    while (pos2 < alRawTime.size) {
                        if (alRawTime[pos2] - rawTime > sca.smoothTime) {
                            break
                        }
                        pos2++
                    }

                    //--- smoothing
                    var sumValue: Double
                    var countValue: Int
                    val avgValue: Double
                    when (sca.smoothMethod) {
                        //--- since pos1 and pos2 are OUTSIDE the smoothing range,
                        //--- then we skip them, starting from pos1 + 1 and ending BEFORE pos2

                        SensorConfig.SMOOTH_METOD_MEDIAN -> {
                            val alSubList = mutableListOf<Double>()
                            for (p in pos1 + 1 until pos2) {
                                val v = alSensorData[p] ?: continue
                                alSubList.add(v)
                            }
                            alSubList.sort()
                            //--- if the number of values is odd, take exactly the middle
                            avgValue = if (alSubList.size % 2 != 0) {
                                alSubList[alSubList.size / 2]
                            }
                            //--- otherwise the arithmetic mean between two values closest to the middle
                            else {
                                val val1 = alSubList[alSubList.size / 2 - 1]
                                val val2 = alSubList[alSubList.size / 2]
                                val1 + (val2 - val1) / 2
                            }
                        }

                        SensorConfig.SMOOTH_METOD_AVERAGE -> {
                            sumValue = 0.0
                            countValue = 0
                            for (p in pos1 + 1 until pos2) {
                                val v = alSensorData[p] ?: continue
                                sumValue += v
                                countValue++
                            }
                            avgValue = sumValue / countValue
                        }

                        else -> avgValue = 0.0
                    }

                    val gldLast = alGLD.lastOrNull()

                    //--- if boundary values are set, we look at the averaged avgValue,
                    //--- so the typical getDynamicXXX from the beginning of the cycle does not suit us
                    val prevTime = (gldLast?.x ?: rawTime)
                    val prevData = gldLast?.y ?: avgValue
                    val curColorIndex = graphicHandler.getLineColorIndex(axisIndex, sca, rawTime, avgValue, prevTime, prevData)

                    if (gldLast == null || rawTime - gldLast.x > xScale || abs(rawData - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {
                        alGLD.add(GraphicLineData(rawTime, avgValue, curColorIndex))
                    }
                }
            }

            aLine?.alGLD = alGLD
        }

        private fun mergePeriods(alPD: MutableList<AbstractPeriodData>, minOnTime: Int, minOffTime: Int) {
            //--- ejection of insufficient on / off periods
            //--- and the subsequent merging of adjacent off / on, respectively
            while (true) {
                var isShortFound = false
                var i = 0
                while (i < alPD.size) {
                    val pd = alPD[i]
                    //--- if the period is too short to account for
                    if (pd.endTime - pd.begTime < (if (pd.getState() != 0) minOnTime else minOffTime)) {
                        //--- if there is at least one long opposite period nearby, then we remove the short one and connect the adjacent ones
                        var isLongFound = false
                        if (i > 0) {
                            val pdPrev = alPD[i - 1]
                            isLongFound = isLongFound or (pdPrev.endTime - pdPrev.begTime >= if (pdPrev.getState() != 0) minOnTime else minOffTime)
                        }
                        if (i < alPD.size - 1) {
                            val pdNext = alPD[i + 1]
                            isLongFound = isLongFound or (pdNext.endTime - pdNext.begTime >= if (pdNext.getState() != 0) minOnTime else minOffTime)
                        }
                        //--- found long neighbor (s)
                        if (isLongFound) {
                            //--- first short period
                            if (i == 0) {
                                alPD[1].begTime = alPD[0].begTime
                                alPD.removeAt(0)
                                i = 1    // the current period is already long, we go immediately further
                            } else if (i == alPD.size - 1) {    //--- middle short period
                                alPD[i - 1].endTime = alPD[i].endTime
                                alPD.removeAt(i)
                                i++    // the current period is already long, we go immediately further (although for the last period this is no longer necessary)
                            } else {   //--- last short period
                                alPD[i - 1].endTime = alPD[i + 1].endTime
                                alPD.removeAt(i)   // delete the current short period
                                alPD.removeAt(i)   // delete the next opposite period, merged with the previous opposite
                                //i++ - do not need to be done, since the current period is now new with an unknown duration
                            }
                            isShortFound = true    // deletion was, it makes sense to go through the chain again
                        } else {
                            i++ // no long neighbors found - let's move on
                        }
                    } else {
                        i++
                    }
                }
                //--- nothing more to throw away and connect
                if (!isShortFound) {
                    break
                }
            }
        }

        //--- define sensor data ignoring
        fun isIgnoreSensorData(scb: SensorConfigBase, sensorData: Double?): Boolean =
            if (sensorData == null) {
                true
            }
            //--- classic variant: if minIgnore < maxIgnore, then ignore below minIgnore or above maxIgnore
            else if (scb.minIgnore < scb.maxIgnore) {
                sensorData < scb.minIgnore || sensorData > scb.maxIgnore
            } else {
                //--- alternative: if minIgnore >= maxIgnore, then ignore between minIgnore and maxIgnore,
                //--- given the case, if minIgnore == maxIgnore, then ignore nothing
                sensorData < scb.minIgnore && sensorData > scb.maxIgnore
            }

        fun getPrecision(aValue: Double): Int {
            val value = abs(aValue)
            //--- updated / simplified version of the output accuracy - more cubic meters - in whole liters, less - in hundreds of milliliters / gram
            return if (value >= 1000) 0
            else if (value >= 100) 1
            else if (value >= 10) 2
            else 3
        }

    }
}
