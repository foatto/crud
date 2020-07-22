@file:JvmName("cLogSession")
package foatto.mms.core_mms.device

import foatto.app.CoreSpringApp
import foatto.core_server.app.system.cLogText
import java.io.File

class cLogSession : cLogText() {

    //--- по умолчанию возвращаем просто соответствующую папку логов
    override fun getLogDir(): File {
        val dirLog = File( CoreSpringApp.hmAliasLogDir[ aliasConfig.alias ] )

        val dID = hmParentData[ "mms_device" ]
        val oID = hmParentData[ "mms_object" ]

        return if( dID != null ) File( dirLog, "device/$dID" )
          else if( oID != null ) File( dirLog, "object/$oID" )
          else dirLog
    }
}
