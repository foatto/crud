package foatto.mms.core_mms.service

import foatto.core.app.chart.AnalogueValueType
import foatto.core_server.service.CoreServiceWorker
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.sql.AdvancedConnection
import foatto.util.AdvancedByteBuffer
import foatto.util.AdvancedLogger
import foatto.util.DateTime_YMDHMS
import foatto.util.getCurrentTimeInt
import java.io.IOException
import java.nio.ByteOrder
import java.time.ZoneId
import java.util.*
import kotlin.system.exitProcess

class Aggregator(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "Aggregator"
                if (args.size == 1) {
                    Aggregator(args[0]).run()
                    exitCode = 1
                } else {
                    println("Usage: $serviceWorkerName <ini-file-name>")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        private val alPreparedSensorTypes = listOf(
            //--- counter sensors
//            SensorConfig.SENSOR_MASS_ACCUMULATED,
//            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
//            SensorConfig.SENSOR_LIQUID_USING,
//            SensorConfig.SENSOR_ENERGO_COUNT_AD,
//            SensorConfig.SENSOR_ENERGO_COUNT_AR,
//            SensorConfig.SENSOR_ENERGO_COUNT_RD,
//            SensorConfig.SENSOR_ENERGO_COUNT_RR,
            //--- analogue sensors
            SensorConfig.SENSOR_LIQUID_LEVEL,
//            SensorConfig.SENSOR_WEIGHT,
//            SensorConfig.SENSOR_TURN,
//            SensorConfig.SENSOR_PRESSURE,
//            SensorConfig.SENSOR_TEMPERATURE,
//            SensorConfig.SENSOR_VOLTAGE,
//            SensorConfig.SENSOR_POWER,
//            SensorConfig.SENSOR_DENSITY,
//            SensorConfig.SENSOR_MASS_FLOW,
//            SensorConfig.SENSOR_VOLUME_FLOW,
//            SensorConfig.SENSOR_ENERGO_VOLTAGE,
//            SensorConfig.SENSOR_ENERGO_CURRENT,
//            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
//            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
//            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
//            SensorConfig.SENSOR_ENERGO_POWER_FULL,
            //--- equipment work sensor
//            SensorConfig.SENSOR_WORK,
        )

        /*
        Оставшиеся типы датчиков:
        SENSOR_SIGNAL = -2   // Есть/нет
        SENSOR_GEO = -1           // координаты, км/ч, м или км
        SENSOR_WORK = 1           // мото * час
        SENSOR_LIQUID_FLOW_CALC,
        SENSOR_LIQUID_USING_COUNTER_STATE = 17
         */
        private const val BATCH_SIZE = "batch_size"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val isRunOnce: Boolean = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var batchSize = 100_000

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val zoneId = ZoneId.systemDefault()
    private var firstCycle = true

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadConfig() {
        super.loadConfig()

        batchSize = hmConfig[BATCH_SIZE]?.toIntOrNull() ?: 100_000
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun cycle() {
        try {
            if (firstCycle) {
                onceCheckZeroRawTimes()
                firstCycle = false
            }

            val alSensorInfo = loadSensorInfo()
            AdvancedLogger.debug("--- Sensors count = ${alSensorInfo.size} ---")
            alSensorInfo.firstOrNull()?.let { sensorCalcInfo ->
                AdvancedLogger.debug("- First calc_time = ${DateTime_YMDHMS(zoneId, sensorCalcInfo.calcTime)}")
            }

            val tmTypeTime = TreeMap<Int, Int>()

            for (sensorInfo in alSensorInfo) {
                val sensorBegTime = getCurrentTimeInt()

                val (alValueSensor, alValueData) = getSensorCalibration(sensorInfo.sensorId)

                when (sensorInfo.sensorType) {
                    SensorConfig.SENSOR_MASS_ACCUMULATED,
                    SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                    SensorConfig.SENSOR_LIQUID_USING,
                    SensorConfig.SENSOR_ENERGO_COUNT_AD,
                    SensorConfig.SENSOR_ENERGO_COUNT_AR,
                    SensorConfig.SENSOR_ENERGO_COUNT_RD,
                    SensorConfig.SENSOR_ENERGO_COUNT_RR,
                    -> {
                        calcCounter(
                            sensorInfo = sensorInfo,
                            alValueSensor = alValueSensor,
                            alValueData = alValueData,
                        )
                    }

                    SensorConfig.SENSOR_LIQUID_LEVEL,
                    SensorConfig.SENSOR_WEIGHT,
                    SensorConfig.SENSOR_TURN,
                    SensorConfig.SENSOR_PRESSURE,
                    SensorConfig.SENSOR_TEMPERATURE,
                    SensorConfig.SENSOR_VOLTAGE,
                    SensorConfig.SENSOR_POWER,
                    SensorConfig.SENSOR_DENSITY,
                    SensorConfig.SENSOR_MASS_FLOW,
                    SensorConfig.SENSOR_VOLUME_FLOW,
                    SensorConfig.SENSOR_ENERGO_VOLTAGE,
                    SensorConfig.SENSOR_ENERGO_CURRENT,
                    SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                    SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
                    SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
                    -> {
                        calcAnalogue(
                            sensorInfo = sensorInfo,
                            alValueSensor = alValueSensor,
                            alValueData = alValueData,
                        )
                    }

                    SensorConfig.SENSOR_WORK -> {
                        calcWork(sensorInfo = sensorInfo)
                    }

                    else -> {
                        AdvancedLogger.error("Unknown sensor type = ${sensorInfo.sensorType}")
                    }
                }
                alConn[0].commit()
                AdvancedLogger.debug("Calc finished: old calc_time = ${DateTime_YMDHMS(zoneId, sensorInfo.calcTime)}")

                val timeCount = tmTypeTime.getOrPut(sensorInfo.sensorType) { 0 }
                tmTypeTime[sensorInfo.sensorType] = timeCount + (getCurrentTimeInt() - sensorBegTime)
            }

            AdvancedLogger.debug("--- sensor types statistics ---")
            tmTypeTime.forEach { (sensorType, time) ->
                AdvancedLogger.debug("$sensorType = $time")
            }

            if (alSensorInfo.isEmpty()) {
                AdvancedLogger.debug("--- SLEEP ---")
                Thread.sleep(60_000)
            }
        } catch (ioe: IOException) {
            alConn[0].rollback()
            AdvancedLogger.error(ioe)
        } catch (t: Throwable) {
            alConn[0].rollback()
            t.printStackTrace()
            //--- передаём ошибку дальше
            throw t
        }
    }

    private fun onceCheckZeroRawTimes() {
        val hmIds = mutableMapOf<Int, MutableList<Int>>()

        var rs = alConn[0].executeQuery(
            """
                SELECT object_id , id  
                FROM MMS_sensor
                WHERE raw_time = 0 
                    AND calc_time <> 0 
                    AND object_id <> 0 
            """
        )
        while (rs.next()) {
            val alId = hmIds.getOrPut(rs.getInt(1)) { mutableListOf() }
            alId += rs.getInt(2)
        }
        rs.close()

        hmIds.forEach { (objectId, sensorIds) ->
            rs = alConn[0].executeQuery(
                """
                    SELECT COUNT(ontime), MAX(ontime)  
                    FROM MMS_data_$objectId
                """
            )
            val newRawTime = if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    rs.getInt(2)
                } else {
                    0
                }
            } else {
                0
            }
            rs.close()
            //--- дополнительная проверка на raw_time = 0, вдруг оно уже изменилось
            alConn[0].executeUpdate(
                """
                    UPDATE MMS_sensor 
                    SET ${if (newRawTime == 0) "calc_time" else "raw_time"} = $newRawTime
                    WHERE id IN ( ${sensorIds.joinToString(" , ")} )
                    AND raw_time = 0
                """
            )
        }
        alConn[0].commit()
    }

    private fun loadSensorInfo(): List<SensorInfo> {
        val alSensorInfo = mutableListOf<SensorInfo>()
        val rsSensors = alConn[0].executeQuery(
            """
                SELECT 
                    id , object_id , sensor_type , port_num , calc_time , ignore_min_sensor , ignore_max_sensor , 
                    is_absolute_count , 
                    analog_min_limit , analog_max_limit , smooth_time ,
                    analog_detect_inc , analog_detect_dec ,
                    bound_value , active_value ,
                    CAST(raw_time AS BIGINT) - CAST(calc_time AS BIGINT) AS diff_time
                FROM MMS_sensor
                WHERE sensor_type IN ( ${alPreparedSensorTypes.joinToString(" , ")} )
                AND is_use_aggregator = 1
                AND CAST(raw_time AS BIGINT) - CAST(calc_time AS BIGINT) > 0 
                ORDER BY diff_time DESC
            """
        )
        while (rsSensors.next()) {
            var pos = 1
            alSensorInfo += SensorInfo(
                sensorId = rsSensors.getInt(pos++),
                objectId = rsSensors.getInt(pos++),
                sensorType = rsSensors.getInt(pos++),
                portNum = rsSensors.getInt(pos++),
                calcTime = rsSensors.getInt(pos++),
                minIgnore = rsSensors.getDouble(pos++),
                maxIgnore = rsSensors.getDouble(pos++),
                isAbsoluteCount = rsSensors.getInt(pos++) != 0,
                analogMinLimit = rsSensors.getDouble(pos++),
                analogMaxLimit = rsSensors.getDouble(pos++),
                smoothTime = rsSensors.getInt(pos++) * 60,
                detectIncKoef = rsSensors.getDouble(pos++),
                detectDecKoef = rsSensors.getDouble(pos++),
                boundValue = rsSensors.getDouble(pos++),
                activeValue = rsSensors.getInt(pos++),
            )
        }
        rsSensors.close()

        return alSensorInfo
    }

    private fun getSensorCalibration(sensorId: Int): Pair<List<Double>, List<Double>> {
        val alValueSensor = mutableListOf<Double>()
        val alValueData = mutableListOf<Double>()
        val rsCalib = alConn[0].executeQuery(
            """
                SELECT value_sensor , value_data
                FROM MMS_sensor_calibration
                WHERE sensor_id = $sensorId
                ORDER BY value_sensor 
            """
        )
        while (rsCalib.next()) {
            alValueSensor += rsCalib.getDouble(1)
            alValueData += rsCalib.getDouble(2)
        }
        rsCalib.close()

        return alValueSensor to alValueData
    }

    private fun startUpSensorDatas(sensorInfo: SensorInfo): Pair<List<Int>, List<AdvancedByteBuffer>> {
        checkAndCreateAggTable(sensorInfo.sensorId)

        AdvancedLogger.debug("Start work for sensorId = ${sensorInfo.sensorId}")
        val (alRawTime, alRawData) = loadAllSensorData(sensorInfo.objectId, sensorInfo.calcTime)
        //--- такого быть не должно (кроме стартового запуска), но предусмотреть не мешает
        if (alRawTime.isEmpty()) {
            AdvancedLogger.error("Empty raw data for sensorId = ${sensorInfo.sensorId}")
        }
        return alRawTime to alRawData
    }

    private fun checkAndCreateAggTable(sensorId: Int) {
        if (!MMSFunction.checkAggregationTableIsExists(alConn[0], sensorId)) {
            AdvancedLogger.info("Agg-table created for sensorId = $sensorId")
            MMSFunction.createAggTable(alConn[0], sensorId)
        }
    }

    private fun loadAllSensorData(objectId: Int, begTime: Int): Pair<List<Int>, List<AdvancedByteBuffer>> {
        //--- collect raw data
        val alRawTime = mutableListOf<Int>()
        val alRawData = mutableListOf<AdvancedByteBuffer>()

        val inRs = alConn[0].executeQuery(
            """
                SELECT ontime , sensor_data 
                FROM MMS_data_${objectId}
                WHERE ontime > $begTime
                ORDER BY ontime
                LIMIT $batchSize
            """
        )
        while (inRs.next()) {
            alRawTime.add(inRs.getInt(1))
            alRawData.add(inRs.getByteBuffer(2, ByteOrder.BIG_ENDIAN))
        }
        inRs.close()

        AdvancedLogger.debug("Loaded data rows = ${alRawTime.size}")
        return alRawTime to alRawData
    }

    private fun refreshCalcTime(sensorId: Int, calcTime: Int) {
        alConn[0].executeUpdate(
            """
                UPDATE MMS_sensor
                SET calc_time = $calcTime
                WHERE id = $sensorId 
            """
        )
    }

//--- COUNTER SENSORS PART

    private fun calcCounter(
        sensorInfo: SensorInfo,
        alValueSensor: List<Double>,
        alValueData: List<Double>,
    ) {
        val (alRawTime, alRawData) = startUpSensorDatas(sensorInfo)

        var prevSumValue = getLastAggregatedCounterValue(sensorInfo.sensorId)

        var curTime: Int? = null
        for (pos in alRawTime.indices) {
            curTime = alRawTime[pos]

            val sensorData = AbstractObjectStateCalc.getSensorData(sensorInfo.portNum, alRawData[pos])?.toDouble() ?: continue

            //--- ignore outbound values
            if (ObjectCalc.isIgnoreSensorData(sensorInfo.minIgnore, sensorInfo.maxIgnore, sensorData)) {
                continue
            }

            val sensorValue = AbstractObjectStateCalc.getSensorValue(alValueSensor, alValueData, sensorData)

            val storeValue = if (sensorInfo.isAbsoluteCount) {
                if (prevSumValue == null || sensorValue < prevSumValue) {
                    Triple(curTime, 0.0, sensorValue)
                } else {
                    Triple(curTime, sensorValue - prevSumValue, sensorValue)
                }
            } else {
                Triple(curTime, sensorValue, 0.0)
            }
            prevSumValue = sensorValue

            storeCounterData(sensorInfo.sensorId, storeValue)
        }

        curTime?.let {
            refreshCalcTime(sensorInfo.sensorId, curTime)
            AdvancedLogger.debug("Calc finished: new calc_time = ${DateTime_YMDHMS(zoneId, curTime)}")
        }
    }

    private fun getLastAggregatedCounterValue(sensorId: Int): Double? {
        val agRs = alConn[0].executeQuery(
            """
                SELECT value_2 
                FROM MMS_agg_${sensorId}
                ORDER BY ontime DESC
                LIMIT 1
            """
        )
        val result = if (agRs.next()) {
            agRs.getDouble(1)
        } else {
            null
        }
        agRs.close()

        return result
    }

    private fun storeCounterData(sensorId: Int, storeValue: Triple<Int, Double, Double>) {
        alConn[0].executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorId} (
                    ontime , value_1 , value_2
                ) VALUES (
                    ${storeValue.first} , ${storeValue.second} , ${storeValue.third}
                )
            """
        )
    }

//--- ANALOGUE SENSORS PART

    private fun calcAnalogue(
        sensorInfo: SensorInfo,
        alValueSensor: List<Double>,
        alValueData: List<Double>,
    ) {
        val (alRawTime, alRawData) = startUpSensorDatas(sensorInfo)

        var curTime: Int? = null
        for (pos in alRawTime.indices) {
            curTime = alRawTime[pos]

            val sensorData = AbstractObjectStateCalc.getSensorData(sensorInfo.portNum, alRawData[pos])?.toDouble() ?: continue

            //--- ignore outbound values
            if (ObjectCalc.isIgnoreSensorData(sensorInfo.minIgnore, sensorInfo.maxIgnore, sensorData)) {
                continue
            }

            val sensorValue = AbstractObjectStateCalc.getSensorValue(alValueSensor, alValueData, sensorData)

            if (sensorInfo.smoothTime == 0) {
                //--- get previous data
                val (prevTime, prevValue) = getPrevData(sensorInfo, curTime, sensorValue)

                val valueType = if (sensorInfo.sensorType == SensorConfig.SENSOR_LIQUID_LEVEL) {
                    getValueTypeForLiquidLevelSensors(sensorInfo, curTime, sensorValue, prevTime, prevValue)
                } else {
                    getValueTypeForAnalogueSensors(sensorInfo, curTime, sensorValue, prevTime)
                }

                storeNonAveragedAnalogueData(sensorInfo, curTime, sensorValue, valueType)
            } else {
                storeRawAnalogueData(sensorInfo, curTime, sensorValue)

                //--- may be collected many non-averaged rows early
                while (true) {
                    //--- search first non-averaged value
                    val avgOntime = getFirstNonAveragedTime(sensorInfo, curTime) ?: break

                    //--- collect values for median averaging
                    val alRawValues = getRawValuesForAveraging(sensorInfo, avgOntime)
                    alRawValues.sort()

                    val avgValue = getMedianAvgValue(alRawValues)

                    //--- get previous data
                    val (prevTime, prevValue) = getPrevData(sensorInfo, avgOntime, avgValue)

                    val valueType = if (sensorInfo.sensorType == SensorConfig.SENSOR_LIQUID_LEVEL) {
                        getValueTypeForLiquidLevelSensors(sensorInfo, avgOntime, avgValue, prevTime, prevValue)
                    } else {
                        getValueTypeForAnalogueSensors(sensorInfo, avgOntime, avgValue, prevTime)
                    }

                    storeAverageValueAndType(sensorInfo, avgOntime, avgValue, valueType)
                }
            }
        }

        curTime?.let {
            refreshCalcTime(sensorInfo.sensorId, curTime)
            AdvancedLogger.debug("Calc finished: new calc_time = ${DateTime_YMDHMS(zoneId, curTime)}")
        }
    }
    /*
        if (gldLast == null || rawTime - gldLast.x > xScale || abs(rawData - gldLast.y) > yScale || curColorIndex != gldLast.colorIndex) {
            val gd = scg?.let {
                AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
            }
            alGLD.add(GraphicLineData(rawTime, avgValue, curColorIndex, gd?.let { XyProjection.wgs_pix(gd.wgs) }))
        }
     */

    private fun getFirstNonAveragedTime(
        sensorInfo: SensorInfo,
        curTime: Int,
    ): Int? {
        val rs = alConn[0].executeQuery(
            """
                SELECT ontime
                FROM MMS_agg_${sensorInfo.sensorId}
                WHERE ontime <= ${curTime - sensorInfo.smoothTime} 
                AND value_2 IS NULL
                ORDER BY ontime ASC
                LIMIT 1
            """
        )
        val nonAvgOntime = if (rs.next()) {
            rs.getInt(1)
        } else {
            null
        }
        rs.close()

        return nonAvgOntime
    }

    private fun getRawValuesForAveraging(
        sensorInfo: SensorInfo,
        avgOntime: Int,
    ): MutableList<Double> {
        val alRawValues = mutableListOf<Double>()

        val rs = alConn[0].executeQuery(
            """
                SELECT value_1
                FROM MMS_agg_${sensorInfo.sensorId}
                WHERE ontime >= ${avgOntime - sensorInfo.smoothTime}
                AND ontime <= ${avgOntime + sensorInfo.smoothTime}  
            """
        )
        while (rs.next()) {
            alRawValues += rs.getDouble(1)
        }
        rs.close()

        return alRawValues
    }

    private fun getMedianAvgValue(alRawValues: MutableList<Double>) =
        //--- if the number of values is odd, take exactly the middle
        if (alRawValues.size % 2 != 0) {
            alRawValues[alRawValues.size / 2]
        }
        //--- otherwise the arithmetic mean between two values closest to the middle
        else {
            val val1 = alRawValues[alRawValues.size / 2 - 1]
            val val2 = alRawValues[alRawValues.size / 2]
            val1 + (val2 - val1) / 2
        }

    private fun getPrevData(
        sensorInfo: SensorInfo,
        avgOntime: Int,
        avgValue: Double,
    ): Pair<Int, Double> {
        val rs = alConn[0].executeQuery(
            """
                SELECT ontime , value_2
                FROM MMS_agg_${sensorInfo.sensorId}
                WHERE ontime < $avgOntime
                ORDER BY ontime DESC
                LIMIT 1
            """
        )
        val prevData = if (rs.next()) {
            rs.getInt(1) to rs.getDouble(2)
        } else {
            avgOntime to avgValue
        }
        rs.close()

        return prevData
    }

    private fun getValueTypeForLiquidLevelSensors(
        sensorInfo: SensorInfo,
        avgOntime: Int,
        avgValue: Double,
        prevTime: Int,
        prevValue: Double,
    ): AnalogueValueType =
        if (avgOntime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) {
            AnalogueValueType.NONE
        } else if (avgOntime == prevTime) {
            AnalogueValueType.NORMAL
        } else {
            val liquidKoef = (avgValue - prevValue) * 3600 / (avgOntime - prevTime)
            if (liquidKoef > 0 && liquidKoef > sensorInfo.detectIncKoef) {
                AnalogueValueType.ABOVE
            } else if (liquidKoef < 0 && -liquidKoef > sensorInfo.detectDecKoef) {
                AnalogueValueType.BELOW
            } else {
                AnalogueValueType.NORMAL
            }
        }

    private fun getValueTypeForAnalogueSensors(
        sensorInfo: SensorInfo,
        avgOntime: Int,
        avgValue: Double,
        prevTime: Int,
    ) =
        if (avgOntime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) {
            AnalogueValueType.NONE
        } else if (sensorInfo.analogMinLimit != sensorInfo.analogMaxLimit) {
            if (avgValue < sensorInfo.analogMinLimit) {
                AnalogueValueType.BELOW
            } else if (avgValue > sensorInfo.analogMaxLimit) {
                AnalogueValueType.ABOVE
            } else {
                AnalogueValueType.NORMAL
            }
        } else {
            AnalogueValueType.NORMAL
        }

    private fun storeNonAveragedAnalogueData(
        sensorInfo: SensorInfo,
        ontime: Int,
        value: Double,
        valueType: AnalogueValueType,
    ) {
        alConn[0].executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorInfo.sensorId} (
                    ontime , value_1 , value_2 , type_1 
                ) VALUES (
                    $ontime , $value , $value , ${valueType.value}
                )
            """
        )
    }

    private fun storeRawAnalogueData(
        sensorInfo: SensorInfo,
        ontime: Int,
        value: Double,
    ) {
        alConn[0].executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorInfo.sensorId} (
                    ontime , value_1
                ) VALUES (
                    $ontime , $value
                )
            """
        )
    }

    private fun storeAverageValueAndType(
        sensorInfo: SensorInfo,
        avgOntime: Int,
        avgValue: Double,
        valueType: AnalogueValueType,
    ) {
        alConn[0].executeUpdate(
            """
                UPDATE MMS_agg_${sensorInfo.sensorId}
                SET value_2 = $avgValue ,
                    type_1 = ${valueType.value} 
                WHERE ontime = $avgOntime
            """
        )
    }

//--- EQUIPMENT WORK SENSOR PART

    private fun calcWork(sensorInfo: SensorInfo) {
        val (alRawTime, alRawData) = startUpSensorDatas(sensorInfo)

        val (oldTime, oldValue) = getLastWorkState(sensorInfo.sensorId)

        var workBeginTime = if (oldValue == 1) {
            oldTime ?: 0
        } else {
            0
        }
        var delayBeginTime = if (oldValue == 0) {
            oldTime ?: 0
        } else {
            0
        }

        var curTime: Int? = null
        for (pos in alRawTime.indices) {
            curTime = alRawTime[pos]

            val sensorData = AbstractObjectStateCalc.getSensorData(sensorInfo.portNum, alRawData[pos])?.toDouble() ?: continue

            //--- ignore outbound values
            if (ObjectCalc.isIgnoreSensorData(sensorInfo.minIgnore, sensorInfo.maxIgnore, sensorData)) {
                continue
            }

            val sensorValue = ((sensorInfo.activeValue == 0) xor (sensorData > sensorInfo.boundValue))

            //--- still in work?
            if (sensorValue) {
                //--- record of previous downtime
                if (delayBeginTime != 0) {
                    storeWorkState(sensorInfo.sensorId, delayBeginTime, curTime, 0, delayBeginTime == oldTime)
                    delayBeginTime = 0
                }
                //--- beginning of work
                if (workBeginTime == 0) {
                    workBeginTime = curTime
                }
            } else {
                //--- record of previous work
                if (workBeginTime != 0) {
                    storeWorkState(sensorInfo.sensorId, workBeginTime, curTime, 1, workBeginTime == oldTime)
                    workBeginTime = 0
                }
                //--- start of downtime
                if (delayBeginTime == 0) {
                    delayBeginTime = curTime
                }
            }
        }
        //--- record of the last unclosed event
        curTime?.let {
            if (workBeginTime != 0) {
                storeWorkState(sensorInfo.sensorId, workBeginTime, curTime, 1, workBeginTime == oldTime)
            }
            if (delayBeginTime != 0) {
                storeWorkState(sensorInfo.sensorId, delayBeginTime, curTime, 0, delayBeginTime == oldTime)
            }
        }

        curTime?.let {
            refreshCalcTime(sensorInfo.sensorId, curTime)
            AdvancedLogger.debug("Calc finished: new calc_time = ${DateTime_YMDHMS(zoneId, curTime)}")
        }
    }

    private fun getLastWorkState(sensorId: Int): Pair<Int?, Int?> {
        val rs = alConn[0].executeQuery(
            """
                SELECT ontime , type_1
                FROM MMS_agg_${sensorId}
                ORDER BY ontime DESC
                LIMIT 1
            """
        )
        val result = if (rs.next()) {
            rs.getInt(1) to rs.getInt(2)
        } else {
            null to null
        }
        rs.close()

        return result
    }

    private fun storeWorkState(sensorId: Int, begTime: Int, endTime: Int, value: Int, forUpdate: Boolean) {
        alConn[0].executeUpdate(
            if (forUpdate) {
                """
                    UPDATE MMS_agg_${sensorId}
                    SET ontime_2 = $endTime
                    WHERE ontime = $begTime
                """
            } else {
                """
                    INSERT INTO MMS_agg_${sensorId} ( ontime , ontime_2 , type_1 )
                    VALUES ( $begTime , $endTime , $value ) 
                """
            }
        )
    }

    /*
            var prevSumValue = getLastAggregatedCounterValue(sensorInfo.sensorId)

--

                val storeValue = if (sensorInfo.isAbsoluteCount) {
                    if (prevSumValue == null || sensorValue < prevSumValue) {
                        Triple(curTime, 0.0, sensorValue)
                    } else {
                        Triple(curTime, sensorValue - prevSumValue, sensorValue)
                    }
                } else {
                    Triple(curTime, sensorValue, 0.0)
                }
                prevSumValue = sensorValue

                storeCounterData(sensorInfo.sensorId, storeValue)

--------

                if (sensorInfo.smoothTime == 0) {
                    //--- get previous non-averaged data
                    val (prevTime, prevValue) = getPrevData(sensorInfo, curTime, sensorValue)

                    val valueType = if (sensorInfo.sensorType == SensorConfig.SENSOR_LIQUID_LEVEL) {
                        getValueTypeForLiquidLevelSensors(sensorInfo, curTime, sensorValue, prevTime, prevValue)
                    } else {
                        getValueTypeForAnalogueSensors(sensorInfo, curTime, sensorValue, prevTime)
                    }

                    storeNonAveragedAnalogueData(sensorInfo, curTime, sensorValue, valueType)
                }
     */

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    class SensorInfo(
        val sensorId: Int,
        val objectId: Int,
        val sensorType: Int,
        val portNum: Int,
        val calcTime: Int,
        val minIgnore: Double,
        val maxIgnore: Double,

        // SensorConfig.SENSOR_MASS_ACCUMULATED, SensorConfig.SENSOR_VOLUME_ACCUMULATED, SensorConfig.SENSOR_LIQUID_USING,
        // SensorConfig.SENSOR_ENERGO_COUNT_AD, SensorConfig.SENSOR_ENERGO_COUNT_AR,
        // SensorConfig.SENSOR_ENERGO_COUNT_RD, SensorConfig.SENSOR_ENERGO_COUNT_RR,
        val isAbsoluteCount: Boolean,

        //SensorConfig.SENSOR_LIQUID_LEVEL, SensorConfig.SENSOR_WEIGHT,
        //SensorConfig.SENSOR_TURN, SensorConfig.SENSOR_PRESSURE,
        //SensorConfig.SENSOR_TEMPERATURE, SensorConfig.SENSOR_VOLTAGE,
        //SensorConfig.SENSOR_POWER, SensorConfig.SENSOR_DENSITY,
        //SensorConfig.SENSOR_MASS_FLOW, SensorConfig.SENSOR_VOLUME_FLOW
        //SensorConfig.SENSOR_ENERGO_VOLTAGE, SensorConfig.SENSOR_ENERGO_CURRENT,
        //SensorConfig.SENSOR_ENERGO_POWER_KOEF, SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
        //SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, SensorConfig.SENSOR_ENERGO_POWER_FULL
        val analogMinLimit: Double, // except SensorConfig.SENSOR_LIQUID_LEVEL
        val analogMaxLimit: Double, // except SensorConfig.SENSOR_LIQUID_LEVEL
        val smoothTime: Int,
        //SensorConfig.SENSOR_LIQUID_LEVEL
        val detectIncKoef: Double,
        val detectDecKoef: Double,
        //SensorConfig.SENSOR_WORK
        val boundValue: Double,
        val activeValue: Int,
    )
}

