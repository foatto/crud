package foatto.server.ds

import foatto.server.model.SensorConfigGeo
import java.nio.channels.SocketChannel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import foatto.server.sql.CoreAdvancedConnection

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
            timeZone = timeZone,
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
        DeviceConfig.getDeviceConfig(conn, serialNo)?.let { dc ->
            deviceConfig = dc
            status += " ID;"
            return true
        } ?: run {
            //--- неизвестный контроллер
            MMSTelematicFunction.writeError(
                conn = conn,
                dirSessionLog = dirSessionLog,
                timeZone = timeZone,
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
                timeZone = timeZone,
                address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                errorText = "Unknown device ID = $serialNo",
            )
            return false
        }
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
