package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {

//--- system actions

    NOTHING,

    FILE,

    CHANGE_PASSWORD,
    LOGOFF,

    SET_START_PAGE,
    CLEAR_START_PAGE,

    MODULE_TABLE,
    MODULE_FORM,

    FORM_ADD,
    FORM_EDIT,
    FORM_DELETE,
    FORM_EXIT,
    FORM_SELECTOR,

//--- MMS custom actions

//--- TS custom actions

}
