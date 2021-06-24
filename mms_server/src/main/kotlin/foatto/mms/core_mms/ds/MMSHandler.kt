package foatto.mms.core_mms.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeArray
import foatto.core.util.getFileWriter
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.ds.AbstractHandler
import foatto.core_server.ds.AbstractTelematicHandler
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.mms.core_mms.cWorkShift
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import foatto.sql.SQLBatch
import java.io.File
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.time.ZoneId
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

abstract class MMSHandler : AbstractTelematicHandler() {

    companion object {

        const val DEVICE_ID_DIVIDER = 10_000_000

        const val DEVICE_TYPE_GALILEO = 1
        const val DEVICE_TYPE_PETROLINE = 2
        const val DEVICE_TYPE_ANDROID = 3
        const val DEVICE_TYPE_ARNAVI = 4
        const val DEVICE_TYPE_ESCORT = 5
        const val DEVICE_TYPE_DEL_VIDEO = 6
        const val DEVICE_TYPE_DEL_PULSAR = 7
        const val DEVICE_TYPE_MIELTA = 8
        const val DEVICE_TYPE_ADM = 9

        private val chmLastDayWork = ConcurrentHashMap<Int, IntArray>()
        private val chmLastWorkShift = ConcurrentHashMap<Int, Int>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        fun fillDeviceTypeColumn(columnDeviceType: ColumnRadioButton) {
            columnDeviceType.defaultValue = DEVICE_TYPE_GALILEO

            columnDeviceType.addChoice(DEVICE_TYPE_GALILEO, "Galileo")
            columnDeviceType.addChoice(DEVICE_TYPE_PETROLINE, "Petroline")
            columnDeviceType.addChoice(DEVICE_TYPE_ANDROID, "Android")
            columnDeviceType.addChoice(DEVICE_TYPE_ARNAVI, "Arnavi")
            columnDeviceType.addChoice(DEVICE_TYPE_ESCORT, "Escort")
            columnDeviceType.addChoice(DEVICE_TYPE_DEL_VIDEO, "Видеорегистратор ДЭЛ-150В")
            columnDeviceType.addChoice(DEVICE_TYPE_DEL_PULSAR, "ДЭЛ-Пульсар")
            columnDeviceType.addChoice(DEVICE_TYPE_MIELTA, "Mielta")
            columnDeviceType.addChoice(DEVICE_TYPE_ADM, "ADM")
        }

        //--- пришлось делать в виде static, т.к. VideoServer не является потомком MMSHandler,
        //--- а в AbstractHandler не знает про прикладные MMS-таблицы
        fun getCommand(stm: CoreAdvancedStatement, aDeviceID: Int): Pair<Int, String?> {
            var cmdID = 0
            var cmdStr: String? = null
            val rs = stm.executeQuery(
                " SELECT MMS_device_command_history.id , MMS_device_command.cmd " +
                    " FROM MMS_device_command_history , MMS_device_command " +
                    " WHERE MMS_device_command_history.command_id = MMS_device_command.id " +
                    " AND MMS_device_command_history.device_id = $aDeviceID AND MMS_device_command_history.for_send <> 0 " +
                    " ORDER BY MMS_device_command_history.send_time "
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
            stm.executeUpdate(" UPDATE MMS_device_command_history SET for_send = 0 , send_time = ${getCurrentTimeInt()} WHERE id = $cmdID")
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val configSessionLogPath: String = "mms_log_session"
    override val configJournalLogPath: String = "mms_log_journal"

    //--- ID прибора - до сих пор всегда целое число
    protected var deviceID = 0

    //--- номер версии прошивки
    protected var fwVersion = 0

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun prepareErrorCommand(dataWorker: CoreDataWorker) {
        writeError(dataWorker.conn, dataWorker.stm, " Disconnect from device ID = $deviceID")
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadDeviceConfig(dataWorker: CoreDataWorker): Boolean {
        //--- во избежание многократной перезагрузки конфигурации прибора в случае,
        //--- когда в каждом пакете данных идёт IMEI-код.
        //--- и раз в 16-17 мин перезагружаем конфигурацию контроллера на случай его перепривязки к другому объекту
        if (deviceConfig == null || getCurrentTimeInt() - lastDeviceConfigOutTime > DEVICE_CONFIG_OUT_PERIOD) {
            deviceConfig = DeviceConfig.getDeviceConfig(dataWorker.stm, deviceID)
            //--- неизвестный контроллер
            if (deviceConfig == null) {
                writeError(dataWorker.conn, dataWorker.stm, "Unknown device ID = $deviceID")
                writeJournal()
                return false
            }
            sbStatus.append("ID;")
            lastDeviceConfigOutTime = getCurrentTimeInt()
        }
        return true
    }

    protected fun writeError(conn: CoreAdvancedConnection, stm: CoreAdvancedStatement, aError: String) {
        sbStatus.append("Error;")
        errorText = aError
        if (deviceConfig != null && deviceID != 0) {
            writeSession(conn, stm, false)
        }
        AdvancedLogger.error(aError)
    }

    protected fun writeSession(conn: CoreAdvancedConnection, stm: CoreAdvancedStatement, isOk: Boolean) {
        //--- какое д.б. имя лог-файла для текущего дня и часа
        val logTime = DateTime_YMDHMS(zoneId, getCurrentTimeInt())
        val curLogFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

        //--- SocketChannel.getLocalAddress(), который есть в Oracle Java, не существует в Android Java,
        //--- поэтому используем более общий метод SocketChannel.socket().getLocalAddress()
        val sbText = StringBuilder(logTime).append(' ').append((selectionKey!!.channel() as SocketChannel).socket().localAddress).append(' ')
            .append(" Длительность [сек]: ").append(getCurrentTimeInt() - begTime).append(' ')
            .append(" Точек записано: ").append(dataCount).append(" из ").append(dataCountAll)
        if (dataCountAll > 0)
            sbText.append(" Время первой точки: ").append(DateTime_YMDHMS(zoneId, firstPointTime))
                .append(" Время последней точки: ").append(DateTime_YMDHMS(zoneId, lastPointTime))
        sbText.append(" Статус: ").append(sbStatus).append(' ')
        if (isOk || errorText == null) {
        } else {
            sbText.append(" Ошибка: ").append(errorText).toString()
        }
        val text = sbText.toString()

        val dirDeviceSessionLog = File(dirSessionLog, "device/$deviceID")
        dirDeviceSessionLog.mkdirs()
        var out = getFileWriter(File(dirDeviceSessionLog, curLogFileName), true)
        out.write(text)
        out.newLine()
        out.flush()
        out.close()

        val dirObjectSessionLog = File(dirSessionLog, "object/${deviceConfig!!.objectID}")
        dirObjectSessionLog.mkdirs()
        out = getFileWriter(File(dirObjectSessionLog, curLogFileName), true)
        out.write(text)
        out.newLine()
        out.flush()
        out.close()

        stm.executeUpdate(
            " UPDATE MMS_device SET fw_version = $fwVersion , last_session_time = ${getCurrentTimeInt()} , last_session_status = '$sbStatus' , " +
                " last_session_error = '${if (isOk || errorText == null) "" else errorText}' WHERE device_id = $deviceID "
        )

        conn.commit()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun roundSpeed(speed: Double): Short {
        when (deviceConfig!!.speedRoundRule) {
            SensorConfigGeo.SPEED_ROUND_RULE_LESS -> return floor(speed).toInt().toShort()
            SensorConfigGeo.SPEED_ROUND_RULE_GREATER -> return ceil(speed).toInt().toShort()
            SensorConfigGeo.SPEED_ROUND_RULE_STANDART -> return round(speed).toInt().toShort()
            else -> return round(speed).toInt().toShort()
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun putSensorPortNumAndDataSize(portNum: Int, dataSize: Int, bbData: AdvancedByteBuffer) {
        bbData.putShort(deviceConfig!!.index * MAX_PORT_PER_DEVICE + portNum).putShort(dataSize - 1)
    }

    protected fun addPoint(stm: CoreAdvancedStatement, time: Int, bbData: AdvancedByteBuffer, sqlBatchData: SQLBatch) {
        //--- если объект прописан, то записываем точки, иначе просто пропускаем
        if (deviceConfig!!.objectID != 0) {
            //--- если возможен режим оффлайн-загрузки данных по этому контроллеру (например, через android-посредника),
            //--- то возможно и повторение точек. В этом случае надо удалить предыдущую(ие) точку(и) с таким же временем.
            //--- Поскольку это очень затратная операция, то по умолчанию режим оффлайн-загрузки данных не включен
            if (deviceConfig!!.isOfflineMode) sqlBatchData.add(" DELETE FROM MMS_data_${deviceConfig!!.objectID} WHERE ontime = $time ; ")
            bbData.flip()
            sqlBatchData.add(" INSERT INTO MMS_data_${deviceConfig!!.objectID} ( ontime , sensor_data ) VALUES ( $time , ${stm.getHexValue(bbData)} ); ")
            //--- создаем новую пустую запись по суточной работе при необходимости
            checkAndCreateDayWork(stm, time)
            //--- создаем новую пустую запись по рабочей смене при необходимости
            if (deviceConfig!!.isAutoWorkShift) checkAndCreateWorkShift(stm, time)
        }
    }

    private fun checkAndCreateDayWork(stm: CoreAdvancedStatement, time: Int) {
        val arrLastDT = chmLastDayWork[deviceConfig!!.objectID]
        val arrDT = getDateTimeArray(deviceConfig!!.zoneId, time)
        //--- создаем новую пустую запись по дневной работе при необходимости
        if (arrLastDT == null || arrLastDT[0] != arrDT[0] || arrLastDT[1] != arrDT[1] || arrLastDT[2] != arrDT[2]) {
            //--- создадим пустую запись по дневной работе , если ее не было
            val rsADR = stm.executeQuery(
                " SELECT id FROM MMS_day_work WHERE object_id = ${deviceConfig!!.objectID} AND ye = ${arrDT[0]} AND mo = ${arrDT[1]} AND da = ${arrDT[2]}"
            )

            val isExist = rsADR.next()
            rsADR.close()
            if (!isExist)
                stm.executeUpdate(
                    " INSERT INTO MMS_day_work ( id , user_id , object_id , ye , mo , da ) VALUES ( " +
                        "${stm.getNextID("MMS_day_work", "id")} , ${deviceConfig!!.userID} , ${deviceConfig!!.objectID} , ${arrDT[0]} , ${arrDT[1]} , ${arrDT[2]} ); "
                )

            chmLastDayWork[deviceConfig!!.objectID] = arrDT
        }
    }

    private fun checkAndCreateWorkShift(stm: CoreAdvancedStatement, time: Int) {
        var lastTime: Int? = chmLastWorkShift[deviceConfig!!.objectID]
        if (lastTime == null || lastTime < time) {
            lastTime = cWorkShift.autoCreateWorkShift(stm, deviceConfig!!.userID, deviceConfig!!.objectID)
            //--- создать не удалось - нет стартового шаблона - обнулим флаг автосоздания
            if (lastTime == null) {
                //--- практически невозможная ситуация - включенный флаг автосоздания рабочих смен
                //--- при отсутствии самих рабочих смен - поэтому достаточно выключить в локальных настройках,
                //--- этого хватит для продолжения нормальной работы
                //sqlBatch.add( new StringBuilder(
                //    " UPDATE MMS_object SET is_auto_work_shift = 0 WHERE id = " ).append( deviceConfig.objectID ) );
                deviceConfig!!.isAutoWorkShift = false
            }
            chmLastWorkShift[deviceConfig!!.objectID] = lastTime!!
        }
    }

}
