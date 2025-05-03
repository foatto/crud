package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.server.model.AppModuleConfig
import foatto.server.model.Permission

val appModuleConfigs: MutableMap<String, AppModuleConfig> = mutableMapOf(

    AppModule.USER to AppModuleConfig(
        caption = "Пользователи",
        enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
        disabledAccessRoles = mutableSetOf(AppRole.USER),
        enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
        disabledFormAddRoles = mutableSetOf(AppRole.USER),
        rowPermissions = mutableMapOf(
            ActionType.MODULE_TABLE to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                disabledRoles = getRoleAllPermissions(AppRole.USER),
            ),
            ActionType.MODULE_FORM to Permission(
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
        )
    ),

    AppModule.USER_PROPERTY_EDIT to AppModuleConfig(
        caption = "Мои настройки",
        enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
        disabledAccessRoles = mutableSetOf(),
        disabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
        rowPermissions = mutableMapOf(
            ActionType.MODULE_TABLE to Permission(
                disabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
            ),
            ActionType.MODULE_FORM to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
            ),
            ActionType.FORM_EDIT to Permission(
                enabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
            ),
            ActionType.FORM_DELETE to Permission(
                disabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
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
        appModuleConfig.rowPermissions[permissionName]?.let { permission ->
            permission.enabledRoles.getOrPut(UserRelationEnum.SELF) { mutableSetOf() }.add(enabledRole)
            permission.enabledRoles.getOrPut(UserRelationEnum.EQUAL) { mutableSetOf() }.add(enabledRole)
            permission.enabledRoles.getOrPut(UserRelationEnum.BOSS) { mutableSetOf() }.add(enabledRole)
            permission.enabledRoles.getOrPut(UserRelationEnum.WORKER) { mutableSetOf() }.add(enabledRole)
            permission.enabledRoles.getOrPut(UserRelationEnum.OTHER) { mutableSetOf() }.add(enabledRole)
            permission.enabledRoles.getOrPut(UserRelationEnum.NOBODY) { mutableSetOf() }.add(enabledRole)
        }
    }
}

fun addDisabledRoles(moduleName: String, permissionName: String, disabledRole: String) {
    appModuleConfigs[moduleName]?.let { appModuleConfig ->
        appModuleConfig.rowPermissions[permissionName]?.let { permission ->
            permission.disabledRoles.getOrPut(UserRelationEnum.SELF) { mutableSetOf() }.add(disabledRole)
            permission.disabledRoles.getOrPut(UserRelationEnum.EQUAL) { mutableSetOf() }.add(disabledRole)
            permission.disabledRoles.getOrPut(UserRelationEnum.BOSS) { mutableSetOf() }.add(disabledRole)
            permission.disabledRoles.getOrPut(UserRelationEnum.WORKER) { mutableSetOf() }.add(disabledRole)
            permission.disabledRoles.getOrPut(UserRelationEnum.OTHER) { mutableSetOf() }.add(disabledRole)
            permission.disabledRoles.getOrPut(UserRelationEnum.NOBODY) { mutableSetOf() }.add(disabledRole)
        }
    }
}

fun checkAccessPermission(module: String?, userRoles: Set<String>): Boolean {
    appModuleConfigs[module]?.let { moduleConfig ->
        //--- любая отрицательная роль достаточна для отказа
        if (
            moduleConfig.disabledAccessRoles.any { disabledRole ->
                userRoles.contains(disabledRole)
            }
        ) {
            return false
        }
        //--- любая положительная роль достаточна для согласия
        if (
            moduleConfig.enabledAccessRoles.any { enabledRole ->
                userRoles.contains(enabledRole)
            }
        ) {
            return true
        }
    }
    //--- по всех прочих/неопределённых ситуациях - отказ
    return false
}

fun checkFormAddPermission(module: String?, userRoles: Set<String>): Boolean {
    appModuleConfigs[module]?.let { moduleConfig ->
        //--- любая отрицательная роль достаточна для отказа
        if (
            moduleConfig.disabledFormAddRoles.any { disabledRole ->
                userRoles.contains(disabledRole)
            }
        ) {
            return false
        }
        //--- любая положительная роль достаточна для согласия
        if (
            moduleConfig.enabledFormAddRoles.any { enabledRole ->
                userRoles.contains(enabledRole)
            }
        ) {
            return true
        }
    }
    //--- по всех прочих/неопределённых ситуациях - отказ
    return false
}

fun checkRowPermission(module: String?, actionType: String, rowUserRelation: UserRelationEnum?, userRoles: Set<String>): Boolean {
    appModuleConfigs[module]?.let { moduleConfig ->
        moduleConfig.rowPermissions[actionType]?.let { permission ->
            //--- вначале любая отрицательная роль достаточна для отказа
            if (
                permission.disabledRoles[rowUserRelation]?.any { disabledRole ->
                    userRoles.contains(disabledRole)
                } == true
            ) {
                return false
            }
            //--- после этого любая положительная роль достаточна для согласия
            if (
                permission.enabledRoles[rowUserRelation]?.any { enabledRole ->
                    userRoles.contains(enabledRole)
                } == true
            ) {
                return true
            }
        }
    }
    //--- по всех прочих/неопределённых ситуациях - отказ
    return false
}

fun checkReportUnlockPermission(module: String?, userRoles: Set<String>): Boolean {
    appModuleConfigs[module]?.let { moduleConfig ->
        //--- любая отрицательная роль достаточна для отказа
        if (
            moduleConfig.disabledReportUnlockRoles.any { disabledRole ->
                userRoles.contains(disabledRole)
            }
        ) {
            return false
        }
        //--- любая положительная роль достаточна для согласия
        if (
            moduleConfig.enabledReportUnlockRoles.any { enabledRole ->
                userRoles.contains(enabledRole)
            }
        ) {
            return true
        }
    }
    //--- по всех прочих/неопределённых ситуациях - отказ
    return false
}

fun getEnabledUserIds(
    module: String?,
    actionType: String,
    relatedUserIds: Map<Int, UserRelationEnum>,
    roles: Set<String>,
): List<Int> =
    relatedUserIds.filterValues { userRelation ->
        checkRowPermission(module, actionType, userRelation, roles)
    }.map { (userId, _) ->
        userId
    }
