package foatto.ts_compose

import foatto.core_compose.Root
import foatto.core_compose.di.SETTINGS_SERVER_ADDRESS
import foatto.core_compose.di.SETTINGS_SERVER_PORT
import foatto.core_compose.di.SETTINGS_SERVER_PROTOCOL
import foatto.core_compose.di.SETTINGS_APP_PARAM
import foatto.core_compose.di.settings

class TSRoot : Root() {
    init {
        settings.putString(SETTINGS_SERVER_PROTOCOL, "http")
        settings.putString(SETTINGS_SERVER_ADDRESS, "192.168.0.44")
        settings.putInt(SETTINGS_SERVER_PORT, 19997)

        if (settings.getStringOrNull(SETTINGS_APP_PARAM) == null) {
            settings.putString(SETTINGS_APP_PARAM, "alias=ts_object&action=table")
        }
    }
}