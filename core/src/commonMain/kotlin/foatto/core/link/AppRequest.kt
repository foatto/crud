package foatto.core.link

class AppRequest(
    var action: String,

    val logon: LogonRequest? = null,
    val find: String? = null,
    val alFormData: List<FormData>? = null
) {
    var sessionID: Long = 0
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class LogonRequest( val login: String, val password: String ) {
    val hmSystemProperties = mutableMapOf<String,String>()
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
    val fileID: Int? = null,
    //-- используем для передачи String-to-String вместо Int-to-String из-за проблем сериализации в JSON (т.к. имена полей получатся в виде цифр)
    val hmFileAdd: Map<String,String>? = null,
    val alFileRemovedID: List<Int>? = null
)

