package foatto.compose_ts

import foatto.compose.Root
import foatto.compose.utils.SETTINGS_START_MODULE
import foatto.compose.utils.SETTINGS_SERVER_ADDRESS
import foatto.compose.utils.SETTINGS_SERVER_PORT
import foatto.compose.utils.SETTINGS_SERVER_PROTOCOL
import foatto.compose.utils.settings
import foatto.core_ts.AppModuleTS

class TSRoot : Root() {
    init {
        settings.putString(SETTINGS_SERVER_PROTOCOL, "http")
        settings.putString(SETTINGS_SERVER_ADDRESS, "192.168.0.44")
        settings.putInt(SETTINGS_SERVER_PORT, 19997)

        if (settings.getStringOrNull(SETTINGS_START_MODULE) == null) {
            settings.putString(SETTINGS_START_MODULE, AppModuleTS.OBJECT)
        }
    }
}