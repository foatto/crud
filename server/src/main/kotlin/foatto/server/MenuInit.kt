package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.model.AppAction
import foatto.core.model.response.MenuData
import foatto.server.model.ServerUserConfig

var menuInit: (serverUserConfig: ServerUserConfig) -> List<MenuData> = { serverUserConfig ->
    val alMenu = mutableListOf<MenuData>()

//        mutableListOf<MenuData>().apply {
//
//            addMenuItem(AppModule.LICENSE, ActionType.MODULE_TABLE, serverUserConfig, this)
//            addMenuItem(AppModule.DEVICE, ActionType.MODULE_TABLE, serverUserConfig, this)
//
//            if (size > 0) {
//                alMenu += MenuData("Лицензирование", null, this)
//            }
//        }

    mutableListOf<MenuData>().apply {

        addMenuItem(AppModule.USER, ActionType.MODULE_TABLE, serverUserConfig, this)

        if (size > 1) {
            alMenu += MenuData("Система", null, this)
        }
    }

    alMenu
}

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
            return
        }
    }
}

fun addSeparator(alMenu: MutableList<MenuData>) {
    alMenu += MenuData("", null)
}
