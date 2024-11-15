package foatto.compose.utils

internal actual fun getFullUrl(url: String): String {
    val protocol = settings.getStringOrNull(SETTINGS_SERVER_PROTOCOL) ?: "http"
    val host = settings.getString(SETTINGS_SERVER_ADDRESS, "")
    val port = settings.getInt(SETTINGS_SERVER_PORT, 80)

    return "$protocol://$host:$port$url"
}