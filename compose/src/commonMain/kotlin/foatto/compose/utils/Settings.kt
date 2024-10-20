package foatto.compose.utils

import com.russhwolf.settings.Settings

const val SETTINGS_SERVER_PROTOCOL: String = "server_protocol"
const val SETTINGS_SERVER_ADDRESS: String = "server_address"
const val SETTINGS_SERVER_PORT: String = "server_port"

const val SETTINGS_START_MODULE: String = "start_module"

const val SETTINGS_LOGIN: String = "login"
const val SETTINGS_PASSWORD: String = "password"
const val SETTINGS_LOGON_EXPIRE: String = "logon_expire"

val settings: Settings = Settings()