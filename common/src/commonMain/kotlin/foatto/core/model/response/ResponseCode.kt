package foatto.core.model.response

import kotlinx.serialization.Serializable

@Serializable
enum class ResponseCode {
    NOTHING,
    OK,
    ERROR,

    LOGON_NEED,
    LOGON_SUCCESS,
    LOGON_SUCCESS_BUT_OLD,
    LOGON_FAILED,
    LOGON_SYSTEM_BLOCKED,
    LOGON_ADMIN_BLOCKED,

    MODULE_TABLE,
    MODULE_FORM,
    MODULE_CHART,
    MODULE_MAP,
    MODULE_SCHEME,
    MODULE_COMPOSITE,
}