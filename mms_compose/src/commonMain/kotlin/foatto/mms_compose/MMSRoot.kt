package foatto.mms_compose

import foatto.core_compose.Root
import foatto.core_compose.SETTINGS_SERVER_ADDRESS
import foatto.core_compose.SETTINGS_SERVER_PORT
import foatto.core_compose.SETTINGS_SERVER_PROTOCOL
import foatto.core_compose.di.settings
import io.ktor.http.*

class MMSRoot : Root() {
    init {
        settings.putString(SETTINGS_SERVER_PROTOCOL, URLProtocol.HTTP.name)
        settings.putString(SETTINGS_SERVER_ADDRESS, "192.168.0.44")
        settings.putInt(SETTINGS_SERVER_PORT, 19999)

        if (settings.getStringOrNull(SETTINGS_APP_PARAM) == null) {
            settings.putString(SETTINGS_APP_PARAM, "alias=mms_day_work&action=table")
        }
    }
}