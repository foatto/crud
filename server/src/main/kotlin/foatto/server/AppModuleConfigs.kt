package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.server.model.AppModuleConfig
import foatto.server.model.Permission

val appModuleConfigs: MutableMap<String, AppModuleConfig> = mutableMapOf(

    AppModule.USER to AppModuleConfig(
        caption = "Пользователи",
        pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
        permissions = mutableMapOf(
            ActionType.MODULE_TABLE to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                disabledRoles = getRoleAllPermissions(AppRole.USER),
            ),
            ActionType.MODULE_FORM to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                disabledRoles = getRoleAllPermissions(AppRole.USER),
            ),
            ActionType.FORM_ADD to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                disabledRoles = getRoleAllPermissions(AppRole.USER),
            ),
            ActionType.FORM_EDIT to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                disabledRoles = getRoleAllPermissions(AppRole.USER),
            ),
            ActionType.FORM_DELETE to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                disabledRoles = getRoleAllPermissions(AppRole.USER),
            ),
            ActionType.FORM_EXIT to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                disabledRoles = getRoleAllPermissions(AppRole.USER),
            ),
        )
    ),
)

fun getRoleAllPermissions(vararg roles: String): MutableMap<UserRelationEnum, MutableSet<String>> = mutableMapOf(
    UserRelationEnum.SELF to mutableSetOf(*roles),
    UserRelationEnum.EQUAL to mutableSetOf(*roles),
    UserRelationEnum.BOSS to mutableSetOf(*roles),
    UserRelationEnum.WORKER to mutableSetOf(*roles),
    UserRelationEnum.OTHER to mutableSetOf(*roles),
    UserRelationEnum.NOBODY to mutableSetOf(*roles),
)

fun addEnabledRoles(moduleName: String, permissionName: String, enabledRole: String) {
    appModuleConfigs[moduleName]?.let { appModuleConfig ->
        appModuleConfig.permissions[permissionName]?.let { permission ->
            permission.enabledRoles[UserRelationEnum.SELF]?.add(enabledRole)
            permission.enabledRoles[UserRelationEnum.EQUAL]?.add(enabledRole)
            permission.enabledRoles[UserRelationEnum.BOSS]?.add(enabledRole)
            permission.enabledRoles[UserRelationEnum.WORKER]?.add(enabledRole)
            permission.enabledRoles[UserRelationEnum.OTHER]?.add(enabledRole)
            permission.enabledRoles[UserRelationEnum.NOBODY]?.add(enabledRole)
        }
    }
}

fun addDisabledRoles(moduleName: String, permissionName: String, disabledRole: String) {
    appModuleConfigs[moduleName]?.let { appModuleConfig ->
        appModuleConfig.permissions[permissionName]?.let { permission ->
            permission.disabledRoles[UserRelationEnum.SELF]?.add(disabledRole)
            permission.disabledRoles[UserRelationEnum.EQUAL]?.add(disabledRole)
            permission.disabledRoles[UserRelationEnum.BOSS]?.add(disabledRole)
            permission.disabledRoles[UserRelationEnum.WORKER]?.add(disabledRole)
            permission.disabledRoles[UserRelationEnum.OTHER]?.add(disabledRole)
            permission.disabledRoles[UserRelationEnum.NOBODY]?.add(disabledRole)
        }
    }
}
