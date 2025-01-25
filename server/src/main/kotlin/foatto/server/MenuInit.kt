package foatto.server

import foatto.core.model.AppAction
import foatto.core.model.response.MenuData
import foatto.server.model.ServerUserConfig

var menuInit: (serverUserConfig: ServerUserConfig) -> List<MenuData> = { serverUserConfig -> mutableListOf() }

fun addMenuItem(module: String, actionType: String, serverUserConfig: ServerUserConfig, alMenu: MutableList<MenuData>) {
    appModuleConfigs[module]?.let { moduleConfig ->
        if (checkAccessPermission(module, serverUserConfig.roles)) {
            alMenu += MenuData(
                caption = moduleConfig.caption,
                action = AppAction(
                    type = actionType,
                    module = module,
                ),
            )
        }
    }
}

fun addSeparator(alMenu: MutableList<MenuData>) {
    alMenu += MenuData("", null)
}
