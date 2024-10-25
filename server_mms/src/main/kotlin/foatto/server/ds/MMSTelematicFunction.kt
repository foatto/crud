package foatto.server.ds

import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeYMDHMSList
import foatto.core.util.getDateTimeYMDHMSString
import foatto.server.sql.CoreAdvancedConnection
import foatto.server.sql.SQLBatch
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.AdvancedLogger
import foatto.server.util.getFileWriter
import kotlinx.datetime.TimeZone
import java.io.File
import java.util.concurrent.ConcurrentHashMap

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

    fun addPoint(conn: CoreAdvancedConnection, deviceConfig: DeviceConfig, time: Int, bbData: AdvancedByteBuffer, sqlBatchData: SQLBatch) {
        //--- если объект прописан, то записываем точки, иначе просто пропускаем
        if (deviceConfig.objectId != 0) {
            bbData.flip()
            sqlBatchData.add(
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
        val arrDT = getDateTimeYMDHMSList(deviceConfig.timeZone, time)
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
        dataCountAll: Int,
        firstPointTime: Int,
        lastPointTime: Int,
        isOk: Boolean = true,
    ) {
        //--- какое д.б. имя лог-файла для текущего дня и часа
        val logTime = getDateTimeYMDHMSString(timeZone, getCurrentTimeInt())
        val curLogFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

        var text = "$logTime $address Длительность [сек]: ${getCurrentTimeInt() - begTime} Точек записано: $dataCount из $dataCountAll "
        if (dataCountAll > 0) {
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
        dataCountAll: Int,
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
                dataCountAll = dataCountAll,
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

}