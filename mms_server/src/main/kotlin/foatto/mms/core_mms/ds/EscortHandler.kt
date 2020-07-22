@file:JvmName("EscortHandler")
package foatto.mms.core_mms.ds

import foatto.core_server.ds.CoreDataServer
import java.nio.channels.SelectionKey

class EscortHandler : WialonIPSHandler() {

    override fun init( aDataServer: CoreDataServer, aSelectionKey: SelectionKey ) {
        deviceType = MMSHandler.DEVICE_TYPE_ESCORT

        super.init( aDataServer, aSelectionKey )
    }

}
