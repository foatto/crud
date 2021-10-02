package foatto.ts.core_ts.ds

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

abstract class TSHandler : AbstractTelematicHandler() {

    companion object {

        const val DEVICE_TYPE_UDS = 1

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        fun fillDeviceTypeColumn(columnDeviceType: ColumnRadioButton) {
            columnDeviceType.defaultValue = DEVICE_TYPE_UDS

            columnDeviceType.addChoice(DEVICE_TYPE_UDS, "УДС-Техно")
        }

//        //--- пришлось делать в виде static, т.к. VideoServer не является потомком TSHandler,
//        //--- а в AbstractHandler не знает про прикладные TS-таблицы
//        fun getCommand(stm: CoreAdvancedStatement, aDeviceID: Int): Pair<Int, String?> {
//            var cmdID = 0
//            var cmdStr: String? = null
//            val rs = stm.executeQuery(
//                " SELECT TS_device_command_history.id , TS_device_command.cmd " +
//                    " FROM TS_device_command_history , TS_device_command " +
//                    " WHERE TS_device_command_history.command_id = TS_device_command.id " +
//                    " AND TS_device_command_history.device_id = $aDeviceID AND TS_device_command_history.for_send <> 0 " +
//                    " ORDER BY TS_device_command_history.send_time "
//            )
//            if (rs.next()) {
//                cmdID = rs.getInt(1)
//                cmdStr = rs.getString(2).trim()
//            }
//            rs.close()
//
//            return Pair(cmdID, cmdStr)
//        }
//
//        fun setCommandSended(stm: CoreAdvancedStatement, cmdID: Int) {
//            //--- отметим успешную отправку команды
//            stm.executeUpdate(" UPDATE TS_device_command_history SET for_send = 0 , send_time = ${getCurrentTimeInt()} WHERE id = $cmdID")
//        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val configSessionLogPath: String = "ts_log_session"
    override val configJournalLogPath: String = "ts_log_journal"

    //--- серийный номер прибора
    protected var serialNo = ""

    //--- номер версии прошивки
    protected var fwVersion = ""

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun prepareErrorCommand(dataWorker: CoreDataWorker) {
        writeError(dataWorker.conn, dataWorker.stm, " Disconnect from serial No = $serialNo")
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadDeviceConfig(dataWorker: CoreDataWorker): Boolean {
        deviceConfig = DeviceConfig.getDeviceConfig(dataWorker.stm, serialNo)
        //--- неизвестный контроллер
        if (deviceConfig == null) {
            writeError(dataWorker.conn, dataWorker.stm, "Unknown serial No = $serialNo")
            writeJournal(
                dirJournalLog = dirJournalLog,
                zoneId = zoneId,
                address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                errorText = "Unknown serial No = $serialNo",
            )
            return false
        }
        status += " ID;"
        return true
    }

    protected fun writeError(conn: CoreAdvancedConnection, stm: CoreAdvancedStatement, aError: String) {
        status += " Error;"
        errorText = aError
        if (deviceConfig != null && serialNo.isNotEmpty()) {
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
        sbText.append(" Статус: ").append(status).append(' ')
        if (isOk || errorText.isEmpty()) {
        } else {
            sbText.append(" Ошибка: ").append(errorText).toString()
        }
        val text = sbText.toString()

        val dirDeviceSessionLog = File(dirSessionLog, "device/$serialNo")
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
            " UPDATE TS_device SET fw_version = '$fwVersion' , last_session_time = ${getCurrentTimeInt()} , last_session_status = '$status' , " +
                " last_session_error = '${if (isOk || errorText == null) "" else errorText}' WHERE serial_no = '$serialNo' "
        )

        conn.commit()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun addPoint(stm: CoreAdvancedStatement, time: Int, bbData: AdvancedByteBuffer, sqlBatchData: SQLBatch) {
        //--- если возможен режим оффлайн-загрузки данных по этому контроллеру (например, через android-посредника),
        //--- то возможно и повторение точек. В этом случае надо удалить предыдущую(ие) точку(и) с таким же временем.
        //--- Поскольку это очень затратная операция, то по умолчанию режим оффлайн-загрузки данных не включен
        //if (deviceConfig!!.isOfflineMode) sqlBatchData.add(" DELETE FROM TS_data_${deviceConfig!!.objectID} WHERE ontime = $time ; ")
        bbData.flip()
        sqlBatchData.add(" INSERT INTO TS_data_${deviceConfig!!.objectID} ( ontime , sensor_data ) VALUES ( $time , ${stm.getHexValue(bbData)} ); ")
    }

}
