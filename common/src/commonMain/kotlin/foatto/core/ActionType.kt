package foatto.core

object ActionType {
    const val NOTHING: String = ""

    const val FILE: String = "FILE"

    const val CHANGE_PASSWORD: String = "CHANGE_PASSWORD"
    const val LOGOFF: String = "LOGOFF"

    const val SET_START_MODULE: String = "SET_START_MODULE"
    const val CLEAR_START_MODULE: String = "CLEAR_START_MODULE"

    const val MODULE_TABLE: String = "MODULE_TABLE"
    const val MODULE_FORM: String = "MODULE_FORM"

    const val FORM_ADD: String = "FORM_ADD"
    const val FORM_EDIT: String = "FORM_EDIT"
    const val FORM_DELETE: String = "FORM_DELETE"
    const val FORM_EXIT: String = "FORM_EXIT"
    const val FORM_SELECTOR: String = "FORM_SELECTOR"
}
