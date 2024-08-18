package foatto.server.model

import foatto.core.model.AppUserConfig
import foatto.server.UserRelationEnum

class ServerUserConfig(
    val id: Int,
    val currentUserName: String,
    val roles: Set<String>,
    val timeOffset: Int,
    val fullNames: Map<Int, String>,
    val shortNames: Map<Int, String>,
    val relatedUserIds: Map<Int, UserRelationEnum>,
) {
    fun toAppUserConfig(): AppUserConfig = AppUserConfig(
        currentUserName = currentUserName,
        timeOffset = timeOffset,
    )
}