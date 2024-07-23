package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.server.model.AppModuleConfig

val appModuleConfigs: MutableMap<String, AppModuleConfig> = mutableMapOf(

    AppModule.USER to AppModuleConfig(
        caption = "Пользователи",
        permissions = mapOf(
            ActionType.MODULE_TABLE to setOf(AppRole.ADMIN),
            ActionType.MODULE_FORM to setOf(AppRole.ADMIN),
            ActionType.FORM_ADD to setOf(AppRole.ADMIN),
            ActionType.FORM_EDIT to setOf(AppRole.ADMIN),
            ActionType.FORM_DELETE to setOf(AppRole.ADMIN),
            ActionType.FORM_EXIT to setOf(AppRole.ADMIN),
        )
    ),
)
