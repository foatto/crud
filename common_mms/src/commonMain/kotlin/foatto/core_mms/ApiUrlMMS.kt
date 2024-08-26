package foatto.core_mms

import foatto.core.ApiUrl

object ApiUrlMMS {
    const val OBJECT: String = "${ApiUrl.URL_BASE}/object/${ApiUrl.CURRENT_VERSION}"
    const val OBJECT_FORM_ACTION: String = "${ApiUrl.URL_BASE}/object_form_action/${ApiUrl.CURRENT_VERSION}"

    const val DEPARTMENT: String = "${ApiUrl.URL_BASE}/department/${ApiUrl.CURRENT_VERSION}"
    const val DEPARTMENT_FORM_ACTION: String = "${ApiUrl.URL_BASE}/department_form_action/${ApiUrl.CURRENT_VERSION}"

    const val GROUP: String = "${ApiUrl.URL_BASE}/group/${ApiUrl.CURRENT_VERSION}"
    const val GROUP_FORM_ACTION: String = "${ApiUrl.URL_BASE}/group_form_action/${ApiUrl.CURRENT_VERSION}"
}