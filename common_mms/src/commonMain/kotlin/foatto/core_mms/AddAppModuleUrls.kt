package foatto.core_mms

import foatto.core.appModuleUrls
import foatto.core.model.AppModuleUrl

fun addAppModuleUrls() {
    appModuleUrls[AppModuleMMS.OBJECT] = AppModuleUrl(
        appUrl = ApiUrlMMS.OBJECT,
        formActionUrl = ApiUrlMMS.OBJECT_FORM_ACTION,
    )

    appModuleUrls[AppModuleMMS.DEPARTMENT] = AppModuleUrl(
        appUrl = ApiUrlMMS.DEPARTMENT,
        formActionUrl = ApiUrlMMS.DEPARTMENT_FORM_ACTION,
    )
    appModuleUrls[AppModuleMMS.GROUP] = AppModuleUrl(
        appUrl = ApiUrlMMS.GROUP,
        formActionUrl = ApiUrlMMS.GROUP_FORM_ACTION,
    )
}
