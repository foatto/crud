package foatto.core

object ApiUrl {
    const val URL_BASE: String = "/api"
    const val CURRENT_VERSION: String = "v1"

    const val ERROR: String = "$URL_BASE/error/$CURRENT_VERSION"

    const val UPLOAD_FORM_FILE: String = "$URL_BASE/upload_form_file/$CURRENT_VERSION"

    const val LOGON: String = "$URL_BASE/logon/$CURRENT_VERSION"
    const val LOGOFF: String = "$URL_BASE/logoff/$CURRENT_VERSION"
    const val CHANGE_PASSWORD: String = "$URL_BASE/change_password/$CURRENT_VERSION"

    const val USER: String = "$URL_BASE/user/$CURRENT_VERSION"
    const val USER_FORM_ACTION: String = "$URL_BASE/user_form_action/$CURRENT_VERSION"
}