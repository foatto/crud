package foatto.core_mms

import foatto.core.model.AppModuleUrl

val appModuleUrlsMMS = mutableMapOf(
    AppModuleMMS.OBJECT to AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT,
        formActionUrl = ApiUrlMMS.OBJECT_FORM_ACTION,
    ),
)