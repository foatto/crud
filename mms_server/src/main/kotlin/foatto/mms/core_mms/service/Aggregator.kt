package foatto.mms.core_mms.service

import foatto.util.AdvancedByteBuffer
import foatto.util.AdvancedLogger
import foatto.util.DateTime_YMDHMS
import foatto.core_server.service.CoreServiceWorker
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.sql.AdvancedConnection
import java.io.IOException
import java.nio.ByteOrder
import java.time.ZoneId
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
                } else println("Usage: $serviceWorkerName <ini-file-name>")
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        private val alPreparedSensorTypes = listOf(
            SensorConfig.SENSOR_MASS_ACCUMULATED,
            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
            SensorConfig.SENSOR_LIQUID_USING,
            SensorConfig.SENSOR_ENERGO_COUNT_AD,
            SensorConfig.SENSOR_ENERGO_COUNT_AR,
            SensorConfig.SENSOR_ENERGO_COUNT_RD,
            SensorConfig.SENSOR_ENERGO_COUNT_RR,
        )

        private val BATCH_SIZE = "batch_size"
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

            val alSensorCalcInfo = getSensorCalcInfos()
            AdvancedLogger.debug("--- Sensors count = ${alSensorCalcInfo.size}")
            alSensorCalcInfo.firstOrNull()?.let { sensorCalcInfo ->
                AdvancedLogger.debug("- First calc_time = ${DateTime_YMDHMS(zoneId, sensorCalcInfo.calcTime)}")
            }

            for (sensorCalcInfo in alSensorCalcInfo) {
                val (alValueSensor, alValueData) = getSensorCalibration(sensorCalcInfo.sensorId)

                when (sensorCalcInfo.sensorType) {
                    SensorConfig.SENSOR_MASS_ACCUMULATED,
                    SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                    SensorConfig.SENSOR_LIQUID_USING,
                    SensorConfig.SENSOR_ENERGO_COUNT_AD,
                    SensorConfig.SENSOR_ENERGO_COUNT_AR,
                    SensorConfig.SENSOR_ENERGO_COUNT_RD,
                    SensorConfig.SENSOR_ENERGO_COUNT_RR,
                    -> {
                        calcCounter(
                            sensorCalcInfo = sensorCalcInfo,
                            alValueSensor = alValueSensor,
                            alValueData = alValueData,
                        )
                    }

                    else -> {
                        AdvancedLogger.error("Unknown sensor type = ${sensorCalcInfo.sensorType}")
                    }
                }
                alConn[0].commit()
                AdvancedLogger.debug("Calc finished for sensorId = ${sensorCalcInfo.sensorId} for old calc_time = ${DateTime_YMDHMS(zoneId, sensorCalcInfo.calcTime)}")

                if (alSensorCalcInfo.isEmpty()) {
                    AdvancedLogger.debug("SLEEP")
                    Thread.sleep(60_000)
                }
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
                WHERE raw_time = 0 AND calc_time <> 0 AND object_id <> 0 
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
                    SELECT COUNT(*), MAX(ontime)  
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

    private fun getSensorCalcInfos(): List<SensorCalcInfo> {
        val alSensorCalcInfo = mutableListOf<SensorCalcInfo>()
        val rsSensors = alConn[0].executeQuery(
            """
                SELECT id , object_id , sensor_type , port_num , calc_time , ignore_min_sensor , ignore_max_sensor , is_absolute_count, 
                    CAST(raw_time AS BIGINT) - CAST(calc_time AS BIGINT) AS diff_time
                FROM MMS_sensor
                WHERE sensor_type IN ( ${alPreparedSensorTypes.joinToString(" , ")} )
                AND CAST(raw_time AS BIGINT) - CAST(calc_time AS BIGINT) > 0 
                ORDER BY diff_time DESC
            """
        )
        while (rsSensors.next()) {
            var pos = 1
            alSensorCalcInfo += SensorCalcInfo(
                sensorId = rsSensors.getInt(pos++),
                objectId = rsSensors.getInt(pos++),
                sensorType = rsSensors.getInt(pos++),
                portNum = rsSensors.getInt(pos++),
                calcTime = rsSensors.getInt(pos++),
                minIgnore = rsSensors.getDouble(pos++),
                maxIgnore = rsSensors.getDouble(pos++),
                //--- SENSOR_MASS_ACCUMULATED, SENSOR_VOLUME_ACCUMULATED, SENSOR_LIQUID_USING,
                //--- SENSOR_ENERGO_COUNT_AD, SENSOR_ENERGO_COUNT_AR, SENSOR_ENERGO_COUNT_RD, SENSOR_ENERGO_COUNT_RR,
                isAbsoluteCount = rsSensors.getInt(pos++) != 0,
            )
        }
        rsSensors.close()

        return alSensorCalcInfo
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

    private fun loadAllSensorData(objectId: Int, begTime: Int): Pair<List<Int>, List<AdvancedByteBuffer>> {
//        var maxSmoothTime = 0
//        oc.hmSensorConfig.values.forEach { hmSC ->
//            hmSC.values.forEach { sensorConfig ->
//                if (sensorConfig is SensorConfigAnalogue) {
//                    maxSmoothTime = max(maxSmoothTime, sensorConfig.smoothTime)
//                }
//            }
//        }

        //--- collect raw data
        val alRawTime = mutableListOf<Int>()
        val alRawData = mutableListOf<AdvancedByteBuffer>()

        val inRs = alConn[0].executeQuery(
            """
                SELECT ${alConn[0].getPreLimit(batchSize)} 
                    ontime , sensor_data FROM MMS_data_${objectId}
                WHERE ontime > $begTime
                ${alConn[0].getMidLimit(batchSize)}
                ORDER BY ontime
                ${alConn[0].getPostLimit(batchSize)}
            """
//                " WHERE ontime >= ${begTime - maxSmoothTime} AND ontime <= ${endTime + maxSmoothTime} " +
        )
        while (inRs.next()) {
            alRawTime.add(inRs.getInt(1))
            alRawData.add(inRs.getByteBuffer(2, ByteOrder.BIG_ENDIAN))
        }
        inRs.close()

        AdvancedLogger.debug("Loaded data rows = ${alRawTime.size}")
        return alRawTime to alRawData
    }

    private fun storeCounterData(sensorId: Int, storeValue: Triple<Int, Double, Double>) {
        alConn[0].executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorId} (
                    ontime, value_1, value_2
                ) VALUES (
                    ${storeValue.first} , ${storeValue.second} , ${storeValue.third}
                )
            """
        )
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

//--- COUNTER PART ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun calcCounter(
        sensorCalcInfo: SensorCalcInfo,
        alValueSensor: List<Double>,
        alValueData: List<Double>,
    ) {
        if (!MMSFunction.checkAggregationTableIsExists(alConn[0], sensorCalcInfo.sensorId)) {
            AdvancedLogger.info("Agg-table created for sensorId = ${sensorCalcInfo.sensorId}")
            MMSFunction.createAggTable(alConn[0], sensorCalcInfo.sensorId)
        }

        val (alRawTime, alRawData) = loadAllSensorData(sensorCalcInfo.objectId, sensorCalcInfo.calcTime)
        //--- такого быть не должно (кроме стартового запуска), но предусмотреть не мешает
        if (alRawTime.isEmpty()) {
            AdvancedLogger.error("Empty raw data for sensorId = ${sensorCalcInfo.sensorId}")
        }
        var prevSumValue = getLastAggregatedCounterValue(sensorCalcInfo.sensorId)

        var curTime: Int? = null
        for (pos in alRawTime.indices) {
            curTime = alRawTime[pos]

            val sensorData = AbstractObjectStateCalc.getSensorData(sensorCalcInfo.portNum, alRawData[pos])?.toDouble() ?: continue

            //--- ignore outbound values
            if (ObjectCalc.isIgnoreSensorData(sensorCalcInfo.minIgnore, sensorCalcInfo.maxIgnore, sensorData)) {
                continue
            }

            val sensorValue = AbstractObjectStateCalc.getSensorValue(alValueSensor, alValueData, sensorData)

            val storeValue = if (sensorCalcInfo.isAbsoluteCount) {
                if (prevSumValue == null || sensorValue < prevSumValue) {
                    Triple(curTime, 0.0, sensorValue)
                } else {
                    Triple(curTime, sensorValue - prevSumValue, sensorValue)
                }
            } else {
                Triple(curTime, sensorValue, 0.0)
            }
            prevSumValue = sensorValue

            storeCounterData(sensorCalcInfo.sensorId, storeValue)
        }

        curTime?.let {
            refreshCalcTime(sensorCalcInfo.sensorId, curTime)
            AdvancedLogger.debug("Calc finished for sensorId = ${sensorCalcInfo.sensorId} and new calc_time = ${DateTime_YMDHMS(zoneId, curTime)}")
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

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private class SensorCalcInfo(
        val sensorId: Int,
        val objectId: Int,
        val sensorType: Int,
        val portNum: Int,
        val calcTime: Int,
        val minIgnore: Double,
        val maxIgnore: Double,
        //--- SENSOR_MASS_ACCUMULATED, SENSOR_VOLUME_ACCUMULATED, SENSOR_LIQUID_USING
        val isAbsoluteCount: Boolean,
    )
}

