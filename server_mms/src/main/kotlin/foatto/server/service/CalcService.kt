package foatto.server.service

import foatto.server.calc.WorkCalcData
import foatto.server.calc.WorkPeriodData
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.model.SensorConfig
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Service
class CalcService(
    private val entityManager: EntityManager,
    private val sensorRepository: SensorRepository,
) {

//---------------------------------------------------------------------------------------------------------------------

    fun calcWork(
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        byGroupLiquidSum: SortedMap<String, SortedMap<String, Double>>,
        allLiquidSum: SortedMap<String, Double>,
    ): SortedMap<String, WorkCalcData> {
        val tmWork = sortedMapOf<String, WorkCalcData>()

        sensorRepository.findByObjAndSensorType(objectEntity, SensorConfig.SENSOR_WORK).forEach { sensorEntity ->
            val wcd = calcWorkSensor(sensorEntity, begTime, endTime)
            tmWork[sensorEntity.descr ?: ""] = wcd

            addLiquidUsingSum(
                groupName = wcd.group,
                liquidName = wcd.liquidName,
                using = wcd.liquidCalc,
                byGroupLiquidSum = byGroupLiquidSum,
                allLiquidSum = allLiquidSum,
            )
        }

        return tmWork
    }

    fun calcWorkSensor(
        sensorEntity: SensorEntity,
        begTime: Int,
        endTime: Int,
    ): WorkCalcData {
        val states = mutableListOf<WorkPeriodData>()

        ApplicationService.withConnection(entityManager) { conn ->
            SensorService.checkAndCreateAggTable(conn, sensorEntity.id)

            val rs = conn.executeQuery(
                """
                    SELECT ontime_0 , ontime_1 , type_0
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 < $endTime
                      AND ontime_1 > $begTime
                    ORDER BY ontime_0
                """
            )
            while (rs.next()) {
                val onTime0 = rs.getInt(1)
                val onTime1 = rs.getInt(2)
                val state = rs.getInt(3) != 0

                states += WorkPeriodData(
                    begTime = max(begTime, onTime0),
                    endTime = min(endTime, onTime1),
                    state = state,
                )
            }
            rs.close()
        }

        val onTime = states
            .filter { wpd -> wpd.state }
            .sumOf { wpd ->
                wpd.endTime - wpd.begTime
            }

        val liquidCalc = if (!sensorEntity.liquidName.isNullOrBlank()) {
            (sensorEntity.liquidNorm ?: 0.0) * onTime / 60.0 / 60.0
        } else {
            null
        }

        return WorkCalcData(
            group = sensorEntity.group ?: "",
            states = states,
            onTime = onTime,
            liquidName = sensorEntity.liquidName,
            liquidCalc = liquidCalc,
        )
    }

//---------------------------------------------------------------------------------------------------------------------

    fun calcEnergo(
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
    ): SortedMap<String, Double> {
        val tmEnergo = sortedMapOf<String, Double>()

        listOf(
            SensorConfig.SENSOR_ENERGO_COUNT_AD,
            SensorConfig.SENSOR_ENERGO_COUNT_AR,
            SensorConfig.SENSOR_ENERGO_COUNT_RD,
            SensorConfig.SENSOR_ENERGO_COUNT_RR,
        ).forEach { sensorType ->
            sensorRepository.findByObjAndSensorType(objectEntity, sensorType).forEach { sensorEntity ->
                val result = calcCounterSensor(sensorEntity, begTime, endTime)
                tmEnergo[sensorEntity.descr ?: ""] = result

//!!!
//            //--- calculate the group amount
//            val groupSum = result.tmGroupSum.getOrPut(sces.group) { CalcSumData() }
//            val byType = groupSum.tmEnergo.getOrPut(sces.sensorType) { sortedMapOf() }
//            val byPhase = byType[0] ?: 0.0
//            byType[0] = byPhase + e
//
//            val byTypeAll = result.allSumData.tmEnergo.getOrPut(sces.sensorType) { sortedMapOf() }
//            val byPhaseAll = byTypeAll[0] ?: 0.0
//            byTypeAll[0] = byPhaseAll + e
            }
        }

        return tmEnergo
    }

//---------------------------------------------------------------------------------------------------------------------

    fun calcLiquidUsing(
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        byGroupLiquidSum: SortedMap<String, SortedMap<String, Double>>,
        allLiquidSum: SortedMap<String, Double>,
    ) {
        listOf(
            SensorConfig.SENSOR_MASS_ACCUMULATED,
            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
            SensorConfig.SENSOR_LIQUID_USING,
        ).forEach { sensorType ->
            sensorRepository.findByObjAndSensorType(objectEntity, sensorType).forEach { sensorEntity ->
                if (!sensorEntity.liquidName.isNullOrBlank()) {
                    val result = calcCounterSensor(sensorEntity, begTime, endTime)

                    addLiquidUsingSum(
                        groupName = sensorEntity.group,
                        liquidName = sensorEntity.liquidName,
                        using = result,
                        byGroupLiquidSum = byGroupLiquidSum,
                        allLiquidSum = allLiquidSum,
                    )
                }
            }
        }
    }

//---------------------------------------------------------------------------------------------------------------------

    fun calcLiquidLevel(
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        byGroupLiquidSum: SortedMap<String, SortedMap<String, Double>>,
        allLiquidSum: SortedMap<String, Double>,
    ) {
    }
//    val tmLiquidLevel = sortedMapOf<String, LiquidLevelCalcData>()

//            //--- liquid level sensors
//            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
//                calcLiquidLevel(alRawTime, alRawData, conn, oc, sc as SensorConfigLiquidLevel, begTime, endTime, 0, result)
//            }
//

//        private fun calcLiquidLevel(
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            conn: CoreAdvancedConnection,
//            oc: ObjectConfig,
//            scll: SensorConfigLiquidLevel,
//            begTime: Int,
//            endTime: Int,
//            axisIndex: Int,
//            result: ObjectCalc
//        ) {
//            val llcd = calcLiquidLevelSensor(conn, oc, scll, alRawTime, alRawData, begTime, endTime, axisIndex)
//
//            result.tmLiquidLevel[scll.descr] = llcd
//
//            result.tmLiquidUsing["${scll.descr} ${scll.liquidName}"] = llcd.usingTotal
//            addLiquidUsingSum(scll.group, scll.liquidName, llcd.usingTotal, result)
//
//            val groupSum = result.tmGroupSum.getOrPut(scll.group) { CalcSumData() }
//            groupSum.addLiquidLevel(scll.descr, llcd.incTotal, llcd.decTotal)
//
//            result.allSumData.addLiquidLevel(scll.descr, llcd.incTotal, llcd.decTotal)
//        }
//

//        fun calcLiquidLevelSensor(
//            conn: CoreAdvancedConnection,
//            oc: ObjectConfig,
//            sca: SensorConfigLiquidLevel,
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            begTime: Int,
//            endTime: Int,
//            axisIndex: Int,
//        ): LiquidLevelCalcData {
//
//            val aLine = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 2, false)
//            val alLSPD = mutableListOf<LiquidStatePeriodData>()
//            getSmoothLiquidGraphicData(conn, alRawTime, alRawData, oc.scg, sca, begTime, endTime, axisIndex, aLine, alLSPD)
//
//            val llcd = LiquidLevelCalcData(sca.containerType, aLine, alLSPD)
//            calcLiquidUsingByLevel(sca, llcd, conn, oc, begTime, endTime, axisIndex)
//
//            return llcd
//        }
//

//---------------------------------------------------------------------------------------------------------------------

    fun calcAnalogueSensorValue(
        objectEntity: ObjectEntity,
        begTime: Int,
        endTime: Int,
        sensorType: Int,
    ): SortedMap<String, Pair<Double?, Double?>> {
        val result = sortedMapOf<String, Pair<Double?, Double?>>()

        ApplicationService.withConnection(entityManager) { conn ->
            sensorRepository.findByObjAndSensorType(objectEntity, sensorType).forEach { sensorEntity ->
                SensorService.checkAndCreateAggTable(conn, sensorEntity.id)

                var rs = conn.executeQuery(
                    """
                        SELECT value_1
                        FROM MMS_agg_${sensorEntity.id}
                        WHERE ontime_0 = ( SELECT MIN(ontime_0) FROM MMS_agg_${sensorEntity.id} WHERE ontime_0 BETWEEN $begTime AND $endTime )
                    """
                )
                val begValue = if (rs.next()) {
                    rs.getDouble(1)
                } else {
                    null
                }
                rs.close()

                rs = conn.executeQuery(
                    """
                        SELECT value_1
                        FROM MMS_agg_${sensorEntity.id}
                        WHERE ontime_0 = ( SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} WHERE ontime_0 BETWEEN $begTime AND $endTime )
                    """
                )
                val endValue = if (rs.next()) {
                    rs.getDouble(1)
                } else {
                    null
                }
                rs.close()

                result[sensorEntity.descr] = begValue to endValue
            }
        }

        return result
    }
//            //--- some analogue sensors
//            oc.hmSensorConfig[]?.values?.forEach { sc ->
//                val sca = sc as SensorConfigAnalogue
//                val aLine = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 2, false)
//                getSmoothAnalogGraphicData(
//                    conn = conn,
//                    alRawTime = alRawTime,
//                    alRawData = alRawData,
//                    scg = oc.scg,
//                    sca = sca,
//                    begTime = begTime,
//                    endTime = endTime,
//                    xScale = 0,
//                    yScale = 0.0,
//                    axisIndex = 0,
//                    aMinLimit = null,
//                    aMaxLimit = null,
//                    aLine = aLine,
//                    graphicHandler = AnalogGraphicHandler()
//                )
//                result.tmTemperature[sc.descr] = aLine
//            }
//
//            oc.hmSensorConfig[]?.values?.forEach { sc ->
//                val sca = sc as SensorConfigAnalogue
//                val aLine = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 2, false)
//                getSmoothAnalogGraphicData(
//                    conn = conn,
//                    alRawTime = alRawTime,
//                    alRawData = alRawData,
//                    scg = oc.scg,
//                    sca = sca,
//                    begTime = begTime,
//                    endTime = endTime,
//                    xScale = 0,
//                    yScale = 0.0,
//                    axisIndex = 0,
//                    aMinLimit = null,
//                    aMaxLimit = null,
//                    aLine = aLine,
//                    graphicHandler = AnalogGraphicHandler()
//                )
//                result.tmDensity[sc.descr] = aLine
//                //--- прикрепить данные по плотности с данным по расходомеру
//                result.tmCounterData.forEach { (_, counterCalcData) ->
//                    if (counterCalcData.scc.group == sc.group) {
//                        counterCalcData.density = aLine
//                    }
//                }
//            }

//---------------------------------------------------------------------------------------------------------------------

    private fun calcCounterSensor(
        sensorEntity: SensorEntity,
        begTime: Int,
        endTime: Int,
    ): Double {
        var result = 0.0
        ApplicationService.withConnection(entityManager) { conn ->
            SensorService.checkAndCreateAggTable(conn, sensorEntity.id)

            val rs = conn.executeQuery(
                """
                    SELECT SUM( value_1 )
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 BETWEEN $begTime AND $endTime
                """
            )
            if (rs.next()) {
                result = rs.getDouble(1)
            }
            rs.close()
        }

        return result
    }

    private fun addLiquidUsingSum(
        groupName: String?,
        liquidName: String?,
        using: Double?,
        byGroupLiquidSum: SortedMap<String, SortedMap<String, Double>>,
        allLiquidSum: SortedMap<String, Double>,
    ) {
        liquidName?.let {
            using?.let {
                val groupSum = byGroupLiquidSum.getOrPut(groupName ?: "") { sortedMapOf() }
                val prevGroupValue = groupSum[liquidName] ?: 0.0
                groupSum[liquidName] = prevGroupValue + using

                val prevAllValue = allLiquidSum[liquidName] ?: 0.0
                allLiquidSum[liquidName] = prevAllValue + using
            }
        }
    }

}

//    var gcd: GeoCalcData? = null
//    val tmCounterData = sortedMapOf<String, CounterCalcData>()
//
//    val tmGroupSum = sortedMapOf<String, CalcSumData>() // sums by group
//    val allSumData = CalcSumData()                  // overall sum
//
//    var sGeoName: String = ""
//    var sGeoRun: String = ""
//    var sGeoOutTime: String = ""
//    var sGeoInTime: String = ""
//    var sGeoWayTime: String = ""
//    var sGeoMovingTime: String = ""
//    var sGeoParkingTime: String = ""
//    var sGeoParkingCount: String = ""
//
//    var sLiquidUsingName: String = ""
//    var sLiquidUsingValue: String = ""
//
//    var sAllSumLiquidName: String = ""
//    var sAllSumLiquidValue: String = ""
//
//    var sLiquidLevelName: String = ""
//    var sLiquidLevelBeg: String = ""
//    var sLiquidLevelEnd: String = ""
//    var sLiquidLevelIncTotal: String = ""
//    var sLiquidLevelDecTotal: String = ""
//    var sLiquidLevelUsingTotal: String = ""
//    var sLiquidLevelUsingCalc: String = ""
//
//    var sLiquidLevelLiquidName: String = ""
//    var sLiquidLevelLiquidInc: String = ""
//    var sLiquidLevelLiquidDec: String = ""
//
//    companion object {
//
//        //--- maximum allowable distance between points
//        private const val MAX_RUN = 100_000 // 100 km = 30 minutes (see MAX_WORK_TIME_INTERVAL) at 200 km/h
//
//        //--- the maximum allowable time interval between points, over which:
//        //--- 1.for geo-sensors - the mileage is not counted for this period
//        //--- 2. for sensors of equipment operation - this period is considered inoperative, regardless of the current state of the point
//        //--- 3. for fuel level sensors - this period is considered inoperative (not consumption, not refueling, not draining) and the level change is not included in any amount
//        const val MAX_WORK_TIME_INTERVAL: Int = 30 * 60
//
//        //--- maximum duration of the previous "normal" period,
//        //--- used to calculate the average fuel consumption during refueling / draining
//        private const val MAX_CALC_PREV_NORMAL_PERIOD = 3 * 60 * 60
//
//        fun calcObject(conn: CoreAdvancedConnection, userConfig: UserConfig, oc: ObjectConfig, begTime: Int, endTime: Int): ObjectCalc {
//
//            val result = ObjectCalc(oc)
//
//            val (alRawTime, alRawData) = loadAllSensorData(conn, oc, begTime, endTime)
//
//            //--- if geo-sensors are registered - we sum up the mileage
//            oc.scg?.let { scg ->
//                calcGeo(alRawTime, alRawData, scg, begTime, endTime, result)
//            }
//
//            return result
//        }
//
//        fun calcGeoSensor(
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            scg: SensorConfigGeo,
//            begTime: Int,
//            endTime: Int,
//            scale: Int,
//            maxEnabledOverSpeed: Int,
//            alZoneSpeedLimit: List<ZoneLimitData>?
//        ): GeoCalcData {
//
//            var begRun = 0
//            var lastRun = 0
//            var run = 0
//
//            var movingBeginTime = 0
//            var parkingBeginTime = 0
//            var parkingCoord: XyPoint? = null
//
//            val alMovingAndParking = mutableListOf<AbstractPeriodData>()
//            val alOverSpeed = mutableListOf<AbstractPeriodData>()
//            val alPointTime = mutableListOf<Int>()
//            val alPointXY = mutableListOf<XyPoint>()
//            val alPointSpeed = mutableListOf<Int>()
//            val alPointOverSpeed = mutableListOf<Int>()
//
//            var normalSpeedBeginTime = 0
//            var overSpeedBeginTime = 0
//            var maxOverSpeedTime = 0
//            var maxOverSpeedCoord: XyPoint? = null
//            var maxOverSpeedMax = 0
//            var maxOverSpeedDiff = 0
//
//            val lastPoint = XyPoint(0, 0)
//
//            var lastTime = 0
//            var curTime = 0
//            for (i in alRawTime.indices) {
//                curTime = alRawTime[i]
//                if (curTime < begTime) {
//                    continue
//                }
//                if (curTime > endTime) {
//                    if (i > 0) {
//                        curTime = alRawTime[i - 1]
//                    }
//                    break
//                }
//
//                val gd = AbstractObjectStateCalc.getGeoData(scg, alRawData[i]) ?: continue
//                // --- Nuances of calculating the mileage:
//                // --- 1. Regardless of the mileage display method (relative / point-to-point or absolute):
//                // --- 1.1. We ignore the runs between points with too long a time interval.
//                // --- 1.2. Ignore points with too high / unrealistic distance between points.
//                // --- 2. Devices that give absolute mileage in their readings manage to reset it in the middle of the day,
//                // --- you have to catch each such reset (the algorithm is somewhat similar to the search for refueling / draining),
//                // --- but skipping sudden points with zero mileage at absolutely normal coordinates.
//                if (gd.distance > 0) {
//                    if (scg.isAbsoluteRun) {
//                        val curRun = gd.distance
//
//                        if (begRun == 0) {
//                            begRun = curRun
//                        } else if (curRun < lastRun || curRun - lastRun > MAX_RUN || lastTime != 0 && curTime - lastTime > MAX_WORK_TIME_INTERVAL) {
//                            run += lastRun - begRun
//                            begRun = curRun
//                        }
//                        lastRun = curRun
//                    } else if (gd.distance < MAX_RUN) {
//                        run += gd.distance
//                    }
//                }
//
//                val pixPoint = XyProjection.wgs_pix(gd.wgs)
//
//                var overSpeed = 0
//
//                //--- on parking ?
//                if (gd.speed <= AbstractObjectStateCalc.MAX_SPEED_AS_PARKING) {
//                    //--- recording previous movement
//                    if (movingBeginTime != 0) {
//                        alMovingAndParking.add(GeoPeriodData(movingBeginTime, curTime, 1))
//                        movingBeginTime = 0
//                    }
//                    //--- parking start (parkingBeginTime - parking flag)
//                    if (parkingBeginTime == 0) {
//                        parkingBeginTime = curTime
//                        parkingCoord = pixPoint
//                    }
//                    //--- parking = end of excess and, as it were, the beginning of normal speed
//                    if (overSpeedBeginTime != 0) {
//                        alOverSpeed.add(OverSpeedPeriodData(overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff))
//                        overSpeedBeginTime = 0
//                        maxOverSpeedTime = 0
//                        maxOverSpeedCoord = null
//                        maxOverSpeedMax = 0
//                        maxOverSpeedDiff = 0
//                    }
//                    if (normalSpeedBeginTime == 0) {
//                        normalSpeedBeginTime = curTime
//                    }
//                } else {
//                    //--- previous parking record
//                    if (parkingBeginTime != 0) {
//                        alMovingAndParking.add(GeoPeriodData(parkingBeginTime, curTime, parkingCoord!!))
//                        parkingBeginTime = 0
//                        parkingCoord = null
//                    }
//                    //--- start of movement
//                    if (movingBeginTime == 0) {
//                        movingBeginTime = curTime
//                    }
//
//                    //--- overspeed handling
//                    overSpeed = calcOverSpeed(scg.maxSpeedLimit, alZoneSpeedLimit, pixPoint, gd.speed)
//                    if (overSpeed > maxEnabledOverSpeed) {
//                        //--- we will record the previous normal movement
//                        if (normalSpeedBeginTime != 0) {
//                            alOverSpeed.add(OverSpeedPeriodData(normalSpeedBeginTime, curTime))
//                            normalSpeedBeginTime = 0
//                        }
//                        //--- overspeed start mark
//                        if (overSpeedBeginTime == 0) {
//                            overSpeedBeginTime = curTime
//                        }
//                        //--- saving the value / coordinates / time of maximum speeding on the site
//                        if (overSpeed > maxOverSpeedDiff) {
//                            maxOverSpeedTime = curTime
//                            maxOverSpeedCoord = pixPoint
//                            maxOverSpeedMax = gd.speed
//                            maxOverSpeedDiff = overSpeed
//                        }
//                    } else {
//                        if (overSpeedBeginTime != 0) {
//                            alOverSpeed.add(OverSpeedPeriodData(overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff))
//                            overSpeedBeginTime = 0
//                            maxOverSpeedTime = 0
//                            maxOverSpeedCoord = null
//                            maxOverSpeedMax = 0
//                            maxOverSpeedDiff = 0
//                        }
//                        if (normalSpeedBeginTime == 0) {
//                            normalSpeedBeginTime = curTime
//                        }
//                    }
//                }
//                //--- only points with movement are recorded for the trajectory
//                if (gd.speed > AbstractObjectStateCalc.MAX_SPEED_AS_PARKING && pixPoint.distance(lastPoint) > scale) {
//                    alPointTime.add(curTime)
//                    alPointXY.add(pixPoint)
//                    alPointSpeed.add(gd.speed)
//                    alPointOverSpeed.add(if (overSpeed < 0) 0 else overSpeed)
//
//                    lastPoint.set(pixPoint)
//                }
//
//                lastTime = curTime
//            }
//            //--- summarize the sub-run of the last (i.e. unfinished) range
//            if (scg.isAbsoluteRun) run += lastRun - begRun
//            //--- record of the last unclosed event
//            if (movingBeginTime != 0) alMovingAndParking.add(GeoPeriodData(movingBeginTime, curTime, 1))
//            if (parkingBeginTime != 0) alMovingAndParking.add(GeoPeriodData(parkingBeginTime, curTime, parkingCoord!!))
//            if (normalSpeedBeginTime != 0) alOverSpeed.add(OverSpeedPeriodData(normalSpeedBeginTime, curTime))
//            if (overSpeedBeginTime != 0) alOverSpeed.add(OverSpeedPeriodData(overSpeedBeginTime, curTime, maxOverSpeedTime, maxOverSpeedCoord!!, maxOverSpeedMax, maxOverSpeedDiff))
//
//            mergePeriods(alMovingAndParking, scg.minMovingTime, scg.minParkingTime)
//            mergePeriods(alOverSpeed, scg.minOverSpeedTime, max(10, scg.minOverSpeedTime / 10))
//
//            //--- calculation of other indicators: time of departure / arrival, in motion / in the parking lot, number of parking lots
//            var outTime = 0
//            var inTime = 0
//            var movingTime = 0
//            var parkingCount = 0
//            var parkingTime = 0
//            for (i in alMovingAndParking.indices) {
//                val pd = alMovingAndParking[i] as GeoPeriodData
//
//                if (pd.getState() != 0) {
//                    if (outTime == 0) outTime = pd.begTime
//                    inTime = pd.endTime
//                    movingTime += pd.endTime - pd.begTime
//                } else {
//                    parkingCount++
//                    parkingTime += pd.endTime - pd.begTime
//                }
//            }
//
//            //--- we convert the sums of meters into km (if the mileage from this sensor is not used, then we will make it negative)
//            return GeoCalcData(
//                group = scg.group,
//                descr = scg.descr,
//                run = run / 1000.0 * scg.runKoef,
//                outTime = outTime,
//                inTime = inTime,
//                movingTime = movingTime,
//                parkingCount = parkingCount,
//                parkingTime = parkingTime,
//                alMovingAndParking = alMovingAndParking,
//                alOverSpeed = alOverSpeed,
//                alPointTime = alPointTime,
//                alPointXY = alPointXY,
//                alPointSpeed = alPointSpeed,
//                alPointOverSpeed = alPointOverSpeed
//            )
//        }
//
//        //--- another public part -----------------------------------------------------------------------------------------------------------
//
//        //--- collect the value of the maximum excess for a given point (taking into account the zones with a speed limit)
//        fun calcOverSpeed(maxSpeedConst: Int, alZoneSpeedLimit: List<ZoneLimitData>?, prjPoint: XyPoint, speed: Int): Int {
//            //--- collecting the maximum excess
//            var maxOverSpeed = -Integer.MAX_VALUE
//
//            //--- looking for an excess among the permanent limit
//            maxOverSpeed = max(maxOverSpeed, speed - maxSpeedConst)
//            //--- looking for speed limit zones
//            alZoneSpeedLimit?.let {
//                for (zd in alZoneSpeedLimit) {
//                    //--- if there are restrictions on the duration of the action, then we will check their entry,
//                    //--- and if we do not enter the interval (s) of the zone, then we go to the next zone
//                    //if(  ! checkZoneInTime(  zd, pointTime  )  ) continue; - пока не применяется
//                    //--- check the geometric entry into the zone
//                    if (!zd.zoneData!!.polygon!!.isContains(prjPoint)) {
//                        continue
//                    }
//                    //--- check for excess
//                    maxOverSpeed = max(maxOverSpeed, speed - zd.maxSpeed)
//                }
//            }
//            return maxOverSpeed
//        }
//
//        //--- smoothing analog value graph
//        fun getSmoothAnalogGraphicData(
//            conn: CoreAdvancedConnection,
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            scg: SensorConfigGeo?,
//            sca: SensorConfigAnalogue,
//            begTime: Int,
//            endTime: Int,
//            xScale: Int,
//            yScale: Double,
//            axisIndex: Int,
//            aMinLimit: ChartElementDTO?,
//            aMaxLimit: ChartElementDTO?,
//            aLine: ChartElementDTO,
//            graphicHandler: iGraphicHandler
//        ) {
//            val alGLD = aLine.alGLD.toMutableList()
//
////            if (sca.isUseAggregator &&
////                MMSFunction.checkAggregationTableIsExists(conn, sca.id) &&
////                (endTime <= sca.calcTime || (sca.rawTime - sca.calcTime) <= MAX_AGGREGATOR_TIME_DIFF)
////            ) {
////                val rs = conn.executeQuery(
////                    """
////                        SELECT ontime, value_2 , type_1
////                        FROM MMS_agg_${sca.id}
////                        WHERE ontime BETWEEN $begTime AND $endTime
////                        ORDER BY ontime
////                    """
////                )
////                while (rs.next()) {
////                    val time = rs.getInt(1)
////                    val value = rs.getDouble(2)
////                    val type = rs.getInt(3)
////
////                    val curColorIndex = when (type) {
////                        AnalogueValueType.NONE.value -> {
////                            chartLineNoneColorIndexes[axisIndex]
////                        }
////
////                        AnalogueValueType.NORMAL.value -> {
////                            chartLineNormalColorIndexes[axisIndex]
////                        }
////
////                        AnalogueValueType.BELOW.value -> {
////                            chartLineBelowColorIndexes[axisIndex]
////                        }
////
////                        AnalogueValueType.ABOVE.value -> {
////                            chartLineAboveColorIndexes[axisIndex]
////                        }
////
////                        else -> {
////                            chartLineNoneColorIndexes[axisIndex]
////                        }
////                    }
////
////                    val gldLast = alGLD.lastOrNull()
////                    if (gldLast == null || time - gldLast.x > xScale || abs(value - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {
//////!!! add GEO info ?
//////                        val gd = scg?.let {
//////                            AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
//////                        }
////                        alGLD.add(ChartElementLineDTO(time, value.toFloat(), curColorIndex, null /*gd?.let { XyProjection.wgs_pix(gd.wgs) }*/))
////                    }
////                }
////                rs.close()
////            } else {
//            //--- immediately add / set static (permanent) constraints if required / supported
//            if (graphicHandler.isStaticMinLimit(sca)) {
//                graphicHandler.setStaticMinLimit(sca, begTime, endTime, aMinLimit)
//            }
//            if (graphicHandler.isStaticMaxLimit(sca)) {
//                graphicHandler.setStaticMaxLimit(sca, begTime, endTime, aMaxLimit)
//            }
//
//            //--- for smoothing, you may need data before and after the time of the current point,
//            //--- therefore, we overload / translate data in advance
//            val alSensorData = mutableListOf<Double?>()
//            for (bb in alRawData) {
//                alSensorData.add(graphicHandler.getRawData(sca, bb))
//            }
//
//            //--- raw data processing -----------------------------------------------------------------------------------------
//
//            for (pos in alRawTime.indices) {
//                val rawTime = alRawTime[pos]
//                //--- immediately skip outrageous points loaded for seamless smoothing between adjacent ranges
//                if (rawTime < begTime) {
//                    continue
//                }
//                if (rawTime > endTime) {
//                    break
//                }
//
//                //--- insert the first and last pseudo-points for seamless connection between periods
//
//                //--- sensor value mb. == null, for example, ignored below / above the specified value boundaries (noise filtering)
//                val rawData = alSensorData[pos] ?: continue
//
//                //--- if lines are shown
//                aLine.let {
//                    //--- finding the left border of the smoothing range
//                    var pos1 = pos - 1
//                    while (pos1 >= 0) {
//                        if (rawTime - alRawTime[pos1] > sca.smoothTime) {
//                            break
//                        }
//                        pos1--
//                    }
//                    //--- finding the right border of the smoothing range
//                    var pos2 = pos + 1
//                    while (pos2 < alRawTime.size) {
//                        if (alRawTime[pos2] - rawTime > sca.smoothTime) {
//                            break
//                        }
//                        pos2++
//                    }
//
//                    //--- smoothing
//                    val avgValue: Double
//                    val alSubList = mutableListOf<Double>()
//
//                    for (p in pos1 + 1 until pos2) {
//                        val v = alSensorData[p] ?: continue
//                        alSubList.add(v)
//                    }
//                    alSubList.sort()
//                    //--- if the number of values is odd, take exactly the middle
//                    avgValue = if (alSubList.size % 2 != 0) {
//                        alSubList[alSubList.size / 2]
//                    }
//                    //--- otherwise the arithmetic mean between two values closest to the middle
//                    else {
//                        val val1 = alSubList[alSubList.size / 2 - 1]
//                        val val2 = alSubList[alSubList.size / 2]
//                        val1 + (val2 - val1) / 2
//                    }
//
//                    val gldLast = alGLD.lastOrNull()
//
//                    //--- if boundary values are set, we look at the averaged avgValue,
//                    //--- so the typical getDynamicXXX from the beginning of the cycle does not suit us
//                    val prevTime = (gldLast?.x ?: rawTime)
//                    val prevData = (gldLast?.y ?: avgValue).toDouble()
//                    val curColorIndex = graphicHandler.getLineColorIndex(axisIndex, sca, rawTime, avgValue, prevTime, prevData)
//
//                    if (gldLast == null || rawTime - gldLast.x > xScale || abs(rawData - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {
//                        val gd = scg?.let {
//                            AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
//                        }
//                        alGLD.add(ChartElementLineDTO(rawTime, avgValue.toFloat(), curColorIndex, gd?.let { XyProjection.wgs_pix(gd.wgs) }))
//                    }
//                }
//            }
////            }
//
//            aLine.alGLD = alGLD
//        }
//
//        //--- we collect periods of liquid level states (refueling, draining, consumption) and apply filters for refueling / draining / consumption
//        fun getLiquidStatePeriodData(
//            sca: SensorConfigLiquidLevel,
//            axisIndex: Int,
//            aLine: ChartElementDTO,
//            alLSPD: MutableList<LiquidStatePeriodData>,
//            gh: LiquidGraphicHandler
//        ) {
//            //--- zero pass: collecting periods from points; we start from the 1st point, because 0th point is always "normal"
//            var begPos = 0
//            var curColorIndex = gh.getLineNormalColorIndex(axisIndex)
//            for (i in 1 until aLine.alGLD.size) {
//                val gdl = aLine.alGLD[i]
//                val newColorIndex = gdl.colorIndex
//                //--- a period of a new type has begun, we end the previous period of a different type
//                if (newColorIndex != curColorIndex) {
//                    //--- the previous period ended at the previous point
//                    val endPos = i - 1
//                    //--- there must be at least two points in the period, we discard one-point periods (usually this is the starting point in the "normal" state)
//                    if (begPos < endPos) {
//                        alLSPD.add(LiquidStatePeriodData(begPos, endPos, curColorIndex))
//                    }
//                    //--- the new period actually starts from the previous point
//                    begPos = i - 1
//                    curColorIndex = newColorIndex
//                }
//            }
//            //--- let's finish the last period
//            val endPos = aLine.alGLD.size - 1
//            if (begPos < endPos) {
//                alLSPD.add(LiquidStatePeriodData(begPos, endPos, curColorIndex))
//            }
//
//            //--- first pass: turn insignificant fillings / drains into "normal" consumption
//            run {
//                var pos = 0
//                while (pos < alLSPD.size) {
//                    val lspd = alLSPD[pos]
//                    //--- skip empty or normal periods immediately
//                    if (lspd.colorIndex == gh.getLineNoneColorIndex(axisIndex) || lspd.colorIndex == gh.getLineNormalColorIndex(axisIndex)) {
//                        pos++
//                        continue
//                    }
//                    //--- determine the insignificance of filling / draining
//                    val begGDL = aLine.alGLD[lspd.begPos]
//                    val endGDL = aLine.alGLD[lspd.endPos]
//                    val isFound = if (lspd.colorIndex == gh.getLineAboveColorIndex(axisIndex)) {
//                        //--- at the same time we catch periods with zero length
//                        endGDL.y - begGDL.y < sca.detectIncMinDiff || endGDL.x - begGDL.x < max(sca.detectIncMinLen, 1)
//                    } else if (lspd.colorIndex == gh.getLineBelowColorIndex(axisIndex)) {
//                        //--- at the same time we catch periods with zero length
//                        -(endGDL.y - begGDL.y) < sca.detectDecMinDiff || endGDL.x - begGDL.x < max(sca.detectDecMinLen, 1)
//                    } else {
//                        false
//                    }
//                    //--- insignificant fill / drain found
//                    if (isFound) {
//                        //--- looking for possible normal left / right periods for merging
//                        var prevNormalLSPD: LiquidStatePeriodData? = null
//                        var nextNormalLSPD: LiquidStatePeriodData? = null
//                        if (pos > 0) {
//                            prevNormalLSPD = alLSPD[pos - 1]
//                            if (prevNormalLSPD.colorIndex != gh.getLineNormalColorIndex(axisIndex)) {
//                                prevNormalLSPD = null
//                            }
//                        }
//                        if (pos < alLSPD.size - 1) {
//                            nextNormalLSPD = alLSPD[pos + 1]
//                            if (nextNormalLSPD.colorIndex != gh.getLineNormalColorIndex(axisIndex)) {
//                                nextNormalLSPD = null
//                            }
//                        }
//                        //--- both adjacent periods are normal, all three are merged into one
//                        if (prevNormalLSPD != null && nextNormalLSPD != null) {
//                            prevNormalLSPD.endPos = nextNormalLSPD.endPos
//                            alLSPD.removeAt(pos)
//                            //--- this is not a typo or an error: after deleting the current period, the next period becomes the current one and is also deleted
//                            alLSPD.removeAt(pos)
//                            //--- after merging two periods, pos already points to the next position, there is no need to increase the counter
//                            //pos++;
//                        } else if (prevNormalLSPD != null) {    //--- no normal neighbors, we normalize ourselves
//                            prevNormalLSPD.endPos = lspd.endPos
//                            alLSPD.removeAt(pos)
//                            //--- after merging two periods, pos already points to the next position, there is no need to increase the counter
//                            //pos++;
//                        } else if (nextNormalLSPD != null) {    //--- the right period is normal, we merge with it
//                            nextNormalLSPD.begPos = lspd.begPos
//                            alLSPD.removeAt(pos)
//                            pos++
//                        } else {                                //--- the left period is normal, we merge with it
//                            lspd.colorIndex = gh.getLineNormalColorIndex(axisIndex)
//                            pos++
//                        }
//                        //--- in any case, normalize "our" points of the smoothed graph
//                        for (i in lspd.begPos + 1..lspd.endPos) {
//                            aLine.alGLD[i].colorIndex = gh.getLineNormalColorIndex(axisIndex)
//                        }
//                    } else pos++    //--- otherwise just go to the next period
//                }
//            }
//
//            //--- second pass - we lengthen refueling and drainage by reducing neighboring normal periods
//            for (pos in alLSPD.indices) {
//                val lspd = alLSPD[pos]
//                //--- skip empty or normal periods immediately
//                if (lspd.colorIndex == gh.getLineNoneColorIndex(axisIndex) || lspd.colorIndex == gh.getLineNormalColorIndex(axisIndex)) {
//                    continue
//                }
//
//                //--- looking for a normal period on the left, if necessary
//                val addTimeBefore = if (lspd.colorIndex == gh.getLineAboveColorIndex(axisIndex)) {
//                    sca.incAddTimeBefore
//                } else {
//                    sca.decAddTimeBefore
//                }
//                if (addTimeBefore > 0 && pos > 0) {
//                    val prevNormalLSPD = alLSPD[pos - 1]
//                    if (prevNormalLSPD.colorIndex == gh.getLineNormalColorIndex(axisIndex)) {
//                        val bt = aLine.alGLD[lspd.begPos].x
//                        // --- lengthen the beginning of our period, shorten the previous normal period from the end
//                        // --- namely>, not> =, in order to prevent single-point normal periods (begPos == endPos)
//                        // --- after lengthening the current abnormal
//                        var p = prevNormalLSPD.endPos - 1
//                        while (p > prevNormalLSPD.begPos) {
//                            if (bt - aLine.alGLD[p].x > addTimeBefore) {
//                                break
//                            }
//                            p--
//                        }
//                        //--- the previous position is valid
//                        p++
//                        //--- is there where to lengthen?
//                        if (p < prevNormalLSPD.endPos) {
//                            prevNormalLSPD.endPos = p
//                            lspd.begPos = p
//                            //--- in any case, let's re-mark "our" points of the smoothed graph
//                            for (i in lspd.begPos + 1..lspd.endPos) {
//                                aLine.alGLD[i].colorIndex = lspd.colorIndex
//                            }
//                        }
//                    }
//                }
//                //--- looking for a normal period on the right, if necessary
//                val addTimeAfter = if (lspd.colorIndex == gh.getLineAboveColorIndex(axisIndex)) {
//                    sca.incAddTimeAfter
//                } else {
//                    sca.decAddTimeAfter
//                }
//                if (addTimeAfter > 0 && pos < alLSPD.size - 1) {
//                    val nextNormalLSPD = alLSPD[pos + 1]
//                    if (nextNormalLSPD.colorIndex == gh.getLineNormalColorIndex(axisIndex)) {
//                        val et = aLine.alGLD[lspd.endPos].x
//                        //--- lengthen the end of our period, shorten the next normal period from the beginning
//                        //--- exactly <, not <=, in order to prevent single-point normal periods (begPos == endPos)
//                        //--- after lengthening the current abnormal
//                        var p = nextNormalLSPD.begPos + 1
//                        while (p < nextNormalLSPD.endPos) {
//                            if (aLine.alGLD[p].x - et > addTimeAfter) {
//                                break
//                            }
//                            p++
//                        }
//                        //--- the previous position is valid
//                        p--
//                        //--- is there where to lengthen?
//                        if (p > nextNormalLSPD.begPos) {
//                            nextNormalLSPD.begPos = p
//                            lspd.endPos = p
//                            //--- in any case, let's re-mark "our" points of the smoothed graph
//                            for (i in lspd.begPos + 1..lspd.endPos) {
//                                aLine.alGLD[i].colorIndex = lspd.colorIndex
//                            }
//                        }
//                    }
//                }
//            }
//
//            //--- third pass: remove insignificant (short) "normal" periods between identical abnormal
//            var pos = 0
//            while (pos < alLSPD.size) {
//                val lspd = alLSPD[pos]
//                //--- skip abnormal periods immediately
//                if (lspd.colorIndex != gh.getLineNormalColorIndex(axisIndex)) {
//                    pos++
//                    continue
//                }
//                //--- determine the insignificance of the expense
//                val begGDL = aLine.alGLD[lspd.begPos]
//                val endGDL = aLine.alGLD[lspd.endPos]
//                //--- at the same time we catch periods with zero length
//                if (endGDL.x - begGDL.x < max(sca.usingMinLen, 1)) {
//                    //--- looking for abnormal periods left / right for merging
//                    var prevAbnormalLSPD: LiquidStatePeriodData? = null
//                    var nextAbnormalLSPD: LiquidStatePeriodData? = null
//                    if (pos > 0) {
//                        prevAbnormalLSPD = alLSPD[pos - 1]
//                        if (prevAbnormalLSPD.colorIndex == gh.getLineNormalColorIndex(axisIndex)) {
//                            prevAbnormalLSPD = null
//                        }
//                    }
//                    if (pos < alLSPD.size - 1) {
//                        nextAbnormalLSPD = alLSPD[pos + 1]
//                        if (nextAbnormalLSPD.colorIndex == gh.getLineNormalColorIndex(axisIndex)) {
//                            nextAbnormalLSPD = null
//                        }
//                    }
//
//                    //--- both neighboring periods are equally abnormal, all three are merged into one (two neighboring differently abnormal periods cannot be merged)
//                    if (prevAbnormalLSPD != null && nextAbnormalLSPD != null && prevAbnormalLSPD.colorIndex == nextAbnormalLSPD.colorIndex) {
//
//                        prevAbnormalLSPD.endPos = nextAbnormalLSPD.endPos
//                        alLSPD.removeAt(pos)
//                        //--- this is not a typo or an error: after deleting the current period, the next period becomes the current one and is also deleted
//                        alLSPD.removeAt(pos)
//                        //--- after merging three periods, pos already points to the next position, there is no need to increase the counter
//                        //pos++;
//                        //--- denormalize "our" points of the smoothed graph
//                        for (i in lspd.begPos + 1..lspd.endPos) {
//                            aLine.alGLD[i].colorIndex = prevAbnormalLSPD.colorIndex
//                        }
//                    } else pos++    //--- otherwise just go to the next period
//                } else pos++    //--- otherwise just go to the next period
//            }
//        }
//
//        //--- we collect periods, values and place of refueling / draining
//        fun calcIncDec(
//            conn: CoreAdvancedConnection,
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            oc: ObjectConfig,
//            sca: SensorConfigLiquidLevel,
//            begTime: Int,
//            endTime: Int,
//            isWaybill: Boolean,
//            alBeg: List<Int>,
//            alEnd: List<Int>,
//            calcMode: Int,
//            hmZoneData: Map<Int, ZoneData>,
//            calcZoneID: Int,
//            axisIndex: Int,
//        ): List<LiquidIncDecData> {
//            val alLIDD = mutableListOf<LiquidIncDecData>()
//
//            val aLine = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 2, false)
//            val alLSPD = mutableListOf<LiquidStatePeriodData>()
//            getSmoothLiquidGraphicData(conn, alRawTime, alRawData, oc.scg, sca, begTime, endTime, axisIndex, aLine, alLSPD)
//
//            val llcd = LiquidLevelCalcData(sca.containerType, aLine, alLSPD)
//            calcLiquidUsingByLevel(sca, llcd, conn, oc, begTime, endTime, axisIndex)
//
//            for (lspd in llcd.alLSPD!!) {
//                val begGLD = llcd.aLine!!.alGLD[lspd.begPos]
//                val endGLD = llcd.aLine!!.alGLD[lspd.endPos]
//                var lidd: LiquidIncDecData? = null
//                if (lspd.colorIndex == ChartColorIndex.LINE_ABOVE_0 && calcMode >= 0) {
//                    lidd = LiquidIncDecData(begGLD.x, endGLD.x, begGLD.y.toDouble(), endGLD.y.toDouble())
//                } else if (lspd.colorIndex == ChartColorIndex.LINE_BELOW_0 && calcMode <= 0) {
//                    lidd = LiquidIncDecData(begGLD.x, endGLD.x, begGLD.y.toDouble(), endGLD.y.toDouble())
//                }
//
//                if (lidd != null) {
//                    var inZoneAll = false
//                    val tsZoneName = TreeSet<String>()
//                    oc.scg?.let { scg ->
//                        for (pos in lspd.begPos..lspd.endPos) {
//                            val gd = AbstractObjectStateCalc.getGeoData(scg, alRawData[pos]) ?: continue
//                            val pixPoint = XyProjection.wgs_pix(gd.wgs)
//
//                            val inZone = fillZoneList(hmZoneData, calcZoneID, pixPoint, tsZoneName)
//                            //--- filter by geofences, if specified
//                            if (calcZoneID != 0 && inZone) inZoneAll = true
//                        }
//                    }
//                    //--- filter by geofences, if specified
//                    if (calcZoneID != 0 && !inZoneAll) continue
//
//                    //--- filter by directions time, if set
//                    if (isWaybill) {
//                        var inWaybill = false
//                        for (wi in alBeg.indices) if (lidd.begTime < alEnd[wi] && lidd.endTime > alBeg[wi]) {
//                            inWaybill = true
//                            break
//                        }
//                        if (inWaybill) continue
//                    }
//
//                    lidd.objectConfig = oc
//                    lidd.sca = sca
//                    lidd.sbZoneName = getSBFromIterable(tsZoneName, ", ")
//
//                    alLIDD.add(lidd)
//                }
//            }
//
//            return alLIDD
//        }
//
//        // --- (new condition - not academically / uselessly Ignore, but consider that equipment outside the specified limits DOES NOT WORK)
//        //if(  sensorData < scw.minIgnore || sensorData > scw.maxIgnore  ) continue;
//        fun getWorkSensorValue(scw: SensorConfigWork, sensorData: Double): Boolean =
//            if (scw.minIgnore < scw.maxIgnore) {
//                sensorData > scw.minIgnore && sensorData < scw.maxIgnore
//            } else {
//                sensorData > scw.minIgnore || sensorData < scw.maxIgnore
//            } &&
//                ((scw.activeValue == 0) xor (sensorData > scw.boundValue))
//
//        fun getSignalSensorValue(scs: SensorConfigSignal, sensorData: Double?): Boolean =
//            sensorData != null &&
//                if (scs.minIgnore < scs.maxIgnore) {
//                    sensorData > scs.minIgnore && sensorData < scs.maxIgnore
//                } else {
//                    sensorData > scs.minIgnore || sensorData < scs.maxIgnore
//                } &&
//                ((scs.activeValue == 0) xor (sensorData > scs.boundValue))
//
//        //--- private part -----------------------------------------------------------------------------------------------------------
//
//        private fun calcGeo(
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            scg: SensorConfigGeo,
//            begTime: Int,
//            endTime: Int,
//            result: ObjectCalc
//        ) {
//            //--- in normal calculations, we do not need trajectory points, so we give the maximum scale.
//            //--- excess is also not needed, so we give maxEnabledOverSpeed = 0
//            result.gcd = calcGeoSensor(alRawTime, alRawData, scg, begTime, endTime, 1_000_000_000, 0, null)
//
//            //--- if the standard for liquid (fuel) is set, we will calculate it
//            if (scg.isUseRun && scg.liquidName.isNotBlank() && scg.liquidNorm != 0.0) {
//                val liquidUsing = scg.liquidNorm * result.gcd!!.run / 100.0
//
//                result.tmLiquidUsing["${scg.descr} (расч.) ${scg.liquidName}"] = liquidUsing
//                addLiquidUsingSum(scg.group, scg.liquidName, liquidUsing, result)
//            }
//
//        }
//
//        private fun mergePeriods(alPD: MutableList<AbstractPeriodData>, minOnTime: Int, minOffTime: Int) {
//            //--- ejection of insufficient on / off periods
//            //--- and the subsequent merging of adjacent off / on, respectively
//            while (true) {
//                var isShortFound = false
//                var i = 0
//                while (i < alPD.size) {
//                    val pd = alPD[i]
//                    //--- if the period is too short to account for
//                    if (pd.endTime - pd.begTime < (if (pd.getState() != 0) minOnTime else minOffTime)) {
//                        //--- if there is at least one long opposite period nearby, then we remove the short one and connect the adjacent ones
//                        var isLongFound = false
//                        if (i > 0) {
//                            val pdPrev = alPD[i - 1]
//                            isLongFound = isLongFound or (pdPrev.endTime - pdPrev.begTime >= if (pdPrev.getState() != 0) minOnTime else minOffTime)
//                        }
//                        if (i < alPD.size - 1) {
//                            val pdNext = alPD[i + 1]
//                            isLongFound = isLongFound or (pdNext.endTime - pdNext.begTime >= if (pdNext.getState() != 0) minOnTime else minOffTime)
//                        }
//                        //--- found long neighbor (s)
//                        if (isLongFound) {
//                            //--- first short period
//                            if (i == 0) {
//                                alPD[1].begTime = alPD[0].begTime
//                                alPD.removeAt(0)
//                                i = 1    // the current period is already long, we go immediately further
//                            } else if (i == alPD.size - 1) {    //--- middle short period
//                                alPD[i - 1].endTime = alPD[i].endTime
//                                alPD.removeAt(i)
//                                i++    // the current period is already long, we go immediately further (although for the last period this is no longer necessary)
//                            } else {   //--- last short period
//                                alPD[i - 1].endTime = alPD[i + 1].endTime
//                                alPD.removeAt(i)   // delete the current short period
//                                alPD.removeAt(i)   // delete the next opposite period, merged with the previous opposite
//                                //i++ - do not need to be done, since the current period is now new with an unknown duration
//                            }
//                            isShortFound = true    // deletion was, it makes sense to go through the chain again
//                        } else {
//                            i++ // no long neighbors found - let's move on
//                        }
//                    } else {
//                        i++
//                    }
//                }
//                //--- nothing more to throw away and connect
//                if (!isShortFound) {
//                    break
//                }
//            }
//        }
//
//        //--- smoothing the graph of the analog value of the liquid / fuel level (abbreviated call for generating reports)
//        private fun getSmoothLiquidGraphicData(
//            conn: CoreAdvancedConnection,
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            scg: SensorConfigGeo?,
//            sca: SensorConfigLiquidLevel,
//            begTime: Int,
//            endTime: Int,
//            axisIndex: Int,
//            aLine: ChartElementDTO,
//            alLSPD: MutableList<LiquidStatePeriodData>
//        ) {
//            val gh = LiquidGraphicHandler()
//            getSmoothAnalogGraphicData(
//                conn = conn,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                scg = scg,
//                sca = sca,
//                begTime = begTime,
//                endTime = endTime,
//                xScale = 0,
//                yScale = 0.0,
//                axisIndex = axisIndex,
//                aMinLimit = null,
//                aMaxLimit = null,
//                aLine = aLine,
//                graphicHandler = gh
//            )
//            getLiquidStatePeriodData(
//                sca = sca,
//                axisIndex = axisIndex,
//                aLine = aLine,
//                alLSPD = alLSPD,
//                gh = gh
//            )
//        }
//
//        private fun calcLiquidUsingByLevel(
//            sca: SensorConfigLiquidLevel,
//            llcd: LiquidLevelCalcData,
//            conn: CoreAdvancedConnection,
//            oc: ObjectConfig,
//            begTime: Int,
//            endTime: Int,
//            axisIndex: Int,
//        ) {
//            val aLine = llcd.aLine
//            val alLSPD = llcd.alLSPD
//
//            if (alLSPD!!.isNotEmpty()) {
//                //--- first we count the usual flow
//                for (i in alLSPD.indices) {
//                    val lspd = alLSPD[i]
//                    val begGDL = aLine!!.alGLD[lspd.begPos]
//                    val endGDL = aLine.alGLD[lspd.endPos]
//                    when (lspd.colorIndex) {
//                        ChartColorIndex.LINE_NORMAL_0 -> {
//                            llcd.usingTotal += begGDL.y - endGDL.y
//                        }
//
//                        ChartColorIndex.LINE_ABOVE_0 -> {
//                            llcd.incTotal += endGDL.y - begGDL.y
//                            if (sca.isUsingCalc) {
//                                //--- looking for the previous normal period
//                                val avgUsing = getPrevNormalPeriodAverageUsing(conn, oc, sca, llcd, begTime, endTime, i, axisIndex)
//                                val calcUsing = avgUsing * (endGDL.x - begGDL.x)
//                                llcd.usingCalc += calcUsing
//                                llcd.usingTotal += calcUsing
//                            }
//                        }
//
//                        ChartColorIndex.LINE_BELOW_0 -> {
//                            llcd.decTotal += begGDL.y - endGDL.y
//                            if (sca.isUsingCalc) {
//                                //--- looking for the previous normal period
//                                val avgUsing = getPrevNormalPeriodAverageUsing(conn, oc, sca, llcd, begTime, endTime, i, axisIndex)
//                                val calcUsing = avgUsing * (endGDL.x - begGDL.x)
//                                llcd.usingCalc += calcUsing
//                                llcd.usingTotal += calcUsing
//                            }
//                        }
//
//                        else -> {}
//                    }
//                }
//            }
//        }
//
//        //--- looking for the previous normal period to calculate the average consumption during refueling / draining
//        private fun getPrevNormalPeriodAverageUsing(
//            conn: CoreAdvancedConnection,
//            oc: ObjectConfig,
//            sca: SensorConfigLiquidLevel,
//            llcd: LiquidLevelCalcData,
//            begTime: Int,
//            endTime: Int,
//            curPos: Int,
//            axisIndex: Int,
//        ): Double {
//
//            var lspdPrevNorm: LiquidStatePeriodData? = null
//            var aLinePrevNorm: ChartElementDTO? = null
//
//            val aLine = llcd.aLine
//            val alLSPD = llcd.alLSPD
//
//            for (i in curPos - 1 downTo 0) {
//                val lspdPrev = alLSPD!![i]
//                val begGDLPrev = aLine!!.alGLD[lspdPrev.begPos]
//                val endGDLPrev = aLine.alGLD[lspdPrev.endPos]
//
//                //--- the found normal period is not the first (no matter how long) or the first, but with a sufficient duration for calculations
//                if (lspdPrev.colorIndex == ChartColorIndex.LINE_NORMAL_0 && (i > 0 || endGDLPrev.x - begGDLPrev.x >= MAX_CALC_PREV_NORMAL_PERIOD)) {
//
//                    lspdPrevNorm = lspdPrev
//                    aLinePrevNorm = aLine
//                    break
//                }
//            }
//            //--- no suitable normal site was found in the entire requested period - we request an extended period
//            if (lspdPrevNorm == null) {
//                //--- let's extend the period into the past with a two-fold margin - this will not greatly affect the processing speed
//                val (alRawTimeExt, alRawDataExt) = loadAllSensorData(conn, oc, begTime - MAX_CALC_PREV_NORMAL_PERIOD * 2, endTime)
//
//                val aLineExt = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 2, false)
//                val alLSPDExt = mutableListOf<LiquidStatePeriodData>()
//                getSmoothLiquidGraphicData(conn, alRawTimeExt, alRawDataExt, oc.scg, sca, begTime - MAX_CALC_PREV_NORMAL_PERIOD * 2, endTime, axisIndex, aLineExt, alLSPDExt)
//
//                //--- the current period in the current range
//                val lspdCur = alLSPD!![curPos]
//                val begGDLPCur = aLine!!.alGLD[lspdCur.begPos]
//                val endGDLPCur = aLine.alGLD[lspdCur.endPos]
//                //--- find the current refueling / draining period in the new extended period
//                var curPosExt = 0
//                while (curPosExt < alLSPDExt.size) {
//                    val lspdCurExt = alLSPDExt[curPosExt]
//                    val begGDLPCurExt = aLineExt.alGLD[lspdCurExt.begPos]
//                    val endGDLPCurExt = aLineExt.alGLD[lspdCurExt.endPos]
//                    if (begGDLPCur.x == begGDLPCurExt.x && endGDLPCur.x == endGDLPCurExt.x) break
//                    curPosExt++
//                }
//                for (i in curPosExt - 1 downTo 0) {
//                    val lspdPrevExt = alLSPDExt[i]
//                    if (lspdPrevExt.colorIndex == ChartColorIndex.LINE_NORMAL_0) {
//                        lspdPrevNorm = lspdPrevExt
//                        aLinePrevNorm = aLineExt
//                        break
//                    }
//                }
//            }
//            //--- let's calculate the same average consumption in the previous normal period
//            if (lspdPrevNorm != null) {
//                var begGDLPrevNorm = aLinePrevNorm!!.alGLD[lspdPrevNorm.begPos]
//                val endGDLPrevNorm = aLinePrevNorm.alGLD[lspdPrevNorm.endPos]
//                //--- the normal period is too long, we take the last N hours - adjust begPos
//                if (endGDLPrevNorm.x - begGDLPrevNorm.x > MAX_CALC_PREV_NORMAL_PERIOD) {
//                    for (begPos in lspdPrevNorm.begPos + 1 until lspdPrevNorm.endPos) {
//                        begGDLPrevNorm = aLinePrevNorm.alGLD[begPos]
//                        if (endGDLPrevNorm.x - begGDLPrevNorm.x <= MAX_CALC_PREV_NORMAL_PERIOD) break
//                    }
//                }
//
//                return if (endGDLPrevNorm.x == begGDLPrevNorm.x) {
//                    0.0
//                } else {
//                    (begGDLPrevNorm.y.toDouble() - endGDLPrevNorm.y) / (endGDLPrevNorm.x - begGDLPrevNorm.x)
//                }
//            } else {
//                return 0.0
//            }
//        }
//
////        private fun searchGDL(aLine: GraphicDataContainer, time: Int): Double {
////            //--- if the time is on / outside the search boundaries, then we take the boundary value
////            if (time <= aLine.alGLD[0].x) return aLine.alGLD[0].y
////            else if (time >= aLine.alGLD[aLine.alGLD.size - 1].x) return aLine.alGLD[aLine.alGLD.size - 1].y
////
////            var pos1 = 0
////            var pos2 = aLine.alGLD.size - 1
////            while (pos1 <= pos2) {
////                val posMid = (pos1 + pos2) / 2
////                val valueMid = aLine.alGLD[posMid].x
////
////                if (time < valueMid) {
////                    pos2 = posMid - 1
////                }
////                else if (time > valueMid) {
////                    pos1 = posMid + 1
////                }
////                else {
////                    return aLine.alGLD[posMid].y
////                }
////            }
////            //--- if nothing was found, then now pos2 is to the left of the desired value, and pos1 is to the right of it.
////            //--- In this case, we approximate the value
////            return (time - aLine.alGLD[pos2].x) / (aLine.alGLD[pos1].x - aLine.alGLD[pos2].x) * (aLine.alGLD[pos1].y - aLine.alGLD[pos2].y) + aLine.alGLD[pos2].y
////        }
//
//        //--- filling in standard output lines (for tabular forms and reports) ---------------------------
//
//        fun fillGeoString(userConfig: UserConfig, result: ObjectCalc) {
//            val zoneId = userConfig.upZoneId
//            result.gcd?.let { gcd ->
//                result.sGeoName += gcd.descr
//                result.sGeoRun += if (gcd.run < 0) '-' else getSplittedDouble(gcd.run, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                result.sGeoOutTime += if (gcd.outTime == 0) "-" else DateTime_DMYHMS(zoneId, gcd.outTime)
//                result.sGeoInTime += if (gcd.inTime == 0) "-" else DateTime_DMYHMS(zoneId, gcd.inTime)
//                result.sGeoWayTime += if (gcd.outTime == 0 || gcd.inTime == 0) '-' else secondIntervalToString(gcd.outTime, gcd.inTime)
//                result.sGeoMovingTime += if (gcd.movingTime < 0) '-' else secondIntervalToString(gcd.movingTime)
//                result.sGeoParkingTime += if (gcd.parkingTime < 0) '-' else secondIntervalToString(gcd.parkingTime)
//                result.sGeoParkingCount += if (gcd.parkingCount < 0) {
//                    '-'
//                } else if (userConfig.upIsUseThousandsDivider) {
//                    getSplittedLong(gcd.parkingCount.toLong())
//                } else {
//                    gcd.parkingCount.toString()
//                }
//            }
//        }
//
////        fun fillZoneString(hmZoneData: Map<Int, ZoneData>, p: XyPoint): StringBuilder {
////            val tsZoneName = TreeSet<String>()
////            for (zd in hmZoneData.values)
////                if (zd.polygon!!.isContains(p))
////                    tsZoneName.add(zd.name)
////            return getSBFromIterable(tsZoneName, ", ")
////        }
//
//        fun fillWorkString(userConfig: UserConfig, result: ObjectCalc) {
//            val workPair = fillWorkString(userConfig, result.tmWork)
//            result.sWorkName = workPair.first
//            result.sWorkValue = workPair.second
//        }
//
//        fun fillWorkString(userConfig: UserConfig, tmWork: SortedMap<String, WorkCalcData>): Pair<String, String> {
//            var sWorkName = ""
//            var sWorkTotal = ""
//            tmWork.forEach { (workName, wcd) ->
//                if (sWorkName.isNotEmpty()) {
//                    sWorkName += '\n'
//                    sWorkTotal += '\n'
//                }
//                sWorkName += workName
//                sWorkTotal += getSplittedDouble(wcd.onTime.toDouble() / 60.0 / 60.0, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//            }
//            return Pair(sWorkName, sWorkTotal)
//        }
//
//        fun fillEnergoString(userConfig: UserConfig, result: ObjectCalc) {
//            result.tmEnergo.forEach { (descr, e) ->
//                if (result.sEnergoName.isNotEmpty()) {
//                    result.sEnergoName += '\n'
//                    result.sEnergoValue += '\n'
//                }
//                result.sEnergoName += descr
//                result.sEnergoValue += getSplittedDouble(e, getPrecision(e), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//            }
//
//            result.allSumData.tmEnergo.forEach { (sensorType, dataByPhase) ->
//                dataByPhase.forEach { (phase, value) ->
//                    if (result.sAllSumEnergoName.isNotEmpty()) {
//                        result.sAllSumEnergoName += '\n'
//                        result.sAllSumEnergoValue += '\n'
//                    }
//                    result.sAllSumEnergoName += (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + getPhaseDescr(phase)
//                    result.sEnergoValue += getSplittedDouble(value, getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                }
//            }
//
//        }
//
//        fun fillLiquidUsingString(userConfig: UserConfig, result: ObjectCalc) {
//            val liquidPair = fillLiquidUsingString(userConfig, result.tmLiquidUsing)
//            result.sLiquidUsingName = liquidPair.first
//            result.sLiquidUsingValue = liquidPair.second
//
//            val allSumLiquidPair = fillLiquidUsingString(userConfig, result.allSumData.tmLiquidUsing)
//            result.sAllSumLiquidName = allSumLiquidPair.first
//            result.sAllSumLiquidValue = allSumLiquidPair.second
//        }
//
//        fun fillLiquidUsingString(userConfig: UserConfig, tmLiquidUsing: SortedMap<String, Double>): Pair<String, String> {
//            var sLiquidUsingName = ""
//            var sLiquidUsing = ""
//            tmLiquidUsing.forEach { (name, total) ->
//                if (sLiquidUsingName.isNotEmpty()) {
//                    sLiquidUsingName += '\n'
//                    sLiquidUsing += '\n'
//                }
//                sLiquidUsingName += name
//                sLiquidUsing += getSplittedDouble(total, getPrecision(total), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//            }
//            return Pair(sLiquidUsingName, sLiquidUsing)
//        }
//
//        private fun fillLiquidLevelString(userConfig: UserConfig, result: ObjectCalc) {
//            val isUsingCalc = result.tmLiquidLevel.values.any { it.usingCalc > 0.0 }
//
//            result.tmLiquidLevel.forEach { (liquidName, llcd) ->
//                if (result.sLiquidLevelName.isNotEmpty()) {
//                    result.sLiquidLevelName += '\n'
//                    result.sLiquidLevelBeg += '\n'
//                    result.sLiquidLevelEnd += '\n'
//                    result.sLiquidLevelIncTotal += '\n'
//                    result.sLiquidLevelDecTotal += '\n'
//                    result.sLiquidLevelUsingTotal += '\n'
//                    if (isUsingCalc) {
//                        result.sLiquidLevelUsingCalc += '\n'
//                    }
//                }
//                result.sLiquidLevelName += liquidName
//                result.sLiquidLevelBeg += getSplittedDouble(llcd.begLevel, getPrecision(llcd.begLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                result.sLiquidLevelEnd += getSplittedDouble(llcd.endLevel, getPrecision(llcd.endLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                result.sLiquidLevelIncTotal += getSplittedDouble(llcd.incTotal, getPrecision(llcd.incTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                result.sLiquidLevelDecTotal += getSplittedDouble(llcd.decTotal, getPrecision(llcd.decTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                result.sLiquidLevelUsingTotal += getSplittedDouble(llcd.usingTotal, getPrecision(llcd.usingTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                if (isUsingCalc) {
//                    result.sLiquidLevelUsingCalc +=
//                        if (llcd.usingCalc <= 0.0) {
//                            "-"
//                        } else {
//                            getSplittedDouble(llcd.usingCalc, getPrecision(llcd.usingCalc), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                        }
//                }
//            }
//
//            result.allSumData.tmLiquidIncDec.forEach { (liquidName, pairIncDec) ->
//                if (result.sLiquidLevelLiquidName.isNotBlank()) {
//                    result.sLiquidLevelLiquidName += '\n'
//                    result.sLiquidLevelLiquidInc += '\n'
//                    result.sLiquidLevelLiquidDec += '\n'
//                }
//                result.sLiquidLevelLiquidName += liquidName
//                result.sLiquidLevelLiquidInc += getSplittedDouble(pairIncDec.first, getPrecision(pairIncDec.first), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//                result.sLiquidLevelLiquidDec += getSplittedDouble(pairIncDec.second, getPrecision(pairIncDec.second), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//            }
//        }
//
//        fun getPhaseDescr(phase: Int) =
//            when (phase) {
//                0 -> " (сумма фаз)"
//                1 -> " (фаза A)"
//                2 -> " (фаза B)"
//                3 -> " (фаза C)"
//                else -> " (неизв. фаза)"
//            }
//
//
//        fun fillZoneList(hmZoneData: Map<Int, ZoneData>, reportZone: Int, p: XyPoint, tsZoneName: TreeSet<String>): Boolean {
//            var inZone = false
//            for ((zoneID, zd) in hmZoneData) {
//                if (zd.polygon!!.isContains(p)) {
//                    var sZoneInfo = zd.name
//                    if (zd.descr.isNotEmpty()) {
//                        sZoneInfo += " (${zd.descr})"
//                    }
//
//                    tsZoneName.add(sZoneInfo)
//                    if (reportZone != 0 && reportZone == zoneID) inZone = true
//                }
//            }
//            return inZone
//        }
//    }
//}
