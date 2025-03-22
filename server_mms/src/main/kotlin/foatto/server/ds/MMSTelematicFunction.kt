package foatto.server.ds

import foatto.core.model.model.xy.XyProjection
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeYMDHMSInts
import foatto.core.util.getDateTimeYMDHMSString
import foatto.server.entity.SensorEntity
import foatto.server.model.SensorConfig
import foatto.server.service.SensorService
import foatto.server.sql.CoreAdvancedConnection
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.AdvancedLogger
import foatto.server.util.getFileWriter
import kotlinx.datetime.TimeZone
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object MMSTelematicFunction {

    const val DEVICE_TYPE_GALILEO = 1
    const val DEVICE_TYPE_PULSAR_DATA = 3

    private val chmLastDayWork = ConcurrentHashMap<Int, List<Int>>()
    private val chmLastWorkShift = ConcurrentHashMap<Int, Int>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    fun fillDeviceTypeColumn(columnDeviceType: ColumnRadioButton) {
//        columnDeviceType.defaultValue = DEVICE_TYPE_GALILEO
//
//        columnDeviceType.addChoice(DEVICE_TYPE_GALILEO, "Galileo")
////            columnDeviceType.addChoice(DEVICE_TYPE_PETROLINE, "Petroline")
//        columnDeviceType.addChoice(DEVICE_TYPE_PULSAR_DATA, "Pulsar Data")
////            columnDeviceType.addChoice(DEVICE_TYPE_ARNAVI, "Arnavi")
////            columnDeviceType.addChoice(DEVICE_TYPE_ESCORT, "Escort")
////            columnDeviceType.addChoice(DEVICE_TYPE_DEL_VIDEO, "Видеорегистратор ДЭЛ-150В")
////            columnDeviceType.addChoice(DEVICE_TYPE_DEL_PULSAR, "ДЭЛ-Пульсар")
////            columnDeviceType.addChoice(DEVICE_TYPE_MIELTA, "Mielta")
////            columnDeviceType.addChoice(DEVICE_TYPE_ADM, "ADM")
////        columnDeviceType.addChoice(DEVICE_TYPE_NAVTELECOM, "NavTelecom")
//    }

    //--- пришлось делать в виде static, т.к. VideoServer не является потомком MMSHandler,
    //--- а в AbstractHandler не знает про прикладные MMS-таблицы
    fun getCommand(conn: CoreAdvancedConnection, deviceId: Int): Pair<Int, String?> {
        var cmdID = 0
        var cmdStr: String? = null
        val rs = conn.executeQuery(
            """
                 SELECT MMS_device_command_history.id , MMS_device_command.cmd 
                 FROM MMS_device_command_history , MMS_device_command 
                 WHERE MMS_device_command_history.command_id = MMS_device_command.id 
                 AND MMS_device_command_history.device_id = $deviceId 
                 AND MMS_device_command_history.for_send <> 0 
                 ORDER BY MMS_device_command_history.send_time 
             """
        )
        if (rs.next()) {
            cmdID = rs.getInt(1)
            cmdStr = rs.getString(2).trim()
        }
        rs.close()

        return Pair(cmdID, cmdStr)
    }

    fun setCommandSended(conn: CoreAdvancedConnection, cmdId: Int) {
        //--- отметим успешную отправку команды
        conn.executeUpdate(
            """ 
                UPDATE MMS_device_command_history 
                SET for_send = 0 , send_time = ${getCurrentTimeInt()} 
                WHERE id = $cmdId
            """
        )
    }

    fun addPoint(conn: CoreAdvancedConnection, deviceConfig: DeviceConfig, time: Int, bbData: AdvancedByteBuffer) {
        //--- если объект прописан, то записываем точки, иначе просто пропускаем
        if (deviceConfig.objectId != 0) {
            bbData.flip()
            conn.executeUpdate(
                """
                    INSERT INTO MMS_data_${deviceConfig.objectId} ( ontime , sensor_data ) 
                    VALUES ( $time , ${conn.getHexValue(bbData)} ) 
                """
            )
            //--- создаем новую пустую запись по суточной работе при необходимости
            checkAndCreateDayWork(conn, deviceConfig, time)
            //--- создаем новую пустую запись по рабочей смене при необходимости
            if (deviceConfig.isAutoWorkShift) {
                checkAndCreateWorkShift(conn, deviceConfig, time)
            }
        }
    }

    private fun checkAndCreateDayWork(conn: CoreAdvancedConnection, deviceConfig: DeviceConfig, time: Int) {
        val arrLastDT = chmLastDayWork[deviceConfig.objectId]
        val arrDT = getDateTimeYMDHMSInts(deviceConfig.timeZone, time)
        //--- создаем новую пустую запись по дневной работе при необходимости
        if (arrLastDT == null || arrLastDT[0] != arrDT[0] || arrLastDT[1] != arrDT[1] || arrLastDT[2] != arrDT[2]) {
            //--- создадим пустую запись по дневной работе, если ее не было
            val rsADR = conn.executeQuery(
                """
                    SELECT id 
                    FROM MMS_day_work 
                    WHERE object_id = ${deviceConfig.objectId} 
                    AND ye = ${arrDT[0]} 
                    AND mo = ${arrDT[1]} 
                    AND da = ${arrDT[2]}
                """
            )
            val isExist = rsADR.next()
            rsADR.close()

            if (!isExist) {
                conn.executeUpdate(
                    """
                        INSERT INTO MMS_day_work ( id , user_id , object_id , ye , mo , da ) VALUES ( 
                        ${conn.getNextIntId("MMS_day_work", "id")} , ${deviceConfig.userId} , ${deviceConfig.objectId} , ${arrDT[0]} , ${arrDT[1]} , ${arrDT[2]} )
                    """
                )
            }
            chmLastDayWork[deviceConfig.objectId] = arrDT
        }
    }

    private fun checkAndCreateWorkShift(conn: CoreAdvancedConnection, deviceConfig: DeviceConfig, time: Int) {
        var lastTime: Int? = chmLastWorkShift[deviceConfig.objectId]
        if (lastTime == null || lastTime < time) {
            lastTime = autoCreateWorkShift(conn, deviceConfig.userId, deviceConfig.objectId)
            //--- создать не удалось - нет стартового шаблона - обнулим флаг автосоздания
            if (lastTime == null) {
                //--- практически невозможная ситуация - включенный флаг автосоздания рабочих смен
                //--- при отсутствии самих рабочих смен - поэтому достаточно выключить в локальных настройках,
                //--- этого хватит для продолжения нормальной работы
                //sqlBatch.add( new StringBuilder(
                //    " UPDATE MMS_object SET is_auto_work_shift = 0 WHERE id = " ).append( deviceConfig.objectId ) );
                deviceConfig.isAutoWorkShift = false
            }
            chmLastWorkShift[deviceConfig.objectId] = lastTime!!
        }
    }

    fun writeSession(
        conn: CoreAdvancedConnection,
        dirSessionLog: File,
        timeZone: TimeZone,
        deviceConfig: DeviceConfig,
        fwVersion: String,
        begTime: Int,
        address: String,
        status: String,
        errorText: String,
        dataCount: Int,
        firstPointTime: Int,
        lastPointTime: Int,
        isOk: Boolean = true,
    ) {
        //--- какое д.б. имя лог-файла для текущего дня и часа
        val logTime = getDateTimeYMDHMSString(timeZone, getCurrentTimeInt())
        val curLogFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

        var text = "$logTime $address Длительность [сек]: ${getCurrentTimeInt() - begTime} Точек записано: $dataCount "
        if (dataCount > 0) {
            text += " Время первой точки: ${getDateTimeYMDHMSString(timeZone, firstPointTime)} Время последней точки: ${getDateTimeYMDHMSString(timeZone, lastPointTime)} "
        }
        text += " Статус: $status "
        if (isOk || errorText.isEmpty()) {
        } else {
            text += " Ошибка: $errorText "
        }

        val dirDeviceSessionLog = File(dirSessionLog, "device/${deviceConfig.deviceId}")
        dirDeviceSessionLog.mkdirs()
        var out = getFileWriter(File(dirDeviceSessionLog, curLogFileName), true)
        out.write(text)
        out.newLine()
        out.flush()
        out.close()

        val dirObjectSessionLog = File(dirSessionLog, "object/${deviceConfig.objectId}")
        dirObjectSessionLog.mkdirs()
        out = getFileWriter(File(dirObjectSessionLog, curLogFileName), true)
        out.write(text)
        out.newLine()
        out.flush()
        out.close()

        conn.executeUpdate(
            """
                UPDATE MMS_device SET 
                fw_version = '$fwVersion' , 
                last_session_time = ${getCurrentTimeInt()} , 
                last_session_status = '$status' , 
                last_session_error = '${if (isOk || errorText.isEmpty()) "" else errorText}' 
                WHERE id = '${deviceConfig.deviceId}'
            """
        )

        conn.commit()
    }

    fun writeError(
        conn: CoreAdvancedConnection,
        dirSessionLog: File,
        timeZone: TimeZone,
        deviceConfig: DeviceConfig?,
        fwVersion: String,
        begTime: Int,
        address: String,
        status: String,
        errorText: String,
        dataCount: Int,
        firstPointTime: Int,
        lastPointTime: Int,
    ) {
        deviceConfig?.let {
            writeSession(
                conn = conn,
                dirSessionLog = dirSessionLog,
                timeZone = timeZone,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = address,
                status = status + "Error;",
                errorText = errorText,
                dataCount = dataCount,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
                isOk = false,
            )
        }
        AdvancedLogger.error(errorText)
    }

    fun autoCreateWorkShift(conn: CoreAdvancedConnection, userId: Int, objectId: Int): Int? {
        //--- найдем последнюю рабочую смену
        var begTime = 0
        var endTime = 0
        val rs = conn.executeQuery(" SELECT beg_dt , end_dt FROM MMS_work_shift WHERE object_id = $objectId ORDER BY end_dt DESC ")
        if (rs.next()) {
            begTime = rs.getInt(1)
            endTime = rs.getInt(2)
        }
        rs.close()
        //--- нашлась такая смена
        if (begTime != 0 && endTime != 0) {
            val workShiftDuration = endTime - begTime
            //--- автоматически создаём смены до настоящего времени
            while (endTime <= getCurrentTimeInt()) {
                begTime = endTime
                endTime += workShiftDuration

                conn.executeUpdate(
                    StringBuilder(
                        " INSERT INTO MMS_work_shift ( "
                    )
                        .append(" id , user_id , object_id , beg_dt , end_dt , beg_dt_fact , end_dt_fact , worker_id , shift_no , run ) VALUES ( ")
                        .append(conn.getNextIntId("MMS_work_shift", "id")).append(" , ")
                        .append(userId).append(" , ").append(objectId).append(" , ")
                        //--- при автосоздании примем фактическое время == документальному времени
                        .append(begTime).append(" , ").append(endTime).append(" , ")
                        .append(begTime).append(" , ").append(endTime).append(" , ")
                        .append(" 0 , '' , 0 ) ").toString()
                )
            }
            return endTime
        } else {
            return null
        }
    }

    fun saveSensorData(
        conn: CoreAdvancedConnection,
        deviceIndex: Int,
        sensorConfigs: Map<Int, List<SensorEntity>>,               // portNum to sensorEntities
        sensorCalibrations: Map<Int, List<Pair<Double, Double>>>,  // sensorId to list of <sensorValue to dataValue> pairs
        pointTime: Int,
        tmSensorData: SortedMap<Int, Int>,
        startPortNum: Int,
        sensorDataSize: Int,
        bbData: AdvancedByteBuffer,
    ) {
        //--- write raw data into MMS_data_XXX
        CoreTelematicFunction.putDigitalSensors(deviceIndex, tmSensorData, startPortNum, sensorDataSize, bbData)

        //--- write smoothed/aggregated data into MMS_agg_YYY
        tmSensorData.forEach { (index, sensorValue) ->
            val portNum = deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + startPortNum + index
            saveSensorData(
                conn = conn,
                sensorConfigs = sensorConfigs,
                sensorCalibrations = sensorCalibrations,
                pointTime = pointTime,
                portNum = portNum,
                sensorValue = sensorValue.toDouble(),
            )
        }
    }

    fun saveSensorData(
        conn: CoreAdvancedConnection,
        deviceIndex: Int,
        sensorConfigs: Map<Int, List<SensorEntity>>,               // portNum to sensorEntities
        sensorCalibrations: Map<Int, List<Pair<Double, Double>>>,  // sensorId to list of <sensorValue to dataValue> pairs
        pointTime: Int,
        tmSensorData: SortedMap<Int, Double>,
        startPortNum: Int,
        bbData: AdvancedByteBuffer,
    ) {
        //--- write raw data into MMS_data_XXX
        CoreTelematicFunction.putDigitalSensors(deviceIndex, tmSensorData, startPortNum, bbData)

        //--- write smoothed/aggregated data into MMS_agg_YYY
        tmSensorData.forEach { (index, sensorValue) ->
            val portNum = deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + startPortNum + index
            saveSensorData(
                conn = conn,
                sensorConfigs = sensorConfigs,
                sensorCalibrations = sensorCalibrations,
                pointTime = pointTime,
                portNum = portNum,
                sensorValue = sensorValue,
            )
        }
    }

    fun saveSensorData(
        conn: CoreAdvancedConnection,
        sensorConfigs: Map<Int, List<SensorEntity>>,               // portNum to sensorEntities
        sensorCalibrations: Map<Int, List<Pair<Double, Double>>>,  // sensorId to list of <sensorValue to dataValue> pairs
        pointTime: Int,
        portNum: Int,
        sensorValue: Double,
    ) {
        //--- на одном порту может быть несколько разнотипных датчиков,
        //--- например, датчик напряжения и датчик работы оборудования (работающий по величине этого напряжения)
        sensorConfigs[portNum]?.forEach { sensorEntity ->
            when (sensorEntity.sensorType) {
                in SensorConfig.analogueSensorTypes -> {
                    SensorService.checkAndCreateAggTable(conn, sensorEntity.id)
                    saveAnalogueSensorData(
                        conn = conn,
                        sensorEntity = sensorEntity,
                        sensorCalibration = sensorCalibrations[sensorEntity.id] ?: emptyList(),
                        pointTime = pointTime,
                        sensorValue = sensorValue,
                    )
                }

                in SensorConfig.counterSensorTypes -> {
                    SensorService.checkAndCreateAggTable(conn, sensorEntity.id)
                    saveCounterSensorData(
                        conn = conn,
                        sensorEntity = sensorEntity,
                        sensorCalibration = sensorCalibrations[sensorEntity.id] ?: emptyList(),
                        pointTime = pointTime,
                        sensorValue = sensorValue,
                    )
                }

                SensorConfig.SENSOR_WORK -> {
                    SensorService.checkAndCreateAggTable(conn, sensorEntity.id)
                    saveWorkSensorData(
                        conn = conn,
                        sensorEntity = sensorEntity,
                        pointTime = pointTime,
                        sensorValue = sensorValue,
                    )
                }
            }
        }
    }

    fun saveGeoSensorData(
        conn: CoreAdvancedConnection,
        sensorEntity: SensorEntity,
        pointTime: Int,
        wgsX: Double,
        wgsY: Double,
        speed: Int,
        absoluteRun: Double?,
    ) {
        SensorService.checkAndCreateAggTable(conn, sensorEntity.id)

        var relativeRun = 0.0

        val rs = conn.executeQuery(
            """
                SELECT value_0 , value_1 , value_2
                FROM MMS_agg_${sensorEntity.id}
                WHERE ontime_0 = ( SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} ) 
            """
        )
        if (rs.next()) {
            val prevWgsX = rs.getDouble(1)
            val prevWgsY = rs.getDouble(2)
            val prevAbsoluteRun = rs.getDouble(3)

            relativeRun = absoluteRun?.let {
                max(absoluteRun - prevAbsoluteRun, 0.0)
            } ?: XyProjection.distance(prevWgsX, prevWgsY, wgsX, wgsY)
        }
        rs.close()

        relativeRun *= sensorEntity.runKoef ?: 1.0

        conn.executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorEntity.id} ( ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3 ) 
                VALUES ( $pointTime , $pointTime , $speed , $wgsX , $wgsY , $absoluteRun , $relativeRun )  
            """
        )
    }

    private fun saveAnalogueSensorData(
        conn: CoreAdvancedConnection,
        sensorEntity: SensorEntity,
        sensorCalibration: List<Pair<Double, Double>>,
        pointTime: Int,
        sensorValue: Double,
    ) {
        //--- ignore outbound values
        if (isIgnoreSensorData(sensorEntity.minIgnore, sensorEntity.maxIgnore, sensorValue)) {
            return
        }
        val dataValue = getDataValue(sensorCalibration, sensorValue)

        val smoothPeriod = sensorEntity.smoothTime ?: 0
        val smoothValue = if (smoothPeriod > 0) {
            val values = mutableListOf(dataValue)
            val rs = conn.executeQuery(
                """
                    SELECT value_0
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 >= ${pointTime - smoothPeriod * 60}
                """
            )
            while (rs.next()) {
                values += rs.getDouble(1)
            }
            rs.close()

            values.sort()

            //--- if the number of values is odd, take exactly the middle
            if (values.size % 2 != 0) {
                values[values.size / 2]
            }
            //--- otherwise the arithmetic mean between two values closest to the middle
            else {
                val val1 = values[values.size / 2 - 1]
                val val2 = values[values.size / 2]
                val1 + (val2 - val1) / 2
            }
        } else {
            dataValue
        }

//        val (valueType, incDecValue) = if (sensorInfo.sensorType == SensorConfig.SENSOR_LIQUID_LEVEL) {
//            getValueTypeForLiquidLevelSensors(sensorInfo, avgOntime, avgValue, prevTime, prevValue)
//        } else {
//            getValueTypeForAnalogueSensors(sensorInfo, avgOntime, avgValue, prevTime)
//        }

        conn.executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorEntity.id} ( ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3 ) 
                VALUES ( $pointTime , $pointTime , 0 , $dataValue , $smoothValue , 0 , 0 )  
            """
        )
    }

    private fun saveCounterSensorData(
        conn: CoreAdvancedConnection,
        sensorEntity: SensorEntity,
        sensorCalibration: List<Pair<Double, Double>>,
        pointTime: Int,
        sensorValue: Double,
    ) {
        //--- ignore outbound values
        if (isIgnoreSensorData(sensorEntity.minIgnore, sensorEntity.maxIgnore, sensorValue)) {
            return
        }
        val dataValue = getDataValue(sensorCalibration, sensorValue)

        var prevValue = 0.0
        if (sensorEntity.isAbsoluteCount == true) {
            val rs = conn.executeQuery(
                """
                    SELECT value_0
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 = ( SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} ) 
                """
            )
            if (rs.next()) {
                prevValue = rs.getDouble(1)
            }
            rs.close()
        }

        var deltaValue = dataValue - prevValue
        //--- учитываем возможный/периодический сброс абсолютного счётчика
        if (deltaValue < 0) {
            deltaValue = 0.0
        }

        var rs = conn.executeQuery(
            """
                SELECT SUM(value_1)
                FROM MMS_agg_${sensorEntity.id}
                WHERE ontime_0 > ${pointTime - 60}
            """
        )
        val lastMinuteSum = if (rs.next()) {
            rs.getDouble(1)
        } else {
            0.0
        }
        rs.close()

        rs = conn.executeQuery(
            """
                SELECT SUM(value_1)
                FROM MMS_agg_${sensorEntity.id}
                WHERE ontime_0 > ${pointTime - 3600}
            """
        )
        val lastHourSum = if (rs.next()) {
            rs.getDouble(1)
        } else {
            0.0
        }
        rs.close()

        conn.executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorEntity.id} ( ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3 ) 
                VALUES ( $pointTime , $pointTime , 0 , $dataValue , $deltaValue , ${lastMinuteSum + deltaValue} , ${lastHourSum + deltaValue} )  
            """
        )
    }

    private fun saveWorkSensorData(
        conn: CoreAdvancedConnection,
        sensorEntity: SensorEntity,
        pointTime: Int,
        sensorValue: Double,
    ) {
        //--- ignore outbound values
        if (isIgnoreSensorData(sensorEntity.minIgnore, sensorEntity.maxIgnore, sensorValue)) {
            return
        }

        val workValue: Boolean = sensorEntity.boundValue?.let { boundValue ->
            sensorEntity.activeValue?.let { activeValue ->
                if (activeValue != 0) {
                    sensorValue > boundValue
                } else {
                    sensorValue < boundValue
                }
            } ?: false
        } ?: false

        val rs = conn.executeQuery(
            """
                SELECT ontime_0 , ontime_1 , type_0
                FROM MMS_agg_${sensorEntity.id}
                WHERE ontime_0 = ( SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} ) 
            """
        )
        val (onTime0, onTime1, prevValue) = if (rs.next()) {
            Triple(rs.getInt(1), rs.getInt(2), rs.getInt(3) != 0)
        } else {
            Triple(0, 0, null)
        }
        rs.close()

        prevValue?.let {
            if (workValue == prevValue) {
                conn.executeUpdate(
                    """
                        UPDATE MMS_agg_${sensorEntity.id} 
                        SET ontime_1 = $pointTime 
                        WHERE ontime_0 = $onTime0 
                    """
                )
                return
            } else {
                //--- слишком короткий предыдущий период
                if (
                    !prevValue && (onTime1 - onTime0 < (sensorEntity.minOffTime ?: 0)) ||
                    prevValue && (onTime1 - onTime0 < (sensorEntity.minOnTime ?: 0))
                ) {
                    conn.executeUpdate(
                        """
                            DELETE FROM MMS_agg_${sensorEntity.id} 
                            WHERE ontime_0 = $onTime0 
                        """
                    )
                }
            }
        }
        conn.executeUpdate(
            """
                INSERT INTO MMS_agg_${sensorEntity.id} ( ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3 ) 
                VALUES ( $pointTime , $pointTime , ${if (workValue) 1 else 0} , 0 , 0 , 0 , 0 )  
            """
        )
    }

    /*
        //--- особые величины - сигнал с устройства
        const val SENSOR_SIGNAL = -2   // Есть/нет

        //--- составной датчик - гео-данных ( координаты,скорость,пробег )
        const val SENSOR_GEO = -1           // координаты, км/ч, м или км

        //--- сигнальная величина - состояние расходомера/счётчика
        const val SENSOR_LIQUID_USING_COUNTER_STATE = 17
*/

    //--- define sensor data ignoring
    private fun isIgnoreSensorData(minIgnore: Double?, maxIgnore: Double?, sensorData: Double?): Boolean =
        sensorData?.let {
            minIgnore?.let {
                maxIgnore?.let {
                    //--- classic variant: if minIgnore < maxIgnore, then ignore below minIgnore or above maxIgnore
                    if (minIgnore < maxIgnore) {
                        sensorData < minIgnore || sensorData > maxIgnore
                    } else {
                        //--- alternative: if minIgnore >= maxIgnore, then ignore between minIgnore and maxIgnore,
                        //--- given the case, if minIgnore == maxIgnore, then ignore nothing
                        sensorData < minIgnore && sensorData > maxIgnore
                    }
                } ?: run {
                    sensorData < minIgnore
                }
            } ?: run {
                maxIgnore?.let {
                    sensorData > maxIgnore
                } ?: false
            }
        } ?: true

    //--- calculation of the measured value by linear approximation
    private fun getDataValue(sensorCalibration: List<Pair<Double, Double>>, sensorValue: Double): Double {
        var pos = -1

        if (sensorCalibration.isEmpty()) {
            return sensorValue
        }
        //--- if only one value is specified, use it as a usual "transforming" multiplier
        //--- (that is, it is a special case of two values, one of which is "0-> 0")
        if (sensorCalibration.size == 1) {
            val calibrationSensorValue = sensorCalibration[0].first
            val calibrationDataValue = sensorCalibration[0].second
            //--- meaningless one-to-one calibration or reduction to 0-th ADC, we can get division by 0, skip
            return if (calibrationSensorValue == calibrationDataValue || calibrationSensorValue == 0.0) {
                sensorValue
            } else {
                sensorValue / calibrationSensorValue * calibrationDataValue
            }
        }

        if (sensorValue < sensorCalibration[0].first) {
            pos = 0
        } else if (sensorValue > sensorCalibration[sensorCalibration.lastIndex].first) {
            pos = sensorCalibration.lastIndex - 1
        } else {
            for (i in 0 until sensorCalibration.size - 1) {
                if (sensorValue >= sensorCalibration[i].first && sensorValue <= sensorCalibration[i + 1].first) {
                    pos = i
                    break
                }
            }
        }

        return (sensorValue - sensorCalibration[pos].first) /
            (sensorCalibration[pos + 1].first - sensorCalibration[pos].first) *
            (sensorCalibration[pos + 1].second - sensorCalibration[pos].second) +
            sensorCalibration[pos].second
    }

}