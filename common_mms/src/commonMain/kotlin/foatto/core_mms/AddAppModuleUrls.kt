package foatto.core_mms

import foatto.core.appModuleUrls
import foatto.core.model.AppModuleUrl

fun addAppModuleUrls() {
    appModuleUrls[AppModuleMMS.OBJECT] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT,
        formActionUrl = ApiUrlMMS.OBJECT_FORM_ACTION,
    )
}
