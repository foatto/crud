package foatto.server

import foatto.server.model.AppRoleConfig

val appRoleConfigs: MutableMap<String, AppRoleConfig> = mutableMapOf(
    AppRole.ADMIN to AppRoleConfig(
        redirectOnLogon = null,
    ),
    AppRole.USER to AppRoleConfig(
        redirectOnLogon = null,
    ),
)
