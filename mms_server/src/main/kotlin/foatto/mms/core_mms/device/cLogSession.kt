package foatto.mms.core_mms.device

import foatto.core_server.app.system.cLogText
import java.io.File

class cLogSession : cLogText() {

    //--- по умолчанию возвращаем просто соответствующую папку логов
    override fun getLogDir(): File? =
        application.hmAliasLogDir[aliasConfig.alias]?.let { dirName ->
            val dirLog = File(dirName)

            getParentId("mms_device")?.let {
                File(dirLog, "device/$it")
            } ?: getParentId("mms_object")?.let {
                File(dirLog, "object/$it")
            } ?: dirLog
        }
}
