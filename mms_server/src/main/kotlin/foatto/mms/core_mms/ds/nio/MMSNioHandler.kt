package foatto.mms.core_mms.ds.nio

import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.AbstractTelematicNioHandler
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.mms.core_mms.ds.DeviceConfig
import foatto.mms.core_mms.ds.MMSTelematicFunction
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.sql.CoreAdvancedConnection
import java.nio.channels.SocketChannel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

abstract class MMSNioHandler : AbstractTelematicNioHandler() {

    override val configSessionLogPath: String = "mms_log_session"
    override val configJournalLogPath: String = "mms_log_journal"

    //--- серийный номер прибора
    protected var serialNo = ""

    //--- номер версии прошивки
    protected var fwVersion = ""

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun prepareErrorCommand(dataWorker: CoreNioWorker) {
        MMSTelematicFunction.writeError(
            conn = dataWorker.conn,
            dirSessionLog = dirSessionLog,
            zoneId = zoneId,
            deviceConfig = deviceConfig,
            fwVersion = fwVersion,
            begTime = begTime,
            address = selectionKey?.let { sk ->
                (sk.channel() as SocketChannel).remoteAddress.toString() + " -> " + (sk.channel() as SocketChannel).localAddress.toString()
            } ?: "(unknown remote address)",
            status = status,
            errorText = "Disconnect from device ID = $serialNo",
            dataCount = dataCount,
            dataCountAll = dataCountAll,
            firstPointTime = firstPointTime,
            lastPointTime = lastPointTime,
        )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadDeviceConfig(conn: CoreAdvancedConnection): Boolean {
        deviceConfig = DeviceConfig.getDeviceConfig(conn, serialNo)
        //--- неизвестный контроллер
        if (deviceConfig == null) {
            MMSTelematicFunction.writeError(
                conn = conn,
                dirSessionLog = dirSessionLog,
                zoneId = zoneId,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = selectionKey?.let { sk ->
                    (sk.channel() as SocketChannel).remoteAddress.toString() + " -> " + (sk.channel() as SocketChannel).localAddress.toString()
                } ?: "(unknown remote address)",
                status = status,
                errorText = "Unknown device ID = $serialNo",
                dataCount = dataCount,
                dataCountAll = dataCountAll,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
            )
            CoreTelematicFunction.writeJournal(
                dirJournalLog = dirJournalLog,
                zoneId = zoneId,
                address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                errorText = "Unknown device ID = $serialNo",
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
