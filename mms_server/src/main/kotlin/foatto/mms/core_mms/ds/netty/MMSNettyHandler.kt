package foatto.mms.core_mms.ds.netty

import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.netty.AbstractTelematicNettyHandler
import foatto.mms.core_mms.ds.DeviceConfig
import foatto.mms.core_mms.ds.MMSTelematicFunction
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.sql.CoreAdvancedConnection
import foatto.sql.DBConfig
import io.netty.channel.Channel
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

abstract class MMSNettyHandler(
    dbConfig: DBConfig,
    dirSessionLog: File,
    dirJournalLog: File,
) : AbstractTelematicNettyHandler(
    dbConfig,
    dirSessionLog,
    dirJournalLog,
) {

    //--- серийный номер прибора
    protected var serialNo = ""

    //--- номер версии прошивки
    protected var fwVersion = ""

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadDeviceConfig(conn: CoreAdvancedConnection, channel: Channel): Boolean {
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
                address = channel.remoteAddress().toString() + " -> " + channel.localAddress().toString(),
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
                address = channel.remoteAddress().toString() + " -> " + channel.localAddress().toString(),
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