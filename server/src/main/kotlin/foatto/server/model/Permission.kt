package foatto.server.model

import foatto.server.UserRelationEnum

class Permission(
    val enabledRoles: MutableMap<UserRelationEnum, MutableSet<String>> = mutableMapOf(),
    val disabledRoles: MutableMap<UserRelationEnum, MutableSet<String>> = mutableMapOf(),
)