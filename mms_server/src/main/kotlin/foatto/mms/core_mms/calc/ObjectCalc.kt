package foatto.mms.core_mms.calc

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.app.graphic.GraphicPointData
import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.geom.XyPoint
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSBFromIterable
import foatto.core.util.getSplittedDouble
import foatto.core.util.getSplittedLong
import foatto.core.util.secondIntervalToString
import foatto.core_server.app.server.UserConfig
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.ZoneLimitData
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.LiquidGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.iGraphicHandler
import foatto.mms.core_mms.sensor.config.*
import foatto.sql.CoreAdvancedStatement
import java.nio.ByteOrder
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class ObjectCalc(val objectConfig: ObjectConfig) {

    var gcd: GeoCalcData? = null
    val tmWork = sortedMapOf<String, WorkCalcData>()
    val tmEnergo = sortedMapOf<String, Double>()
    val tmLiquidUsing = sortedMapOf<String, Double>()   // liquid using by sensor descr
    val tmLiquidLevel = sortedMapOf<String, LiquidLevelCalcData>()

    val tmGroupSum = sortedMapOf<String, CalcSumData>() // sums by group
    val allSumData = CalcSumData()                  // overall sum

    val tmTemperature = sortedMapOf<String, GraphicDataContainer>()
    val tmDensity = sortedMapOf<String, GraphicDataContainer>()

    var sGeoName = ""
    var sGeoRun = ""
    var sGeoOutTime = ""
    var sGeoInTime = ""
    var sGeoWayTime = ""
    var sGeoMovingTime = ""
    var sGeoParkingTime = ""
    var sGeoParkingCount = ""

    var sWorkName = ""
    var sWorkValue = ""

    var sEnergoName = ""
    var sEnergoValue = ""

    var sAllSumEnergoName = ""
    var sAllSumEnergoValue = ""

    var sLiquidUsingName = ""
    var sLiquidUsingValue = ""

    var sAllSumLiquidName = ""
    var sAllSumLiquidValue = ""

    var sLiquidLevelName = ""
    var sLiquidLevelBeg = ""
    var sLiquidLevelEnd = ""
    var sLiquidLevelIncTotal = ""
    var sLiquidLevelDecTotal = ""
    var sLiquidLevelUsingTotal = ""
    var sLiquidLevelUsingCalc = ""

    var sLiquidLevelLiquidName = ""
    var sLiquidLevelLiquidInc = ""
    var sLiquidLevelLiquidDec = ""

    companion object {

        //--- maximum allowable distance between points
        private const val MAX_RUN = 100_000 // 100 km = 30 minutes (see MAX_WORK_TIME_INTERVAL) at 200 km/h

        //--- the maximum allowable time interval between points, over which:
        //--- 1.for geo-sensors - the mileage is not counted for this period
        //--- 2. for sensors of equipment operation - this period is considered inoperative, regardless of the current state of the point
        //--- 3. for fuel level sensors - this period is considered inoperative (not consumption, not refueling, not draining) and the level change is not included in any amount
        const val MAX_WORK_TIME_INTERVAL = 30 * 60

        //--- maximum duration of the previous "normal" period,
        //--- used to calculate the average fuel consumption during refueling / draining
        private const val MAX_CALC_PREV_NORMAL_PERIOD = 3 * 60 * 60

        fun calcObject(stm: CoreAdvancedStatement, userConfig: UserConfig, oc: ObjectConfig, begTime: Int, endTime: Int): ObjectCalc {

            val result = ObjectCalc(oc)

            val (alRawTime, alRawData) = loadAllSensorData(stm, oc, begTime, endTime)

            //--- if geo-sensors are registered - we sum up the mileage
            oc.scg?.let { scg ->
                calcGeo(alRawTime, alRawData, scg, begTime, endTime, result)
            }

            //--- equipment operation sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_WORK]?.values?.forEach { sc ->
                calcWork(alRawTime, alRawData, sc as SensorConfigWork, begTime, endTime, result)
            }

            //--- sensors - electricity meters
            listOf(
                SensorConfig.SENSOR_ENERGO_COUNT_AD,
                SensorConfig.SENSOR_ENERGO_COUNT_AR,
                SensorConfig.SENSOR_ENERGO_COUNT_RD,
                SensorConfig.SENSOR_ENERGO_COUNT_RR
            ).forEach { sensorType ->
                oc.hmSensorConfig[sensorType]?.values?.forEach { sc ->
                    calcEnergo(alRawTime, alRawData, sc as SensorConfigEnergoSummary, begTime, endTime, result)
                }
            }

            //--- liquid calc sensor
            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING]?.values?.forEach { sc ->
                calcLiquidCalcSensor(alRawTime, alRawData, sc as SensorConfigCounter, begTime, endTime, result)
            }

            //--- volume accumulated values sensor
            oc.hmSensorConfig[SensorConfig.SENSOR_VOLUME_ACCUMULATED]?.values?.forEach { sc ->
                calcLiquidSummary(alRawTime, alRawData, sc as SensorConfigLiquidSummary, begTime, endTime, result)
            }

            //--- mass accumulated values sensor
            oc.hmSensorConfig[SensorConfig.SENSOR_MASS_ACCUMULATED]?.values?.forEach { sc ->
                calcLiquidSummary(alRawTime, alRawData, sc as SensorConfigLiquidSummary, begTime, endTime, result)
            }

            //--- liquid level sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
                calcLiquidLevel(alRawTime, alRawData, stm, oc, sc as SensorConfigLiquidLevel, begTime, endTime, result)
            }

            //--- some analogue sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_TEMPERATURE]?.values?.forEach { sc ->
                val sca = sc as SensorConfigAnalogue
                val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 2)
                getSmoothAnalogGraphicData(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    scg = oc.scg,
                    scsc = sca,
                    begTime = begTime,
                    endTime = endTime,
                    xScale = 0,
                    yScale = 0.0,
                    aMinLimit = null,
                    aMaxLimit = null,
                    aPoint = null,
                    aLine = aLine,
                    gh = AnalogGraphicHandler()
                )
                result.tmTemperature[sc.descr] = aLine
            }

            oc.hmSensorConfig[SensorConfig.SENSOR_DENSITY]?.values?.forEach { sc ->
                val sca = sc as SensorConfigAnalogue
                val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 2)
                getSmoothAnalogGraphicData(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    scg = oc.scg,
                    scsc = sca,
                    begTime = begTime,
                    endTime = endTime,
                    xScale = 0,
                    yScale = 0.0,
                    aMinLimit = null,
                    aMaxLimit = null,
                    aPoint = null,
                    aLine = aLine,
                    gh = AnalogGraphicHandler()
                )
                result.tmDensity[sc.descr] = aLine
            }

            if (oc.scg != null) {
                fillGeoString(userConfig, result)
            }
            fillWorkString(userConfig, result)
            fillEnergoString(userConfig, result)
            fillLiquidUsingString(userConfig, result)
            fillLiquidLevelString(userConfig, result)

            return result
        }

        fun loadAllSensorData(stm: CoreAdvancedStatement, oc: ObjectConfig, begTime: Int, endTime: Int): Pair<List<Int>, List<AdvancedByteBuffer>> {
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
                " SELECT ontime , sensor_data FROM MMS_data_${oc.objectId} " +
                    " WHERE ontime >= ${begTime - maxSmoothTime} AND ontime <= ${endTime + maxSmoothTime} " +
                    " ORDER BY ontime "

            val inRs = stm.executeQuery(sql)
            while (inRs.next()) {
                alRawTime.add(inRs.getInt(1))
                alRawData.add(inRs.getByteBuffer(2, ByteOrder.BIG_ENDIAN))
            }
            inRs.close()

            return Pair(alRawTime, alRawData)
        }

        fun calcGeoSensor(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scg: SensorConfigGeo,
            begTime: Int,
            endTime: Int,
            scale: Int,
            maxEnabledOverSpeed: Int,
            alZoneSpeedLimit: List<ZoneLimitData>?
        ): GeoCalcData {

            var begRun = 0
            var lastRun = 0
            var run = 0

            var movingBeginTime = 0
            var parkingBeginTime = 0
            var parkingCoord: XyPoint? = null

            val alMovingAndParking = mutableListOf<AbstractPeriodData>()
            val alOverSpeed = mutableListOf<AbstractPeriodData>()
            val alPointTime = mutableListOf<Int>()
            val alPointXY = mutableListOf<XyPoint>()
            val alPointSpeed = mutableListOf<Int>()
            val alPointOverSpeed = mutableListOf<Int>()

            var normalSpeedBeginTime = 0
            var overSpeedBeginTime = 0
            var maxOverSpeedTime = 0
            var maxOverSpeedCoord: XyPoint? = null
            var maxOverSpeedMax = 0
            var maxOverSpeedDiff = 0

            val lastPoint = XyPoint(0, 0)

            var lastTime = 0
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

                val gd = AbstractObjectStateCalc.getGeoData(scg, alRawData[i]) ?: continue
                // --- Nuances of calculating the mileage:
                // --- 1. Regardless of the mileage display method (relative / point-to-point or absolute):
                // --- 1.1. We ignore the runs between points with too long a time interval.
                // --- 1.2. Ignore points with too high / unrealistic distance between points.
                // --- 2. Devices that give absolute mileage in their readings manage to reset it in the middle of the day,
                // --- you have to catch each such reset (the algorithm is somewhat similar to the search for refueling / draining),
                // --- but skipping sudden points with zero mileage at absolutely normal coordinates.
                if (gd.distance > 0) {
                    if (scg.isAbsoluteRun) {
                        val curRun = gd.distance

                        if (begRun == 0) {
                            begRun = curRun
                        } else if (curRun < lastRun || curRun - lastRun > MAX_RUN || lastTime != 0 && curTime - lastTime > MAX_WORK_TIME_INTERVAL) {
                            run += lastRun - begRun
                            begRun = curRun
                        }
                        lastRun = curRun
                    } else if (gd.distance < MAX_RUN) {
                        run += gd.distance
                    }
                }

                val pixPoint = XyProjection.wgs_pix(gd.wgs)

                var overSpeed = 0

                //--- on parking ?
                if (gd.speed <= AbstractObjectStateCalc.MAX_SPEED_AS_PARKING) {
                    //--- recording previous movement
                    if (movingBeginTime != 0) {
                        alMovingAndParking.add(GeoPeriodData(movingBeginTime, curTime, 1))
                        movingBeginTime = 0
                    }
                    //--- parking start (parkingBeginTime - parking flag)
                    if (parkingBeginTime == 0) {
                        parkingBeginTime = curTime
                        parkingCoord = pixPoint
                    }
                    //--- parking = end of excess and, as it were, the beginning of normal speed
                    if (overSpeedBeginTime != 0) {
                        alOverSpeed.add(OverSpeedPeriodData(overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff))
                        overSpeedBeginTime = 0
                        maxOverSpeedTime = 0
                        maxOverSpeedCoord = null
                        maxOverSpeedMax = 0
                        maxOverSpeedDiff = 0
                    }
                    if (normalSpeedBeginTime == 0) {
                        normalSpeedBeginTime = curTime
                    }
                } else {
                    //--- previous parking record
                    if (parkingBeginTime != 0) {
                        alMovingAndParking.add(GeoPeriodData(parkingBeginTime, curTime, parkingCoord!!))
                        parkingBeginTime = 0
                        parkingCoord = null
                    }
                    //--- start of movement
                    if (movingBeginTime == 0) {
                        movingBeginTime = curTime
                    }

                    //--- overspeed handling
                    overSpeed = calcOverSpeed(scg.maxSpeedLimit, alZoneSpeedLimit, pixPoint, gd.speed)
                    if (overSpeed > maxEnabledOverSpeed) {
                        //--- we will record the previous normal movement
                        if (normalSpeedBeginTime != 0) {
                            alOverSpeed.add(OverSpeedPeriodData(normalSpeedBeginTime, curTime))
                            normalSpeedBeginTime = 0
                        }
                        //--- overspeed start mark
                        if (overSpeedBeginTime == 0) {
                            overSpeedBeginTime = curTime
                        }
                        //--- saving the value / coordinates / time of maximum speeding on the site
                        if (overSpeed > maxOverSpeedDiff) {
                            maxOverSpeedTime = curTime
                            maxOverSpeedCoord = pixPoint
                            maxOverSpeedMax = gd.speed
                            maxOverSpeedDiff = overSpeed
                        }
                    } else {
                        if (overSpeedBeginTime != 0) {
                            alOverSpeed.add(OverSpeedPeriodData(overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff))
                            overSpeedBeginTime = 0
                            maxOverSpeedTime = 0
                            maxOverSpeedCoord = null
                            maxOverSpeedMax = 0
                            maxOverSpeedDiff = 0
                        }
                        if (normalSpeedBeginTime == 0) {
                            normalSpeedBeginTime = curTime
                        }
                    }
                }
                //--- only points with movement are recorded for the trajectory
                if (gd.speed > AbstractObjectStateCalc.MAX_SPEED_AS_PARKING && pixPoint.distance(lastPoint) > scale) {
                    alPointTime.add(curTime)
                    alPointXY.add(pixPoint)
                    alPointSpeed.add(gd.speed)
                    alPointOverSpeed.add(if (overSpeed < 0) 0 else overSpeed)

                    lastPoint.set(pixPoint)
                }

                lastTime = curTime
            }
            //--- summarize the sub-run of the last (i.e. unfinished) range
            if (scg.isAbsoluteRun) run += lastRun - begRun
            //--- record of the last unclosed event
            if (movingBeginTime != 0) alMovingAndParking.add(GeoPeriodData(movingBeginTime, curTime, 1))
            if (parkingBeginTime != 0) alMovingAndParking.add(GeoPeriodData(parkingBeginTime, curTime, parkingCoord!!))
            if (normalSpeedBeginTime != 0) alOverSpeed.add(OverSpeedPeriodData(normalSpeedBeginTime, curTime))
            if (overSpeedBeginTime != 0) alOverSpeed.add(OverSpeedPeriodData(overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff))

            mergePeriods(alMovingAndParking, scg.minMovingTime, scg.minParkingTime)
            mergePeriods(alOverSpeed, scg.minOverSpeedTime, max(10, scg.minOverSpeedTime / 10))

            //--- calculation of other indicators: time of departure / arrival, in motion / in the parking lot, number of parking lots
            var outTime = 0
            var inTime = 0
            var movingTime = 0
            var parkingCount = 0
            var parkingTime = 0
            for (i in alMovingAndParking.indices) {
                val pd = alMovingAndParking[i] as GeoPeriodData

                if (pd.getState() != 0) {
                    if (outTime == 0) outTime = pd.begTime
                    inTime = pd.endTime
                    movingTime += pd.endTime - pd.begTime
                } else {
                    parkingCount++
                    parkingTime += pd.endTime - pd.begTime
                }
            }

            //--- we convert the sums of meters into km (if the mileage from this sensor is not used, then we will make it negative)
            return GeoCalcData(
                group = scg.group,
                descr = scg.descr,
                run = run / 1000.0 * scg.runKoef,
                outTime = outTime,
                inTime = inTime,
                movingTime = movingTime,
                parkingCount = parkingCount,
                parkingTime = parkingTime,
                alMovingAndParking = alMovingAndParking,
                alOverSpeed = alOverSpeed,
                alPointTime = alPointTime,
                alPointXY = alPointXY,
                alPointSpeed = alPointSpeed,
                alPointOverSpeed = alPointOverSpeed
            )
        }

        fun calcWorkSensor(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scw: SensorConfigWork,
            begTime: Int,
            endTime: Int,
        ): WorkCalcData {

            val alResult = mutableListOf<AbstractPeriodData>()

            var workBeginTime = 0
            var delayBeginTime = 0

            var lastTime = 0
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

                val sensorData = AbstractObjectStateCalc.getSensorData(scw.portNum, alRawData[i])?.toDouble()

                // --- manually ignore:
                // --- 1. too long intervals between points (except for the first point)
                // --- 2. foreign values
                val curValue = sensorData != null &&
                    (lastTime == 0 || curTime - lastTime <= MAX_WORK_TIME_INTERVAL) &&
                    getWorkSensorValue(scw, sensorData)

                //--- still in work?
                if (curValue) {
                    //--- record of previous downtime
                    if (delayBeginTime != 0) {
                        alResult.add(WorkPeriodData(delayBeginTime, curTime, 0))
                        delayBeginTime = 0
                    }
                    //--- Beginning of work
                    if (workBeginTime == 0) {
                        workBeginTime = curTime
                    }
                } else {
                    //--- record of previous work
                    if (workBeginTime != 0) {
                        alResult.add(WorkPeriodData(workBeginTime, curTime, 1))
                        workBeginTime = 0
                    }
                    //--- start of downtime
                    if (delayBeginTime == 0) {
                        delayBeginTime = curTime
                    }
                }
                lastTime = curTime
            }

            //--- record of the last unclosed event
            if (workBeginTime != 0) {
                alResult.add(WorkPeriodData(workBeginTime, curTime, 1))
            }
            if (delayBeginTime != 0) {
                alResult.add(WorkPeriodData(delayBeginTime, curTime, 0))
            }

            //--- merging of work / downtime periods according to minimum durations
            mergePeriods(alResult, scw.minOnTime, scw.minOffTime)

            return WorkCalcData(scw.group, alResult)
        }

        fun calcEnergoSensor(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            sces: SensorConfigEnergoSummary,
            begTime: Int,
            endTime: Int
        ): Double {
            //--- counters that give absolute values in their readings can reset it on command or overflow.
            //--- you will have to catch each such reset (the algorithm is somewhat similar to the search for refueling / draining)
            //--- also skip sudden dots with a zero counter
            var begE = 0.0
            var lastE = 0.0
            var energo = 0.0

            for (i in alRawTime.indices) {
                val curTime = alRawTime[i]
                if (curTime < begTime) continue
                if (curTime > endTime) break

                val sensorData = AbstractObjectStateCalc.getSensorData(sces.portNum, alRawData[i])?.toDouble() ?: continue
                if (isIgnoreSensorData(sces, sensorData)) continue

                val sensorValue = AbstractObjectStateCalc.getSensorValue(sces.alValueSensor, sces.alValueData, sensorData)

                if (begE == 0.0) {
                    begE = sensorValue
                } else if (sensorValue < lastE) {
                    energo += lastE - begE
                    begE = sensorValue
                }
                lastE = sensorValue

            }
            energo += lastE - begE

            return energo
        }

        fun calcLiquidLevelSensor(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            stm: CoreAdvancedStatement,
            oc: ObjectConfig,
            sca: SensorConfigLiquidLevel,
            begTime: Int,
            endTime: Int,
        ): LiquidLevelCalcData {

            val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 2)
            val alLSPD = mutableListOf<LiquidStatePeriodData>()
            getSmoothLiquidGraphicData(alRawTime, alRawData, oc.scg, sca, begTime, endTime, aLine, alLSPD)

            val llcd = LiquidLevelCalcData(sca.containerType, aLine, alLSPD)
            calcLiquidUsingByLevel(sca, llcd, stm, oc, begTime, endTime)

            return llcd
        }

        //--- another public part -----------------------------------------------------------------------------------------------------------

        //--- collect the value of the maximum excess for a given point (taking into account the zones with a speed limit)
        fun calcOverSpeed(maxSpeedConst: Int, alZoneSpeedLimit: List<ZoneLimitData>?, prjPoint: XyPoint, speed: Int): Int {
            //--- collecting the maximum excess
            var maxOverSpeed = -Integer.MAX_VALUE

            //--- looking for an excess among the permanent limit
            maxOverSpeed = max(maxOverSpeed, speed - maxSpeedConst)
            //--- looking for speed limit zones
            alZoneSpeedLimit?.let {
                for (zd in alZoneSpeedLimit) {
                    //--- if there are restrictions on the duration of the action, then we will check their entry,
                    //--- and if we do not enter the interval (s) of the zone, then we go to the next zone
                    //if(  ! checkZoneInTime(  zd, pointTime  )  ) continue; - пока не применяется
                    //--- check the geometric entry into the zone
                    if (!zd.zoneData!!.polygon!!.isContains(prjPoint)) continue
                    //--- check for excess
                    maxOverSpeed = max(maxOverSpeed, speed - zd.maxSpeed)
                }
            }
            return maxOverSpeed
        }

        //--- smoothing analog value graph
        fun getSmoothAnalogGraphicData(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scg: SensorConfigGeo?,
            scsc: SensorConfigAnalogue,
            begTime: Int,
            endTime: Int,
            xScale: Int,
            yScale: Double,
            aMinLimit: GraphicDataContainer?,
            aMaxLimit: GraphicDataContainer?,
            aPoint: GraphicDataContainer?,
            aLine: GraphicDataContainer?,
            gh: iGraphicHandler
        ) {

            val isStaticMinLimit = gh.isStaticMinLimit(scsc)
            val isStaticMaxLimit = gh.isStaticMaxLimit(scsc)
            val isDynamicMinLimit = gh.isDynamicMinLimit(scsc)
            val isDynamicMaxLimit = gh.isDynamicMaxLimit(scsc)

            //--- immediately add / set static (permanent) constraints if required / supported
            if (gh.isStaticMinLimit(scsc)) gh.setStaticMinLimit(scsc, begTime, endTime, aMinLimit)
            if (gh.isStaticMaxLimit(scsc)) gh.setStaticMaxLimit(scsc, begTime, endTime, aMaxLimit)

            //--- for smoothing, you may need data before and after the time of the current point,
            //--- therefore, we overload / translate data in advance
            val alSensorData = mutableListOf<Double?>()
            for (bb in alRawData) alSensorData.add(gh.getRawData(scsc, bb))

            val alGPD = aPoint?.alGPD?.toMutableList() ?: mutableListOf()
            val alGLD = aLine?.alGLD?.toMutableList() ?: mutableListOf()

            //--- raw data processing -----------------------------------------------------------------------------------------

            for (pos in alRawTime.indices) {
                val rawTime = alRawTime[pos]
                //--- immediately skip outrageous points loaded for seamless smoothing between adjacent ranges
                if (rawTime < begTime) continue
                if (rawTime > endTime) break

                //--- insert the first and last pseudo-points for seamless connection between periods

                //--- sensor value mb. == null, for example, ignored below / above the specified value boundaries (noise filtering)
                val rawData = alSensorData[pos] ?: continue

                //--- adding dynamic boundaries / value limits
                if (isDynamicMinLimit) {
                    //--- if the line point is far enough from the previous one (or just the first one :)
                    if (aMinLimit!!.alGLD.isEmpty() || rawTime - aMinLimit.alGLD[aMinLimit.alGLD.size - 1].x > xScale)
                        gh.addDynamicMinLimit(rawTime, gh.getDynamicMinLimit(scsc, rawTime, rawData), aMinLimit)
                }
                if (isDynamicMaxLimit) {
                    //--- if the line point is far enough from the previous one (or just the first one :)
                    if (aMaxLimit!!.alGLD.isEmpty() || rawTime - aMaxLimit.alGLD[aMaxLimit.alGLD.size - 1].x > xScale)
                        gh.addDynamicMaxLimit(rawTime, gh.getDynamicMaxLimit(scsc, rawTime, rawData), aMaxLimit)
                }

                //--- if points are shown
                aPoint?.let {
                    val colorIndex = if (isStaticMinLimit && rawData < gh.getStaticMinLimit(scsc) ||
                        isStaticMaxLimit && rawData > gh.getStaticMaxLimit(scsc) ||
                        isDynamicMinLimit && rawData < gh.getDynamicMinLimit(scsc, rawTime, rawData) ||
                        isDynamicMaxLimit && rawData > gh.getDynamicMaxLimit(scsc, rawTime, rawData)
                    ) GraphicColorIndex.POINT_ABOVE
                    else GraphicColorIndex.POINT_NORMAL

                    val gpdLast = if (alGPD.isEmpty()) null else alGPD[alGPD.size - 1]

                    if (gpdLast == null || rawTime - gpdLast.x > xScale || abs(rawData - gpdLast.y) > yScale || colorIndex != gpdLast.colorIndex)
                        alGPD.add(GraphicPointData(rawTime, rawData, colorIndex))
                }

                //--- if lines are shown
                aLine?.let {
                    //--- finding the left border of the smoothing range
                    var pos1 = pos - 1
                    while (pos1 >= 0) {
                        if (rawTime - alRawTime[pos1] > scsc.smoothTime) break
                        pos1--
                    }
                    //--- finding the right border of the smoothing range
                    var pos2 = pos + 1
                    while (pos2 < alRawTime.size) {
                        if (alRawTime[pos2] - rawTime > scsc.smoothTime) break
                        pos2++
                    }

                    //--- smoothing
                    var sumValue: Double
                    var countValue: Int
                    val avgValue: Double
                    when (scsc.smoothMethod) {
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

                        SensorConfig.SMOOTH_METOD_AVERAGE_SQUARE -> {
                            sumValue = 0.0
                            countValue = 0
                            for (p in pos1 + 1 until pos2) {
                                val v = alSensorData[p] ?: continue
                                sumValue += v * v
                                countValue++
                            }
                            avgValue = sqrt(sumValue / countValue)
                        }

                        SensorConfig.SMOOTH_METOD_AVERAGE_GEOMETRIC -> {
                            sumValue = 1.0   // there will be a multiplication, so the initial value = 1
                            countValue = 0
                            for (p in pos1 + 1 until pos2) {
                                val v = alSensorData[p] ?: continue
                                sumValue *= v
                                countValue++
                            }
                            avgValue = sumValue.pow(1.0 / countValue)
                        }

                        else -> avgValue = 0.0
                    }

                    val gldLast = if (alGLD.isEmpty()) null else alGLD[alGLD.size - 1]

                    //--- if boundary values are set, we look at the averaged avgValue,
                    //--- so the typical getDynamicXXX from the beginning of the cycle does not suit us
                    val prevTime = (gldLast?.x ?: rawTime)
                    val prevData = gldLast?.y ?: avgValue
                    val curColorIndex = gh.getLineColorIndex(scsc, rawTime, avgValue, prevTime, prevData)

                    if (gldLast == null || rawTime - gldLast.x > xScale || abs(rawData - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {
                        val gd = scg?.let {
                            AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
                        }
                        alGLD.add(GraphicLineData(rawTime, avgValue, curColorIndex, gd?.let { XyProjection.wgs_pix(gd.wgs) }))
                    }
                }
            }

            aPoint?.alGPD = alGPD.toTypedArray()
            aLine?.alGLD = alGLD.toTypedArray()
        }

        //--- we collect periods of liquid level states (refueling, draining, consumption) and apply filters for refueling / draining / consumption
        fun getLiquidStatePeriodData(
            sca: SensorConfigLiquidLevel,
            aLine: GraphicDataContainer,
            alLSPD: MutableList<LiquidStatePeriodData>,
            gh: LiquidGraphicHandler
        ) {
            //--- zero pass: collecting periods from points; we start from the 1st point, because 0th point is always "normal"
            var begPos = 0
            var curColorIndex = gh.lineNormalColorIndex
            for (i in 1 until aLine.alGLD.size) {
                val gdl = aLine.alGLD[i]
                val newColorIndex = gdl.colorIndex
                //--- a period of a new type has begun, we end the previous period of a different type
                if (newColorIndex != curColorIndex) {
                    //--- the previous period ended at the previous point
                    val endPos = i - 1
                    //--- there must be at least two points in the period, we discard one-point periods (usually this is the starting point in the "normal" state)
                    if (begPos < endPos) alLSPD.add(LiquidStatePeriodData(begPos, endPos, curColorIndex))
                    //--- the new period actually starts from the previous point
                    begPos = i - 1
                    curColorIndex = newColorIndex
                }
            }
            //--- let's finish the last period
            val endPos = aLine.alGLD.size - 1
            if (begPos < endPos) alLSPD.add(LiquidStatePeriodData(begPos, endPos, curColorIndex))

            //--- first pass: turn insignificant fillings / drains into "normal" consumption
            run {
                var pos = 0
                while (pos < alLSPD.size) {
                    val lspd = alLSPD[pos]
                    //--- skip empty or normal periods immediately
                    if (lspd.colorIndex == gh.lineNoneColorIndex || lspd.colorIndex == gh.lineNormalColorIndex) {
                        pos++
                        continue
                    }
                    //--- determine the insignificance of filling / draining
                    val begGDL = aLine.alGLD[lspd.begPos]
                    val endGDL = aLine.alGLD[lspd.endPos]
                    var isFound = false
                    if (lspd.colorIndex == gh.lineCriticalColorIndex) {
                        //--- at the same time we catch periods with zero length
                        isFound = endGDL.y - begGDL.y < sca.detectIncMinDiff || endGDL.x - begGDL.x < max(sca.detectIncMinLen, 1)
                    } else if (lspd.colorIndex == gh.lineWarningColorIndex) {
                        //--- at the same time we catch periods with zero length
                        isFound = -(endGDL.y - begGDL.y) < sca.detectDecMinDiff || endGDL.x - begGDL.x < max(sca.detectDecMinLen, 1)
                    }
                    //--- insignificant fill / drain found
                    if (isFound) {
                        //--- looking for possible normal left / right periods for merging
                        var prevNormalLSPD: LiquidStatePeriodData? = null
                        var nextNormalLSPD: LiquidStatePeriodData? = null
                        if (pos > 0) {
                            prevNormalLSPD = alLSPD[pos - 1]
                            if (prevNormalLSPD.colorIndex != gh.lineNormalColorIndex) prevNormalLSPD = null
                        }
                        if (pos < alLSPD.size - 1) {
                            nextNormalLSPD = alLSPD[pos + 1]
                            if (nextNormalLSPD.colorIndex != gh.lineNormalColorIndex) nextNormalLSPD = null
                        }
                        //--- both adjacent periods are normal, all three are merged into one
                        if (prevNormalLSPD != null && nextNormalLSPD != null) {
                            prevNormalLSPD.endPos = nextNormalLSPD.endPos
                            alLSPD.removeAt(pos)
                            //--- this is not a typo or an error: after deleting the current period, the next period becomes the current one and is also deleted
                            alLSPD.removeAt(pos)
                            //--- after merging two periods, pos already points to the next position, there is no need to increase the counter
                            //pos++;
                        } else if (prevNormalLSPD != null) {    //--- no normal neighbors, we normalize ourselves
                            prevNormalLSPD.endPos = lspd.endPos
                            alLSPD.removeAt(pos)
                            //--- after merging two periods, pos already points to the next position, there is no need to increase the counter
                            //pos++;
                        } else if (nextNormalLSPD != null) {    //--- the right period is normal, we merge with it
                            nextNormalLSPD.begPos = lspd.begPos
                            alLSPD.removeAt(pos)
                            pos++
                        } else {                                //--- the left period is normal, we merge with it
                            lspd.colorIndex = gh.lineNormalColorIndex
                            pos++
                        }
                        //--- in any case, normalize "our" points of the smoothed graph
                        for (i in lspd.begPos + 1..lspd.endPos) aLine.alGLD[i].colorIndex = gh.lineNormalColorIndex
                    } else pos++    //--- otherwise just go to the next period
                }
            }

            //--- second pass - we lengthen refueling and drainage by reducing neighboring normal periods
            for (pos in alLSPD.indices) {
                val lspd = alLSPD[pos]
                //--- skip empty or normal periods immediately
                if (lspd.colorIndex == gh.lineNoneColorIndex || lspd.colorIndex == gh.lineNormalColorIndex) continue

                //--- looking for a normal period on the left, if necessary
                val addTimeBefore = if (lspd.colorIndex == gh.lineCriticalColorIndex) sca.incAddTimeBefore
                else sca.decAddTimeBefore
                if (addTimeBefore > 0 && pos > 0) {
                    val prevNormalLSPD = alLSPD[pos - 1]
                    if (prevNormalLSPD.colorIndex == gh.lineNormalColorIndex) {
                        val bt = aLine.alGLD[lspd.begPos].x
                        // --- lengthen the beginning of our period, shorten the previous normal period from the end
                        // --- namely>, not> =, in order to prevent single-point normal periods (begPos == endPos)
                        // --- after lengthening the current abnormal
                        var p = prevNormalLSPD.endPos - 1
                        while (p > prevNormalLSPD.begPos) {
                            if (bt - aLine.alGLD[p].x > addTimeBefore) break
                            p--
                        }
                        //--- the previous position is valid
                        p++
                        //--- is there where to lengthen?
                        if (p < prevNormalLSPD.endPos) {
                            prevNormalLSPD.endPos = p
                            lspd.begPos = p
                            //--- in any case, let's re-mark "our" points of the smoothed graph
                            for (i in lspd.begPos + 1..lspd.endPos) aLine.alGLD[i].colorIndex = lspd.colorIndex
                        }
                    }
                }
                //--- looking for a normal period on the right, if necessary
                val addTimeAfter = if (lspd.colorIndex == gh.lineCriticalColorIndex) sca.incAddTimeAfter
                else sca.decAddTimeAfter
                if (addTimeAfter > 0 && pos < alLSPD.size - 1) {
                    val nextNormalLSPD = alLSPD[pos + 1]
                    if (nextNormalLSPD.colorIndex == gh.lineNormalColorIndex) {
                        val et = aLine.alGLD[lspd.endPos].x
                        //--- lengthen the end of our period, shorten the next normal period from the beginning
                        //--- exactly <, not <=, in order to prevent single-point normal periods (begPos == endPos)
                        //--- after lengthening the current abnormal
                        var p = nextNormalLSPD.begPos + 1
                        while (p < nextNormalLSPD.endPos) {
                            if (aLine.alGLD[p].x - et > addTimeAfter) break
                            p++
                        }
                        //--- the previous position is valid
                        p--
                        //--- is there where to lengthen?
                        if (p > nextNormalLSPD.begPos) {
                            nextNormalLSPD.begPos = p
                            lspd.endPos = p
                            //--- in any case, let's re-mark "our" points of the smoothed graph
                            for (i in lspd.begPos + 1..lspd.endPos) aLine.alGLD[i].colorIndex = lspd.colorIndex
                        }
                    }
                }
            }

            //--- third pass: remove insignificant (short) "normal" periods between identical abnormal
            var pos = 0
            while (pos < alLSPD.size) {
                val lspd = alLSPD[pos]
                //--- skip abnormal periods immediately
                if (lspd.colorIndex != gh.lineNormalColorIndex) {
                    pos++
                    continue
                }
                //--- determine the insignificance of the expense
                val begGDL = aLine.alGLD[lspd.begPos]
                val endGDL = aLine.alGLD[lspd.endPos]
                //--- at the same time we catch periods with zero length
                if (endGDL.x - begGDL.x < max(sca.usingMinLen, 1)) {
                    //--- looking for abnormal periods left / right for merging
                    var prevAbnormalLSPD: LiquidStatePeriodData? = null
                    var nextAbnormalLSPD: LiquidStatePeriodData? = null
                    if (pos > 0) {
                        prevAbnormalLSPD = alLSPD[pos - 1]
                        if (prevAbnormalLSPD.colorIndex == gh.lineNormalColorIndex) prevAbnormalLSPD = null
                    }
                    if (pos < alLSPD.size - 1) {
                        nextAbnormalLSPD = alLSPD[pos + 1]
                        if (nextAbnormalLSPD.colorIndex == gh.lineNormalColorIndex) nextAbnormalLSPD = null
                    }

                    //--- both neighboring periods are equally abnormal, all three are merged into one (two neighboring differently abnormal periods cannot be merged)
                    if (prevAbnormalLSPD != null && nextAbnormalLSPD != null && prevAbnormalLSPD.colorIndex == nextAbnormalLSPD.colorIndex) {

                        prevAbnormalLSPD.endPos = nextAbnormalLSPD.endPos
                        alLSPD.removeAt(pos)
                        //--- this is not a typo or an error: after deleting the current period, the next period becomes the current one and is also deleted
                        alLSPD.removeAt(pos)
                        //--- after merging three periods, pos already points to the next position, there is no need to increase the counter
                        //pos++;
                        //--- denormalize "our" points of the smoothed graph
                        for (i in lspd.begPos + 1..lspd.endPos) aLine.alGLD[i].colorIndex = prevAbnormalLSPD.colorIndex
                    } else pos++    //--- otherwise just go to the next period
                } else pos++    //--- otherwise just go to the next period
            }
        }

        //--- we collect periods, values and place of refueling / draining
        fun calcIncDec(
            stm: CoreAdvancedStatement,
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            oc: ObjectConfig,
            sca: SensorConfigLiquidLevel,
            begTime: Int,
            endTime: Int,
            isWaybill: Boolean,
            alBeg: List<Int>,
            alEnd: List<Int>,
            calcMode: Int,
            hmZoneData: Map<Int, ZoneData>,
            calcZoneID: Int
        ): List<LiquidIncDecData> {
            val alLIDD = mutableListOf<LiquidIncDecData>()

            val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 2)
            val alLSPD = mutableListOf<LiquidStatePeriodData>()
            getSmoothLiquidGraphicData(alRawTime, alRawData, oc.scg, sca, begTime, endTime, aLine, alLSPD)

            val llcd = LiquidLevelCalcData(sca.containerType, aLine, alLSPD)
            calcLiquidUsingByLevel(sca, llcd, stm, oc, begTime, endTime)

            for (lspd in llcd.alLSPD!!) {
                val begGLD = llcd.aLine!!.alGLD[lspd.begPos]
                val endGLD = llcd.aLine!!.alGLD[lspd.endPos]
                var lidd: LiquidIncDecData? = null
                if (lspd.colorIndex == GraphicColorIndex.LINE_ABOVE_0 && calcMode >= 0) {
                    lidd = LiquidIncDecData(begGLD.x, endGLD.x, begGLD.y, endGLD.y)
                } else if (lspd.colorIndex == GraphicColorIndex.LINE_BELOW_0 && calcMode <= 0) {
                    lidd = LiquidIncDecData(begGLD.x, endGLD.x, begGLD.y, endGLD.y)
                }

                if (lidd != null) {
                    var inZoneAll = false
                    val tsZoneName = TreeSet<String>()
                    oc.scg?.let { scg ->
                        for (pos in lspd.begPos..lspd.endPos) {
                            val gd = AbstractObjectStateCalc.getGeoData(scg, alRawData[pos]) ?: continue
                            val pixPoint = XyProjection.wgs_pix(gd.wgs)

                            val inZone = fillZoneList(hmZoneData, calcZoneID, pixPoint, tsZoneName)
                            //--- filter by geofences, if specified
                            if (calcZoneID != 0 && inZone) inZoneAll = true
                        }
                    }
                    //--- filter by geofences, if specified
                    if (calcZoneID != 0 && !inZoneAll) continue

                    //--- filter by directions time, if set
                    if (isWaybill) {
                        var inWaybill = false
                        for (wi in alBeg.indices) if (lidd.begTime < alEnd[wi] && lidd.endTime > alBeg[wi]) {
                            inWaybill = true
                            break
                        }
                        if (inWaybill) continue
                    }

                    lidd.objectConfig = oc
                    lidd.sca = sca
                    lidd.sbZoneName = getSBFromIterable(tsZoneName, ", ")

                    alLIDD.add(lidd)
                }
            }

            return alLIDD
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

        // --- (new condition - not academically / uselessly Ignore, but consider that equipment outside the specified limits DOES NOT WORK)
        //if(  sensorData < scw.minIgnore || sensorData > scw.maxIgnore  ) continue;
        fun getWorkSensorValue(scw: SensorConfigWork, sensorData: Double?): Boolean =
            sensorData != null &&
                if (scw.minIgnore < scw.maxIgnore) {
                    sensorData > scw.minIgnore && sensorData < scw.maxIgnore
                } else {
                    sensorData > scw.minIgnore || sensorData < scw.maxIgnore
                } &&
                ((scw.activeValue == 0) xor (sensorData > scw.boundValue))

        fun getSignalSensorValue(scs: SensorConfigSignal, sensorData: Double?): Boolean =
            sensorData != null &&
                if (scs.minIgnore < scs.maxIgnore) {
                    sensorData > scs.minIgnore && sensorData < scs.maxIgnore
                } else {
                    sensorData > scs.minIgnore || sensorData < scs.maxIgnore
                } &&
                ((scs.activeValue == 0) xor (sensorData > scs.boundValue))

        //--- private part -----------------------------------------------------------------------------------------------------------

        private fun calcGeo(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scg: SensorConfigGeo,
            begTime: Int,
            endTime: Int,
            result: ObjectCalc
        ) {
            //--- in normal calculations, we do not need trajectory points, so we give the maximum scale.
            //--- excess is also not needed, so we give maxEnabledOverSpeed = 0
            result.gcd = calcGeoSensor(alRawTime, alRawData, scg, begTime, endTime, 1_000_000_000, 0, null)

            //--- if the standard for liquid (fuel) is set, we will calculate it
            if (scg.isUseRun && scg.liquidName.isNotEmpty() && scg.liquidNorm != 0.0) {
                val liquidUsing = scg.liquidNorm * result.gcd!!.run / 100.0

                result.tmLiquidUsing["${scg.descr} (расч.) ${scg.liquidName}"] = liquidUsing
                addLiquidUsingSum(scg.group, scg.liquidName, liquidUsing, result)
            }

        }

        private fun calcWork(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scw: SensorConfigWork,
            begTime: Int,
            endTime: Int,
            result: ObjectCalc
        ) {
            val wcd = calcWorkSensor(alRawTime, alRawData, scw, begTime, endTime)
            result.tmWork[scw.descr] = wcd

            //--- needed to determine the uniqueness of equipment in a group to calculate the average fuel consumption
            val groupSum = result.tmGroupSum.getOrPut(scw.group) { CalcSumData() }
            groupSum.tmWork[scw.descr] = wcd.onTime

            //--- do not needed
            //result.allSumData.tmWork[scw.descr] = wcd.onTime

            //--- if the standard for liquid (fuel) is set, we will calculate it
            if (scw.liquidName.isNotEmpty() && scw.liquidNorm != 0.0) {
                val liquidUsing = scw.liquidNorm * wcd.onTime / 60.0 / 60.0

                result.tmLiquidUsing["${scw.descr} (расч.) ${scw.liquidName}"] = liquidUsing
                addLiquidUsingSum(scw.group, scw.liquidName, liquidUsing, result)
            }
        }

        private fun calcEnergo(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            sces: SensorConfigEnergoSummary,
            begTime: Int,
            endTime: Int,
            result: ObjectCalc
        ) {
            val e = calcEnergoSensor(alRawTime, alRawData, sces, begTime, endTime)

            result.tmEnergo[sces.descr] = e

            //--- calculate the group amount
            val groupSum = result.tmGroupSum.getOrPut(sces.group) { CalcSumData() }
            val byType = groupSum.tmEnergo.getOrPut(sces.sensorType) { sortedMapOf<Int, Double>() }
            val byPhase = byType[sces.phase] ?: 0.0
            byType[sces.phase] = byPhase + e

            val byTypeAll = result.allSumData.tmEnergo.getOrPut(sces.sensorType) { sortedMapOf<Int, Double>() }
            val byPhaseAll = byTypeAll[sces.phase] ?: 0.0
            byTypeAll[sces.phase] = byPhaseAll + e
        }

        private fun calcLiquidCalcSensor(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scu: SensorConfigCounter,
            begTime: Int,
            endTime: Int,
            result: ObjectCalc
        ) {
            //--- if the name of the liquid (fuel) is given, then add its consumption to the general list
            if (scu.liquidName.isNotEmpty()) {
                val liquidUsing = getSensorCountData(alRawTime, alRawData, scu, begTime, endTime)

                result.tmLiquidUsing["${scu.descr} ${scu.liquidName}"] = liquidUsing
                addLiquidUsingSum(scu.group, scu.liquidName, liquidUsing, result)
            }
        }

        private fun calcLiquidSummary(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scls: SensorConfigLiquidSummary,
            begTime: Int,
            endTime: Int,
            result: ObjectCalc
        ) {
            if (scls.liquidName.isNotEmpty()) {
                val liquidUsing = calcLiquidAccumulatedSensor(alRawTime, alRawData, scls, begTime, endTime)

                result.tmLiquidUsing["${scls.descr} ${scls.liquidName}"] = liquidUsing
                addLiquidUsingSum(scls.group, scls.liquidName, liquidUsing, result)
            }
        }

        private fun calcLiquidLevel(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            stm: CoreAdvancedStatement,
            oc: ObjectConfig,
            scll: SensorConfigLiquidLevel,
            begTime: Int,
            endTime: Int,
            result: ObjectCalc
        ) {
            val llcd = calcLiquidLevelSensor(alRawTime, alRawData, stm, oc, scll, begTime, endTime)

            result.tmLiquidLevel[scll.descr] = llcd

            result.tmLiquidUsing["${scll.descr} ${scll.liquidName}"] = llcd.usingTotal
            addLiquidUsingSum(scll.group, scll.liquidName, llcd.usingTotal, result)

            val groupSum = result.tmGroupSum.getOrPut(scll.group) { CalcSumData() }
            groupSum.addLiquidLevel(scll.descr, llcd.incTotal, llcd.decTotal)

            result.allSumData.addLiquidLevel(scll.descr, llcd.incTotal, llcd.decTotal)
        }

        //-----------------------------

        private fun addLiquidUsingSum(
            groupName: String,
            liquidName: String,
            using: Double,
            result: ObjectCalc,
        ) {
            val groupSum = result.tmGroupSum.getOrPut(groupName) { CalcSumData() }
            groupSum.addLiquidUsing(liquidName, using)

            result.allSumData.addLiquidUsing(liquidName, using)
        }

        private fun calcLiquidAccumulatedSensor(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scls: SensorConfigLiquidSummary,
            begTime: Int,
            endTime: Int
        ): Double {
            //--- counters that give absolute values in their readings can reset it on command or overflow.
            //--- you will have to catch each such reset (the algorithm is somewhat similar to the search for refueling / draining)
            //--- also skip sudden dots with a zero counter
            var begValue = 0.0
            var lastValue = 0.0
            var value = 0.0

            for (i in alRawTime.indices) {
                val curTime = alRawTime[i]
                if (curTime < begTime) continue
                if (curTime > endTime) break

                val sensorData = AbstractObjectStateCalc.getSensorData(scls.portNum, alRawData[i])?.toDouble() ?: continue
                if (isIgnoreSensorData(scls, sensorData)) continue

                val sensorValue = AbstractObjectStateCalc.getSensorValue(scls.alValueSensor, scls.alValueData, sensorData)

                if (begValue <= 0.0) {
                    begValue = sensorValue
                } else if (sensorValue < lastValue) {
                    value += lastValue - begValue
                    begValue = sensorValue
                }
                lastValue = sensorValue
            }
            value += lastValue - begValue

            return value
        }

        //--- not yet applied
        //    private static boolean checkZoneInTime(  ZoneLimitData zd, long pointTime  ) throws Exception {
        //        //--- if there are no restrictions, then everything is a bunch :)
        //        if(  zd.alBeg.isEmpty()  ) return true;
        //        //--- if there are restrictions on the time of validity, check their entry
        //        else {
        //            for(  int j = 0; j < zd.alBeg.size(); j++  )
        //                if(  pointTime >= zd.alBeg.get(  j  ) && pointTime <= zd.alEnd.get(  j  )  ) return true; // just enter one interval
        //            return false;
        //        }
        //    }

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

//        private fun multiplePeriods(alPD1: List<AbstractPeriodData>, alPD2: List<AbstractPeriodData>): List<MultiplePeriodData> {
//            val alResult = mutableListOf<MultiplePeriodData>()
//
//            var pos1 = 0
//            var pos2 = 0
//            while (pos1 < alPD1.size && pos2 < alPD2.size) {
//                val pd1 = alPD1[pos1]
//                val pd2 = alPD2[pos2]
//
//                //--- complete mismatch: the first is quite earlier than the second
//                //--- 1: -|===|-----
//                //--- 2: -----|===|-
//                if (pd1.endTime <= pd2.begTime) {
//                    alResult.add(MultiplePeriodData(pd1.begTime, pd1.endTime, pd1.getState(), 0))
//                    pos1++
//                } else if (pd2.endTime <= pd1.begTime) {
//                    alResult.add(MultiplePeriodData(pd2.begTime, pd2.endTime, 0, pd2.getState()))
//                    pos2++
//                } else if (pd1.begTime <= pd2.begTime) {
//                    //--- "empty time" between the beginning of the first and second periods is possible only if
//                    //--- if the second period is the initial element in its list (i.e. there is no data before it),
//                    //--- otherwise this segment should have been recorded in the previous step
//                    //--- 1: -|=======|-   -|=====|-   -|===|---
//                    //--- 2: -|?|===|---   -|?|===|-   -|?|===|-
//                    if (pd1.begTime < pd2.begTime && pos2 == 0) alResult.add(MultiplePeriodData(pd1.begTime, pd2.begTime, pd1.getState(), 0))
//                    //--- subvariants: the second ended earlier than the first - i.e. completely enters the first or ended simultaneously with the first
//                    //--- 1: -|=======|-   -|=====|-
//                    //--- 2: ---|===|---   ---|===|-
//                    //--- 1: -|=======|-   -|=====|-
//                    //--- 2: -|=====|---   -|=====|-
//                    if (pd2.endTime <= pd1.endTime) {
//                        alResult.add(MultiplePeriodData(pd2.begTime, pd2.endTime, pd1.getState(), pd2.getState()))
//                        pos2++
//                        if (pd1.endTime == pd2.endTime) {
//                            pos1++
//                        }
//                        //--- если это был последний второй, то запишем остаток/хвост первого
//                        else if (pos2 == alPD2.size) {
//                            alResult.add(MultiplePeriodData(pd2.endTime, pd1.endTime, pd1.getState(), 0))
//                            pos1++
//                        }
//                    }
//                    //--- the first ended before the second
//                    //--- 1: -|===|---
//                    //--- 2: ---|===|-
//                    //--- 1: -|===|---
//                    //--- 2: -|=====|-
//                    else if (pd1.endTime < pd2.endTime) {
//                        alResult.add(MultiplePeriodData(pd2.begTime, pd1.endTime, pd1.getState(), pd2.getState()))
//                        pos1++
//                        //--- if it was the last first, then write the remainder / tail of the second
//                        if (pos1 == alPD1.size) {
//                            alResult.add(MultiplePeriodData(pd1.endTime, pd2.endTime, 0, pd2.getState()))
//                            pos2++
//                        }
//                    }
//                }
//                //--- partial overlap: the second started earlier than the first
//                //--- 1: ---|===|---   ---|===|-   ---|===|-
//                //--- 2: -|=======|-   -|=====|-   -|===|---
//                //--- partial overlap: the first started earlier than the second or started the same way
//                //--- 1: -|=======|-   -|=====|-   -|===|---
//                //--- 2: ---|===|---   ---|===|-   ---|===|-
//                //--- 1: -|=======|-   -|=====|-   -|===|---
//                //--- 2: -|=====|---   -|=====|-   -|=====|-
//                //--- complete mismatch: the second is quite earlier than the first
//                //--- 1: -----|===|-
//                //--- 2: -|===|-----
//                else if (pd2.begTime < pd1.begTime) {
//                    // --- "empty time" between the beginning of the second and first periods is possible only
//                    // --- if the first period is the initial element in its list (i.e. there is no data before it),
//                    // --- otherwise this segment should have been written at the previous step
//                    //--- 1: -|?|===|---   -|?|===|-   -|?|===|-
//                    //--- 2: -|=======|-   -|=====|-   -|===|---
//                    if (pos1 == 0) alResult.add(MultiplePeriodData(pd2.begTime, pd1.begTime, 0, pd2.getState()))
//                    //--- subvariants: the first ended earlier than the second - i.e. completely enters the second or ended simultaneously with the second
//                    //--- 1: ---|===|---   ---|===|-
//                    //--- 2: -|=======|-   -|=====|-
//                    if (pd1.endTime <= pd2.endTime) {
//                        alResult.add(MultiplePeriodData(pd1.begTime, pd1.endTime, pd1.getState(), pd2.getState()))
//                        pos1++
//                        if (pd1.endTime == pd2.endTime) pos2++
//                        //--- if it was the last first, then write the remainder / tail of the second
//                        else if (pos1 == alPD1.size) {
//                            alResult.add(MultiplePeriodData(pd1.endTime, pd2.endTime, 0, pd2.getState()))
//                            pos2++
//                        }
//                    }
//                    //--- the second ended before the first
//                    //--- 1: ---|===|-
//                    //--- 2: -|===|---
//                    else if (pd2.endTime < pd1.endTime) {
//                        alResult.add(MultiplePeriodData(pd1.begTime, pd2.endTime, pd1.getState(), pd2.getState()))
//                        pos2++
//                        //--- if it was the last second, then write the remainder / tail of the first
//                        if (pos2 == alPD2.size) {
//                            alResult.add(MultiplePeriodData(pd2.endTime, pd1.endTime, pd1.getState(), 0))
//                            pos1++
//                        }
//                    }
//                }
//            }
//            //--- add the remaining periods of the first sequence, if any
//            while (pos1 < alPD1.size) {
//                val pd1 = alPD1[pos1]
//                alResult.add(MultiplePeriodData(pd1.begTime, pd1.endTime, pd1.getState(), 0))
//                pos1++
//            }
//            //--- add the remaining periods of the second sequence, if any
//            while (pos2 < alPD2.size) {
//                val pd2 = alPD2[pos2]
//                alResult.add(MultiplePeriodData(pd2.begTime, pd2.endTime, 0, pd2.getState()))
//                pos2++
//            }
//            return alResult
//        }

        //--- smoothing the graph of the analog value of the liquid / fuel level (abbreviated call for generating reports)
        private fun getSmoothLiquidGraphicData(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scg: SensorConfigGeo?,
            sca: SensorConfigLiquidLevel,
            begTime: Int,
            endTime: Int,
            aLine: GraphicDataContainer,
            alLSPD: MutableList<LiquidStatePeriodData>
        ) {
            val gh = LiquidGraphicHandler()
            getSmoothAnalogGraphicData(alRawTime, alRawData, scg, sca, begTime, endTime, 0, 0.0, null, null, null, aLine, gh)
            getLiquidStatePeriodData(sca, aLine, alLSPD, gh)
        }

        private fun calcLiquidUsingByLevel(
            sca: SensorConfigLiquidLevel,
            llcd: LiquidLevelCalcData,
            stm: CoreAdvancedStatement,
            oc: ObjectConfig,
            begTime: Int,
            endTime: Int
        ) {
            val aLine = llcd.aLine
            val alLSPD = llcd.alLSPD

            if (alLSPD!!.isNotEmpty()) {
                //--- first we count the usual flow
                for (i in alLSPD.indices) {
                    val lspd = alLSPD[i]
                    val begGDL = aLine!!.alGLD[lspd.begPos]
                    val endGDL = aLine.alGLD[lspd.endPos]
                    when (lspd.colorIndex) {
                        GraphicColorIndex.LINE_NORMAL_0 -> {
                            llcd.usingTotal += begGDL.y - endGDL.y
                        }
                        GraphicColorIndex.LINE_ABOVE_0 -> {
                            llcd.incTotal += endGDL.y - begGDL.y
                            if (sca.isUsingCalc) {
                                //--- looking for the previous normal period
                                val avgUsing = getPrevNormalPeriodAverageUsing(llcd, i, stm, oc, sca, begTime, endTime)
                                val calcUsing = avgUsing * (endGDL.x - begGDL.x)
                                llcd.usingCalc += calcUsing
                                llcd.usingTotal += calcUsing
                            }
                        }
                        GraphicColorIndex.LINE_BELOW_0 -> {
                            llcd.decTotal += begGDL.y - endGDL.y
                            if (sca.isUsingCalc) {
                                //--- looking for the previous normal period
                                val avgUsing = getPrevNormalPeriodAverageUsing(llcd, i, stm, oc, sca, begTime, endTime)
                                val calcUsing = avgUsing * (endGDL.x - begGDL.x)
                                llcd.usingCalc += calcUsing
                                llcd.usingTotal += calcUsing
                            }
                        }
                    }
                }
            }
        }

        //--- looking for the previous normal period to calculate the average consumption during refueling / draining
        private fun getPrevNormalPeriodAverageUsing(
            llcd: LiquidLevelCalcData,
            curPos: Int,
            stm: CoreAdvancedStatement,
            oc: ObjectConfig,
            sca: SensorConfigLiquidLevel,
            begTime: Int,
            endTime: Int
        ): Double {

            var lspdPrevNorm: LiquidStatePeriodData? = null
            var aLinePrevNorm: GraphicDataContainer? = null

            val aLine = llcd.aLine
            val alLSPD = llcd.alLSPD

            for (i in curPos - 1 downTo 0) {
                val lspdPrev = alLSPD!![i]
                val begGDLPrev = aLine!!.alGLD[lspdPrev.begPos]
                val endGDLPrev = aLine.alGLD[lspdPrev.endPos]

                //--- the found normal period is not the first (no matter how long) or the first, but with a sufficient duration for calculations
                if (lspdPrev.colorIndex == GraphicColorIndex.LINE_NORMAL_0 && (i > 0 || endGDLPrev.x - begGDLPrev.x >= MAX_CALC_PREV_NORMAL_PERIOD)) {

                    lspdPrevNorm = lspdPrev
                    aLinePrevNorm = aLine
                    break
                }
            }
            //--- no suitable normal site was found in the entire requested period - we request an extended period
            if (lspdPrevNorm == null) {
                //--- let's extend the period into the past with a two-fold margin - this will not greatly affect the processing speed
                val (alRawTimeExt, alRawDataExt) = loadAllSensorData(stm, oc, begTime - MAX_CALC_PREV_NORMAL_PERIOD * 2, endTime)

                val aLineExt = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 2)
                val alLSPDExt = mutableListOf<LiquidStatePeriodData>()
                getSmoothLiquidGraphicData(alRawTimeExt, alRawDataExt, oc.scg, sca, begTime - MAX_CALC_PREV_NORMAL_PERIOD * 2, endTime, aLineExt, alLSPDExt)

                //--- the current period in the current range
                val lspdCur = alLSPD!![curPos]
                val begGDLPCur = aLine!!.alGLD[lspdCur.begPos]
                val endGDLPCur = aLine.alGLD[lspdCur.endPos]
                //--- find the current refueling / draining period in the new extended period
                var curPosExt = 0
                while (curPosExt < alLSPDExt.size) {
                    val lspdCurExt = alLSPDExt[curPosExt]
                    val begGDLPCurExt = aLineExt.alGLD[lspdCurExt.begPos]
                    val endGDLPCurExt = aLineExt.alGLD[lspdCurExt.endPos]
                    if (begGDLPCur.x == begGDLPCurExt.x && endGDLPCur.x == endGDLPCurExt.x) break
                    curPosExt++
                }
                for (i in curPosExt - 1 downTo 0) {
                    val lspdPrevExt = alLSPDExt[i]
                    if (lspdPrevExt.colorIndex == GraphicColorIndex.LINE_NORMAL_0) {
                        lspdPrevNorm = lspdPrevExt
                        aLinePrevNorm = aLineExt
                        break
                    }
                }
            }
            //--- let's calculate the same average consumption in the previous normal period
            if (lspdPrevNorm != null) {
                var begGDLPrevNorm = aLinePrevNorm!!.alGLD[lspdPrevNorm.begPos]
                val endGDLPrevNorm = aLinePrevNorm.alGLD[lspdPrevNorm.endPos]
                //--- the normal period is too long, we take the last N hours - adjust begPos
                if (endGDLPrevNorm.x - begGDLPrevNorm.x > MAX_CALC_PREV_NORMAL_PERIOD) {
                    for (begPos in lspdPrevNorm.begPos + 1 until lspdPrevNorm.endPos) {
                        begGDLPrevNorm = aLinePrevNorm.alGLD[begPos]
                        if (endGDLPrevNorm.x - begGDLPrevNorm.x <= MAX_CALC_PREV_NORMAL_PERIOD) break
                    }
                }

                return if (endGDLPrevNorm.x == begGDLPrevNorm.x) {
                    0.0
                } else {
                    (begGDLPrevNorm.y - endGDLPrevNorm.y) / (endGDLPrevNorm.x - begGDLPrevNorm.x)
                }
            } else {
                return 0.0
            }
        }

        //--- universal function for determining the REAL sum of counter values
        private fun getSensorCountData(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            scu: SensorConfigCounter,
            begTime: Int,
            endTime: Int
        ): Double {
            //--- counters that give absolute values in their readings can reset it on command or overflow.
            //--- you will have to catch each such reset (the algorithm is somewhat similar to the search for refueling / draining)
            //--- also skip sudden dots with a zero counter
            var begValue = 0.0
            var lastValue = 0.0
            var value = 0.0

            for (pos in alRawTime.indices) {
                val rawTime = alRawTime[pos]

                if (rawTime < begTime) continue
                if (rawTime > endTime) break

                val sensorData = AbstractObjectStateCalc.getSensorData(scu.portNum, alRawData[pos])?.toDouble() ?: continue
                //--- ignore outbound values
                if (isIgnoreSensorData(scu, sensorData)) continue

                val sensorValue = AbstractObjectStateCalc.getSensorValue(scu.alValueSensor, scu.alValueData, sensorData)

                if (scu.isAbsoluteCount) {
                    if (begValue <= 0.0) {
                        begValue = sensorValue
                    } else if (sensorValue < lastValue) {
                        value += lastValue - begValue
                        begValue = sensorValue
                    }
                    lastValue = sensorValue
                } else {
                    value += sensorValue
                }
            }
            if (scu.isAbsoluteCount) {
                value += lastValue - begValue
            }
            return value
        }

//        private fun searchGDL(aLine: GraphicDataContainer, time: Int): Double {
//            //--- if the time is on / outside the search boundaries, then we take the boundary value
//            if (time <= aLine.alGLD[0].x) return aLine.alGLD[0].y
//            else if (time >= aLine.alGLD[aLine.alGLD.size - 1].x) return aLine.alGLD[aLine.alGLD.size - 1].y
//
//            var pos1 = 0
//            var pos2 = aLine.alGLD.size - 1
//            while (pos1 <= pos2) {
//                val posMid = (pos1 + pos2) / 2
//                val valueMid = aLine.alGLD[posMid].x
//
//                if (time < valueMid) {
//                    pos2 = posMid - 1
//                }
//                else if (time > valueMid) {
//                    pos1 = posMid + 1
//                }
//                else {
//                    return aLine.alGLD[posMid].y
//                }
//            }
//            //--- if nothing was found, then now pos2 is to the left of the desired value, and pos1 is to the right of it.
//            //--- In this case, we approximate the value
//            return (time - aLine.alGLD[pos2].x) / (aLine.alGLD[pos1].x - aLine.alGLD[pos2].x) * (aLine.alGLD[pos1].y - aLine.alGLD[pos2].y) + aLine.alGLD[pos2].y
//        }

        //--- filling in standard output lines (for tabular forms and reports) ---------------------------

        fun fillGeoString(userConfig: UserConfig, result: ObjectCalc) {
            val zoneId = userConfig.upZoneId
            result.gcd?.let { gcd ->
                result.sGeoName += gcd.descr
                result.sGeoRun += if (gcd.run < 0) '-' else getSplittedDouble(gcd.run, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                result.sGeoOutTime += if (gcd.outTime == 0) "-" else DateTime_DMYHMS(zoneId, gcd.outTime)
                result.sGeoInTime += if (gcd.inTime == 0) "-" else DateTime_DMYHMS(zoneId, gcd.inTime)
                result.sGeoWayTime += if (gcd.outTime == 0 || gcd.inTime == 0) '-' else secondIntervalToString(gcd.outTime, gcd.inTime)
                result.sGeoMovingTime += if (gcd.movingTime < 0) '-' else secondIntervalToString(gcd.movingTime)
                result.sGeoParkingTime += if (gcd.parkingTime < 0) '-' else secondIntervalToString(gcd.parkingTime)
                result.sGeoParkingCount += if (gcd.parkingCount < 0) {
                    '-'
                } else if (userConfig.upIsUseThousandsDivider) {
                    getSplittedLong(gcd.parkingCount.toLong())
                } else {
                    gcd.parkingCount.toString()
                }
            }
        }

//        fun fillZoneString(hmZoneData: Map<Int, ZoneData>, p: XyPoint): StringBuilder {
//            val tsZoneName = TreeSet<String>()
//            for (zd in hmZoneData.values)
//                if (zd.polygon!!.isContains(p))
//                    tsZoneName.add(zd.name)
//            return getSBFromIterable(tsZoneName, ", ")
//        }

        fun fillWorkString(userConfig: UserConfig, result: ObjectCalc) {
            val workPair = fillWorkString(userConfig, result.tmWork)
            result.sWorkName = workPair.first
            result.sWorkValue = workPair.second
        }

        fun fillWorkString(userConfig: UserConfig, tmWork: SortedMap<String, WorkCalcData>): Pair<String, String> {
            var sWorkName = ""
            var sWorkTotal = ""
            tmWork.forEach { (workName, wcd) ->
                if (sWorkName.isNotEmpty()) {
                    sWorkName += '\n'
                    sWorkTotal += '\n'
                }
                sWorkName += workName
                sWorkTotal += getSplittedDouble(wcd.onTime.toDouble() / 60.0 / 60.0, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
            }
            return Pair(sWorkName, sWorkTotal)
        }

        fun fillEnergoString(userConfig: UserConfig, result: ObjectCalc) {
            result.tmEnergo.forEach { (descr, e) ->
                if (result.sEnergoName.isNotEmpty()) {
                    result.sEnergoName += '\n'
                    result.sEnergoValue += '\n'
                }
                result.sEnergoName += descr
                result.sEnergoValue += getSplittedDouble(e, getPrecision(e), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
            }

            result.allSumData.tmEnergo.forEach { (sensorType, dataByPhase) ->
                dataByPhase.forEach { (phase, value) ->
                    if (result.sAllSumEnergoName.isNotEmpty()) {
                        result.sAllSumEnergoName += '\n'
                        result.sAllSumEnergoValue += '\n'
                    }
                    result.sAllSumEnergoName += (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + getPhaseDescr(phase)
                    result.sEnergoValue += getSplittedDouble(value, getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                }
            }

        }

        fun fillLiquidUsingString(userConfig: UserConfig, result: ObjectCalc) {
            val liquidPair = fillLiquidUsingString(userConfig, result.tmLiquidUsing)
            result.sLiquidUsingName = liquidPair.first
            result.sLiquidUsingValue = liquidPair.second

            val allSumLiquidPair = fillLiquidUsingString(userConfig, result.allSumData.tmLiquidUsing)
            result.sAllSumLiquidName = allSumLiquidPair.first
            result.sAllSumLiquidValue = allSumLiquidPair.second
        }

        fun fillLiquidUsingString(userConfig: UserConfig, tmLiquidUsing: SortedMap<String, Double>): Pair<String, String> {
            var sLiquidUsingName = ""
            var sLiquidUsing = ""
            tmLiquidUsing.forEach { (name, total) ->
                if (sLiquidUsingName.isNotEmpty()) {
                    sLiquidUsingName += '\n'
                    sLiquidUsing += '\n'
                }
                sLiquidUsingName += name
                sLiquidUsing += getSplittedDouble(total, getPrecision(total), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
            }
            return Pair(sLiquidUsingName, sLiquidUsing)
        }

        private fun fillLiquidLevelString(userConfig: UserConfig, result: ObjectCalc) {
            val isUsingCalc = result.tmLiquidLevel.values.any { it.usingCalc > 0.0 }

            result.tmLiquidLevel.forEach { (liquidName, llcd) ->
                if (result.sLiquidLevelName.isNotEmpty()) {
                    result.sLiquidLevelName += '\n'
                    result.sLiquidLevelBeg += '\n'
                    result.sLiquidLevelEnd += '\n'
                    result.sLiquidLevelIncTotal += '\n'
                    result.sLiquidLevelDecTotal += '\n'
                    result.sLiquidLevelUsingTotal += '\n'
                    if (isUsingCalc) {
                        result.sLiquidLevelUsingCalc += '\n'
                    }
                }
                result.sLiquidLevelName += liquidName
                result.sLiquidLevelBeg += getSplittedDouble(llcd.begLevel, getPrecision(llcd.begLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                result.sLiquidLevelEnd += getSplittedDouble(llcd.endLevel, getPrecision(llcd.endLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                result.sLiquidLevelIncTotal += getSplittedDouble(llcd.incTotal, getPrecision(llcd.incTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                result.sLiquidLevelDecTotal += getSplittedDouble(llcd.decTotal, getPrecision(llcd.decTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                result.sLiquidLevelUsingTotal += getSplittedDouble(llcd.usingTotal, getPrecision(llcd.usingTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                if (isUsingCalc) {
                    result.sLiquidLevelUsingCalc +=
                        if (llcd.usingCalc <= 0.0) {
                            "-"
                        } else {
                            getSplittedDouble(llcd.usingCalc, getPrecision(llcd.usingCalc), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                        }
                }
            }

            result.allSumData.tmLiquidIncDec.forEach { (liquidName, pairIncDec) ->
                if (result.sLiquidLevelLiquidName.isNotEmpty()) {
                    result.sLiquidLevelLiquidName += '\n'
                    result.sLiquidLevelLiquidInc += '\n'
                    result.sLiquidLevelLiquidDec += '\n'
                }
                result.sLiquidLevelLiquidName += liquidName
                result.sLiquidLevelLiquidInc += getSplittedDouble(pairIncDec.first, getPrecision(pairIncDec.first), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                result.sLiquidLevelLiquidDec += getSplittedDouble(pairIncDec.second, getPrecision(pairIncDec.second), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
            }
        }

        fun getPhaseDescr(phase: Int) =
            when (phase) {
                0 -> " (сумма фаз)"
                1 -> " (фаза A)"
                2 -> " (фаза B)"
                3 -> " (фаза C)"
                else -> " (неизв. фаза)"
            }

        fun getPrecision(aValue: Double): Int {
            val value = abs(aValue)
            //--- updated / simplified version of the output accuracy - more cubic meters - in whole liters, less - in hundreds of milliliters / gram
            return if (value >= 1000) 0
            else if (value >= 100) 1
            else if (value >= 10) 2
            else 3
        }

        fun fillZoneList(hmZoneData: Map<Int, ZoneData>, reportZone: Int, p: XyPoint, tsZoneName: TreeSet<String>): Boolean {
            var inZone = false
            for ((zoneID, zd) in hmZoneData) {
                if (zd.polygon!!.isContains(p)) {
                    var sZoneInfo = zd.name
                    if (zd.descr.isNotEmpty()) {
                        sZoneInfo += " (${zd.descr})"
                    }

                    tsZoneName.add(sZoneInfo)
                    if (reportZone != 0 && reportZone == zoneID) inZone = true
                }
            }
            return inZone
        }
    }
}
