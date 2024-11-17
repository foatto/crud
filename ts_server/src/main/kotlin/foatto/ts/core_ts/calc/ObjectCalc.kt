package foatto.ts.core_ts.calc

import foatto.core.app.chart.ChartElementDTO
import foatto.core.app.chart.ChartElementLineDTO
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.graphic.server.graphic_handler.iGraphicHandler
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigBase
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.util.AdvancedByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

class ObjectCalc(val objectConfig: ObjectConfig) {

    companion object {

        //--- the maximum allowable time interval between points
        const val MAX_WORK_TIME_INTERVAL = 10 * 60

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
            aMinLimit: ChartElementDTO?,
            aMaxLimit: ChartElementDTO?,
            aLine: ChartElementDTO?,
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
                    val alSubList = mutableListOf<Double>()
                    for (p in pos1 + 1 until pos2) {
                        val v = alSensorData[p] ?: continue
                        alSubList.add(v)
                    }
                    alSubList.sort()
                    //--- if the number of values is odd, take exactly the middle
                    val avgValue = if (alSubList.size % 2 != 0) {
                        alSubList[alSubList.size / 2]
                    }
                    //--- otherwise the arithmetic mean between two values closest to the middle
                    else {
                        val val1 = alSubList[alSubList.size / 2 - 1]
                        val val2 = alSubList[alSubList.size / 2]
                        val1 + (val2 - val1) / 2
                    }

                    val gldLast = alGLD.lastOrNull()

                    //--- if boundary values are set, we look at the averaged avgValue,
                    //--- so the typical getDynamicXXX from the beginning of the cycle does not suit us
                    val prevTime = (gldLast?.x ?: rawTime)
                    val prevData = (gldLast?.y ?: avgValue).toDouble()
                    val curColorIndex = graphicHandler.getLineColorIndex(axisIndex, sca, rawTime, avgValue, prevTime, prevData)

                    if (gldLast == null || rawTime - gldLast.x > xScale || abs(rawData - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {
                        alGLD.add(ChartElementLineDTO(rawTime, avgValue.toFloat(), curColorIndex))
                    }
                }
            }

            aLine?.alGLD = alGLD
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

    }
}
