package foatto.core_mms

import foatto.core.ApiUrl

object ApiUrlMMS {
    const val OBJECT: String = "${ApiUrl.URL_BASE}/object/${ApiUrl.CURRENT_VERSION}"
    const val OBJECT_FORM_ACTION: String = "${ApiUrl.URL_BASE}/object_form_action/${ApiUrl.CURRENT_VERSION}"

    const val DEPARTMENT: String = "${ApiUrl.URL_BASE}/department/${ApiUrl.CURRENT_VERSION}"
    const val DEPARTMENT_FORM_ACTION: String = "${ApiUrl.URL_BASE}/department_form_action/${ApiUrl.CURRENT_VERSION}"

    const val GROUP: String = "${ApiUrl.URL_BASE}/group/${ApiUrl.CURRENT_VERSION}"
    const val GROUP_FORM_ACTION: String = "${ApiUrl.URL_BASE}/group_form_action/${ApiUrl.CURRENT_VERSION}"

    const val SENSOR: String = "${ApiUrl.URL_BASE}/sensor/${ApiUrl.CURRENT_VERSION}"
    const val SENSOR_FORM_ACTION: String = "${ApiUrl.URL_BASE}/sensor_form_action/${ApiUrl.CURRENT_VERSION}"

    const val SENSOR_CALIBRATION: String = "${ApiUrl.URL_BASE}/sensor_calibration/${ApiUrl.CURRENT_VERSION}"
    const val SENSOR_CALIBRATION_FORM_ACTION: String = "${ApiUrl.URL_BASE}/sensor_calibration_form_action/${ApiUrl.CURRENT_VERSION}"

    const val CHART_LIQUID_LEVEL: String = "${ApiUrl.URL_BASE}/chart_liquid_level/${ApiUrl.CURRENT_VERSION}"
    const val CHART_LIQUID_LEVEL_ACTION: String = "${ApiUrl.URL_BASE}/chart_liquid_level_action/${ApiUrl.CURRENT_VERSION}"

    const val MAP_TRACE: String = "${ApiUrl.URL_BASE}/map_trace/${ApiUrl.CURRENT_VERSION}"
    const val MAP_TRACE_ACTION: String = "${ApiUrl.URL_BASE}/map_trace_action/${ApiUrl.CURRENT_VERSION}"

    const val SCHEME_ANALOGUE_INDICATOR_STATE: String = "${ApiUrl.URL_BASE}/scheme_analogue_indicator_state/${ApiUrl.CURRENT_VERSION}"
    const val SCHEME_ANALOGUE_INDICATOR_STATE_ACTION: String = "${ApiUrl.URL_BASE}/scheme_analogue_indicator_state_action/${ApiUrl.CURRENT_VERSION}"

    const val COMPOSITE_OBJECT_DASHBOARD: String = "${ApiUrl.URL_BASE}/composite_object_dashboard/${ApiUrl.CURRENT_VERSION}"
}