package foatto.server.model

import foatto.core.model.AppUserConfig

class ServerUserConfig(
    val id: Int,
    val currentUserName: String,
    val roles: Set<String>,
    val timeOffset: Int,
) {
    fun toAppUserConfig() = AppUserConfig(
        currentUserName = currentUserName,
        timeOffset = timeOffset,
    )
}