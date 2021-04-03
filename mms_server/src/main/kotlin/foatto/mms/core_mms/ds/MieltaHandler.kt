package foatto.mms.core_mms.ds

import foatto.core_server.ds.CoreDataServer
import java.nio.channels.SelectionKey

class MieltaHandler : WialonIPSHandler() {

    override fun init(aDataServer: CoreDataServer, aSelectionKey: SelectionKey) {
        deviceType = DEVICE_TYPE_MIELTA

        super.init(aDataServer, aSelectionKey)
    }

}