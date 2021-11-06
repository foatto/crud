package foatto.core.link

class AppRequest(
    var action: String,

    val logon: LogonRequest? = null,
    val find: String? = null,
    val alFormData: List<FormData>? = null
) {
    var sessionId: Long = 0
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class LogonRequest(val login: String, val password: String) {
    val hmSystemProperties = mutableMapOf<String, String>()
}

class FormData(
    //--- STRING, INT, DOUBLE
    val stringValue: String? = null,

    //--- TEXT
    val textValue: String? = null,

    //--- BOOLEAN
    val booleanValue: Boolean? = null,

    //--- DATE, TIME, DATE_TIME
    val alDateTimeValue: List<String>? = null,

    //--- COMBO, RADIO
    val comboValue: Int? = null,

    //--- FILE
    val fileId: Int? = null,
    //-- use for passing String-to-String instead of Int-to-String due to serialization problems in JSON (since field names will be obtained as numbers)
    val hmFileAdd: Map<String, String>? = null,
    val alFileRemovedId: List<Int>? = null
)

