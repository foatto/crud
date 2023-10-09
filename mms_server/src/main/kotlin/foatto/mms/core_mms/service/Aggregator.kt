package foatto.mms.core_mms.service

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core_server.service.CoreServiceWorker
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.service.Replicator
import foatto.sql.AdvancedConnection
import java.io.IOException
import java.nio.ByteOrder
import kotlin.system.exitProcess

class Aggregator(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "Aggregator"
                if (args.size == 1) {
                    Replicator(args[0]).run()
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
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val isRunOnce: Boolean = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    private val alDestName = mutableListOf<String>()
//    private val alDestProtocol = mutableListOf<String>()
//    private val alDestIP = mutableListOf<String>()
//    private val alDestPort = mutableListOf<Int>()
//    private val alDestRole = mutableListOf<Int?>()

//    private var cyclePause: Long = 0

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

//        val destName = hmConfig[CoreReplicator.CONFIG_DEST_NAME_ + index] ?: break
//
//        alDestName.add(destName)
//        alDestProtocol += hmConfig[CoreReplicator.CONFIG_DEST_PROTOCOL_ + index] ?: "HTTP"
//        alDestIP += hmConfig[CoreReplicator.CONFIG_DEST_IP_ + index]!!
//        alDestPort += hmConfig[CoreReplicator.CONFIG_DEST_PORT_ + index]!!.toInt()
//        //--- если роль репликатора не указана явно, то он выполняет обе роли - и отправителя и получателя
//        alDestRole += hmConfig[CoreReplicator.CONFIG_DEST_ROLE_ + index]?.toInt()
//
//        cyclePause = hmConfig[CoreReplicator.CONFIG_CYCLE_PAUSE]!!.toLong() * 1000
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun cycle() {
        while (true) {
            try {
                val alSensorCalcInfo = getSensorCalcInfos()
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
                    AdvancedLogger.debug("Calc finished for sensorId = ${sensorCalcInfo.sensorId}")
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
    }

    private fun getSensorCalcInfos(): List<SensorCalcInfo> {
        val alSensorCalcInfo = mutableListOf<SensorCalcInfo>()
        val rsSensors = alConn[0].executeQuery(
            """
                SELECT id , object_id , sensor_type , port_num , calc_time , ignore_min_sensor , ignore_max_sensor ,  
                    is_absolute_count , 
                    ( raw_time - calc_time ) AS diff_time
                FROM MMS_sensor
                WHERE sensor_type IN ( ${alPreparedSensorTypes.joinToString(" , ")} )
                AND diff_time > 0 
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
                SELECT ontime , sensor_data FROM MMS_data_${objectId}
                WHERE ontime > $begTime 
                ORDER BY ontime 
            """
//                " WHERE ontime >= ${begTime - maxSmoothTime} AND ontime <= ${endTime + maxSmoothTime} " +
        )
        while (inRs.next()) {
            alRawTime.add(inRs.getInt(1))
            alRawData.add(inRs.getByteBuffer(2, ByteOrder.BIG_ENDIAN))
        }
        inRs.close()

        return alRawTime to alRawData
    }

    private fun storeCounterData(sensorId: Int, storeValue: Triple<Int, Double, Double>) {
        alConn[0].executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorId} (
                    ontime, value_1, value_2
                ) VALUE (
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
                WHERE sensor_id = $sensorId 
            """
        )
    }

//--- COUNTER PART ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun calcCounter(
        sensorCalcInfo: SensorCalcInfo,
        alValueSensor: List<Double>,
        alValueData: List<Double>,
    ) {
        val (alRawTime, alRawData) = loadAllSensorData(sensorCalcInfo.objectId, sensorCalcInfo.calcTime)
        //--- такого быть не должно, но предусмотреть не мешает
        if (alRawTime.isEmpty()) {
            AdvancedLogger.error("Empty raw data for sensorId = '${sensorCalcInfo.sensorId}'")
            return
        }
        var prevSumValue = getLastAggregatedCounterValue(sensorCalcInfo.sensorId)

        var curTime: Int? = null
        for (pos in alRawTime.indices) {
            val sensorData = AbstractObjectStateCalc.getSensorData(sensorCalcInfo.portNum, alRawData[pos])?.toDouble() ?: continue
            //--- ignore outbound values
            if (ObjectCalc.isIgnoreSensorData(sensorCalcInfo.minIgnore, sensorCalcInfo.maxIgnore, sensorData)) {
                continue
            }

            curTime = alRawTime[pos]
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
            agRs.getDouble(2)
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

