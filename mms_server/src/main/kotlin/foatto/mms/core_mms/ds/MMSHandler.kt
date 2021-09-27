package foatto.mms.core_mms.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeArray
import foatto.core.util.getFileWriter
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.ds.AbstractTelematicHandler
import foatto.core_server.ds.CoreDataWorker
import foatto.mms.core_mms.cWorkShift
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import foatto.sql.SQLBatch
import java.io.File
import java.nio.channels.SocketChannel
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

abstract class MMSHandler : AbstractTelematicHandler() {

    companion object {

        const val DEVICE_ID_DIVIDER = 10_000_000

        const val DEVICE_TYPE_GALILEO = 1
        const val DEVICE_TYPE_PETROLINE = 2
        const val DEVICE_TYPE_PULSAR_DATA = 3
//        const val DEVICE_TYPE_ARNAVI = 4
//        const val DEVICE_TYPE_ESCORT = 5
//        const val DEVICE_TYPE_DEL_VIDEO = 6
//        const val DEVICE_TYPE_DEL_PULSAR = 7
//        const val DEVICE_TYPE_MIELTA = 8
//        const val DEVICE_TYPE_ADM = 9

        private val chmLastDayWork = ConcurrentHashMap<Int, IntArray>()
        private val chmLastWorkShift = ConcurrentHashMap<Int, Int>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        fun fillDeviceTypeColumn(columnDeviceType: ColumnRadioButton) {
            columnDeviceType.defaultValue = DEVICE_TYPE_GALILEO

            columnDeviceType.addChoice(DEVICE_TYPE_GALILEO, "Galileo")
            columnDeviceType.addChoice(DEVICE_TYPE_PETROLINE, "Petroline")
            columnDeviceType.addChoice(DEVICE_TYPE_PULSAR_DATA, "Pulsar Data")
//            columnDeviceType.addChoice(DEVICE_TYPE_ARNAVI, "Arnavi")
//            columnDeviceType.addChoice(DEVICE_TYPE_ESCORT, "Escort")
//            columnDeviceType.addChoice(DEVICE_TYPE_DEL_VIDEO, "Видеорегистратор ДЭЛ-150В")
//            columnDeviceType.addChoice(DEVICE_TYPE_DEL_PULSAR, "ДЭЛ-Пульсар")
//            columnDeviceType.addChoice(DEVICE_TYPE_MIELTA, "Mielta")
//            columnDeviceType.addChoice(DEVICE_TYPE_ADM, "ADM")
        }

        //--- пришлось делать в виде static, т.к. VideoServer не является потомком MMSHandler,
        //--- а в AbstractHandler не знает про прикладные MMS-таблицы
        fun getCommand(stm: CoreAdvancedStatement, aDeviceID: Int): Pair<Int, String?> {
            var cmdID = 0
            var cmdStr: String? = null
            val rs = stm.executeQuery(
                """
                     SELECT MMS_device_command_history.id , MMS_device_command.cmd 
                     FROM MMS_device_command_history , MMS_device_command 
                     WHERE MMS_device_command_history.command_id = MMS_device_command.id 
                     AND MMS_device_command_history.device_id = $aDeviceID 
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

        fun setCommandSended(stm: CoreAdvancedStatement, cmdID: Int) {
            //--- отметим успешную отправку команды
            stm.executeUpdate(
                """ 
                    UPDATE MMS_device_command_history 
                    SET for_send = 0 , send_time = ${getCurrentTimeInt()} 
                    WHERE id = $cmdID
                """
            )
        }

        fun addPoint(stm: CoreAdvancedStatement, deviceConfig: DeviceConfig, time: Int, bbData: AdvancedByteBuffer, sqlBatchData: SQLBatch) {
            //--- если объект прописан, то записываем точки, иначе просто пропускаем
            if (deviceConfig.objectId != 0) {
                //--- если возможен режим оффлайн-загрузки данных по этому контроллеру (например, через android-посредника),
                //--- то возможно и повторение точек. В этом случае надо удалить предыдущую(ие) точку(и) с таким же временем.
                //--- Поскольку это очень затратная операция, то по умолчанию режим оффлайн-загрузки данных не включен
                if (deviceConfig.isOfflineMode) {
                    sqlBatchData.add(
                        """
                            DELETE FROM MMS_data_${deviceConfig.objectId} 
                            WHERE ontime = $time 
                        """
                    )
                }
                bbData.flip()
                sqlBatchData.add(
                    """
                        INSERT INTO MMS_data_${deviceConfig.objectId} ( ontime , sensor_data ) 
                        VALUES ( $time , ${stm.getHexValue(bbData)} ) 
                    """
                )
                //--- создаем новую пустую запись по суточной работе при необходимости
                checkAndCreateDayWork(stm, deviceConfig, time)
                //--- создаем новую пустую запись по рабочей смене при необходимости
                if (deviceConfig.isAutoWorkShift) {
                    checkAndCreateWorkShift(stm, deviceConfig, time)
                }
            }
        }

        private fun checkAndCreateDayWork(stm: CoreAdvancedStatement, deviceConfig: DeviceConfig, time: Int) {
            val arrLastDT = chmLastDayWork[deviceConfig.objectId]
            val arrDT = getDateTimeArray(deviceConfig.zoneId, time)
            //--- создаем новую пустую запись по дневной работе при необходимости
            if (arrLastDT == null || arrLastDT[0] != arrDT[0] || arrLastDT[1] != arrDT[1] || arrLastDT[2] != arrDT[2]) {
                //--- создадим пустую запись по дневной работе , если ее не было
                val rsADR = stm.executeQuery(
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
                    stm.executeUpdate(
                        """
                            INSERT INTO MMS_day_work ( id , user_id , object_id , ye , mo , da ) VALUES ( 
                            ${stm.getNextID("MMS_day_work", "id")} , ${deviceConfig.userId} , ${deviceConfig.objectId} , ${arrDT[0]} , ${arrDT[1]} , ${arrDT[2]} )
                        """
                    )
                }
                chmLastDayWork[deviceConfig.objectId] = arrDT
            }
        }

        private fun checkAndCreateWorkShift(stm: CoreAdvancedStatement, deviceConfig: DeviceConfig, time: Int) {
            var lastTime: Int? = chmLastWorkShift[deviceConfig.objectId]
            if (lastTime == null || lastTime < time) {
                lastTime = cWorkShift.autoCreateWorkShift(stm, deviceConfig.userId, deviceConfig.objectId)
                //--- создать не удалось - нет стартового шаблона - обнулим флаг автосоздания
                if (lastTime == null) {
                    //--- практически невозможная ситуация - включенный флаг автосоздания рабочих смен
                    //--- при отсутствии самих рабочих смен - поэтому достаточно выключить в локальных настройках,
                    //--- этого хватит для продолжения нормальной работы
                    //sqlBatch.add( new StringBuilder(
                    //    " UPDATE MMS_object SET is_auto_work_shift = 0 WHERE id = " ).append( deviceConfig.objectID ) );
                    deviceConfig.isAutoWorkShift = false
                }
                chmLastWorkShift[deviceConfig.objectId] = lastTime!!
            }
        }

        fun writeSession(
            conn: CoreAdvancedConnection,
            stm: CoreAdvancedStatement,
            dirSessionLog: File,
            zoneId: ZoneId,
            deviceId: Int,
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
            val logTime = DateTime_YMDHMS(zoneId, getCurrentTimeInt())
            val curLogFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

            var text = "$logTime $address Длительность [сек]: ${getCurrentTimeInt() - begTime} Точек записано: $dataCount из $dataCountAll "
            if (dataCountAll > 0) {
                text += " Время первой точки: ${DateTime_YMDHMS(zoneId, firstPointTime)} Время последней точки: ${DateTime_YMDHMS(zoneId, lastPointTime)} "
            }
            text += " Статус: $status "
            if (isOk || errorText.isEmpty()) {
            } else {
                text += " Ошибка: $errorText "
            }

            val dirDeviceSessionLog = File(dirSessionLog, "device/$deviceId")
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

            stm.executeUpdate(
                " UPDATE MMS_device SET fw_version = '$fwVersion' , last_session_time = ${getCurrentTimeInt()} , last_session_status = '$status' , " +
                    " last_session_error = '${if (isOk || errorText.isEmpty()) "" else errorText}' WHERE device_id = $deviceId "
            )

            conn.commit()
        }

        fun writeError(
            conn: CoreAdvancedConnection,
            stm: CoreAdvancedStatement,
            dirSessionLog: File,
            zoneId: ZoneId,
            deviceId: Int,
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
            if (deviceConfig != null && deviceId != 0) {
                writeSession(
                    conn = conn,
                    stm = stm,
                    dirSessionLog = dirSessionLog,
                    zoneId = zoneId,
                    deviceId = deviceId,
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
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val configSessionLogPath: String = "mms_log_session"
    override val configJournalLogPath: String = "mms_log_journal"

    //--- ID прибора - до сих пор всегда целое число
    protected var deviceId = 0

    //--- номер версии прошивки
    protected var fwVersion = ""

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun prepareErrorCommand(dataWorker: CoreDataWorker) {
        writeError(
            conn = dataWorker.conn,
            stm = dataWorker.stm,
            dirSessionLog = dirSessionLog,
            zoneId = zoneId,
            deviceId = deviceId,
            deviceConfig = deviceConfig,
            fwVersion = fwVersion,
            begTime = begTime,
            address = (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
            status = status,
            errorText = " Disconnect from device ID = $deviceId",
            dataCount = dataCount,
            dataCountAll = dataCountAll,
            firstPointTime = firstPointTime,
            lastPointTime = lastPointTime,
        )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadDeviceConfig(dataWorker: CoreDataWorker): Boolean {
        deviceConfig = DeviceConfig.getDeviceConfig(dataWorker.stm, deviceId)
        //--- неизвестный контроллер
        if (deviceConfig == null) {
            writeError(
                conn = dataWorker.conn,
                stm = dataWorker.stm,
                dirSessionLog = dirSessionLog,
                zoneId = zoneId,
                deviceId = deviceId,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                status = status,
                errorText = "Unknown device ID = $deviceId",
                dataCount = dataCount,
                dataCountAll = dataCountAll,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
            )
            writeJournal(
                dirJournalLog = dirJournalLog,
                zoneId = zoneId,
                address = (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                errorText = "Unknown device ID = $deviceId",
            )
            return false
        }
        status += " ID;"
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun roundSpeed(speed: Double) =
        when (deviceConfig!!.speedRoundRule) {
            SensorConfigGeo.SPEED_ROUND_RULE_LESS -> floor(speed).toInt().toShort()
            SensorConfigGeo.SPEED_ROUND_RULE_GREATER -> ceil(speed).toInt().toShort()
            SensorConfigGeo.SPEED_ROUND_RULE_STANDART -> round(speed).toInt().toShort()
            else -> round(speed).toInt().toShort()
        }

}
