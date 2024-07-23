package foatto.core

import foatto.core.model.AppModuleUrl

val appModuleUrls: MutableMap<String, AppModuleUrl> = mutableMapOf(
    AppModule.USER to AppModuleUrl(
        appUrl = ApiUrl.USER,
        formActionUrl = ApiUrl.USER_FORM_ACTION,
    ),
)