package foatto.ts.core_ts.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getFileWriter
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.AbstractTelematicNioHandler
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.sql.CoreAdvancedConnection
import foatto.sql.SQLBatch
import java.io.File
import java.nio.channels.SocketChannel

abstract class TSHandler : AbstractTelematicNioHandler() {

    companion object {

        const val DEVICE_TYPE_UDS = 1
        const val DEVICE_TYPE_UDS_OLD = 2

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        fun fillDeviceTypeColumn(columnDeviceType: ColumnRadioButton) {
            columnDeviceType.defaultValue = DEVICE_TYPE_UDS

            columnDeviceType.addChoice(DEVICE_TYPE_UDS, "УДС-Техно 2.0")
            columnDeviceType.addChoice(DEVICE_TYPE_UDS_OLD, "УДС-Техно 1.0")
        }

    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val configSessionLogPath: String = "ts_log_session"
    override val configJournalLogPath: String = "ts_log_journal"

    //--- серийный номер прибора
    protected var serialNo = ""

    //--- IMEI код модема
    protected var imei = ""

    //--- номер версии прошивки
    protected var fwVersion = ""

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun prepareErrorCommand(dataWorker: CoreNioWorker) {
        writeError(dataWorker.conn, " Disconnect from serial No = $serialNo")
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadDeviceConfig(dataWorker: CoreNioWorker, deviceType: Int): Boolean {
        deviceConfig = DeviceConfig.getDeviceConfig(dataWorker.conn, serialNo, deviceType)
        //--- неизвестный контроллер
        if (deviceConfig == null) {
            writeError(dataWorker.conn, "Unknown serial No = $serialNo")
            CoreTelematicFunction.writeJournal(
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

    protected fun writeError(conn: CoreAdvancedConnection, aError: String) {
        status += " Error;"
        errorText = aError
        if (deviceConfig != null && serialNo.isNotEmpty()) {
            writeSession(conn, false)
        }
        AdvancedLogger.error(aError)
    }

    protected fun writeSession(conn: CoreAdvancedConnection, isOk: Boolean) {
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

        deviceConfig?.let { dc ->
            val dirDeviceSessionLog = File(dirSessionLog, "device/${dc.deviceId}")
            dirDeviceSessionLog.mkdirs()
            var out = getFileWriter(File(dirDeviceSessionLog, curLogFileName), true)
            out.write(text)
            out.newLine()
            out.flush()
            out.close()

            val dirObjectSessionLog = File(dirSessionLog, "object/${dc.objectId}")
            dirObjectSessionLog.mkdirs()
            out = getFileWriter(File(dirObjectSessionLog, curLogFileName), true)
            out.write(text)
            out.newLine()
            out.flush()
            out.close()

            conn.executeUpdate(
                """
                    UPDATE TS_device SET 
                    fw_version = '$fwVersion' , 
                    imei = '$imei' , 
                    last_session_time = ${getCurrentTimeInt()} , 
                    last_session_status = '$status' ,
                    last_session_error = '${if (isOk || errorText.isEmpty()) "" else errorText}' 
                    WHERE id = '${deviceConfig!!.deviceId}'
                """
            )
        }

        conn.commit()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun addPoint(conn: CoreAdvancedConnection, time: Int, bbData: AdvancedByteBuffer, sqlBatchData: SQLBatch) {
        bbData.flip()
        sqlBatchData.add(" INSERT INTO TS_data_${deviceConfig!!.objectId} ( ontime , sensor_data ) VALUES ( $time , ${conn.getHexValue(bbData)} ); ")
    }

}
