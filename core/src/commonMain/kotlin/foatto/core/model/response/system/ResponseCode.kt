package foatto.core.model.response.system

import kotlinx.serialization.Serializable

@Serializable
enum class ResponseCode {
    NOTHING,
    OK,
    ERROR,

    LOGON_NEED,
    LOGON_SUCCESS,
    LOGON_FAILED,
    LOGON_ADMIN_BLOCKED,

    MODULE_TABLE,
    MODULE_FORM,
}