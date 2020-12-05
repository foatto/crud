package foatto.shop

//----------------------------------------------------------------------------------------------------------------

fun escapeString(raw: String) = raw.replace('"', '`').replace('\'', '`').replace("\\", "\\\\")/*.replace( "/", "\\/" )*/.replace("\n", "\\n").replace("\t", "\\t")

//----------------------------------------------------------------------------------------------------------------

fun Boolean?.toJson(fieldName: String) = "\"$fieldName\":" + (this?.let { "$this" } ?: "null")

fun Number?.toJson(fieldName: String) = "\"$fieldName\":" + (this?.let { "$this" } ?: "null")

fun String?.toJson(fieldName: String) =
    "\"$fieldName\":" +
        (this?.let {
            "\"${escapeString(this)}\""
        } ?: "null")

//fun Pair<Number,Number>?.toJson( fieldName: String ) =
//    "\"$fieldName\":" +
//    ( this?.let {
//        "{\"first\":$first," +
//         "\"second\":$second}"
//    }
//    ?: "null" )
//
//fun Pair<String,String>?.toJson( fieldName: String ) =
//    "\"$fieldName\":" +
//    ( this?.let {
//        "{${first.toJson("first")}," +
//         "${second.toJson("second")}\"}"
//    }
//    ?: "null" )

fun Array<Long>?.toJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) json += "null"
    else {
        json += "["

        for (value in this)
            json += "$value,"

        if (this.isNotEmpty())
            json = json.substring(0, json.length - 1)

        json += "]"
    }
    return json
}

fun List<Number>?.numberToJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) json += "null"
    else {
        json += "["

        for (value in this)
            json += "$value,"

        if (this.isNotEmpty())
            json = json.substring(0, json.length - 1)

        json += "]"
    }
    return json
}

fun List<String>?.stringToJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) json += "null"
    else {
        json += "["

        for (value in this)
            json += "\"${escapeString(value)}\","

        if (this.isNotEmpty())
            json = json.substring(0, json.length - 1)

        json += "]"
    }
    return json
}

fun Map<String, String>?.stringToJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) json += "null"
    else {
        json += "{"

        for ((key, value) in this)
            json += value.toJson(key) + ","

        if (this.isNotEmpty())
            json = json.substring(0, json.length - 1)

        json += "}"
    }
    return json
}

//----------------------------------------------------------------------------------------------------------------
//
//fun AppRequest.toJson(): String {
//    var json = "{"
//
//    json += action.toJson( "action" ) + ","
//
//    json += logon.toJson( "logon" ) + ","
//
//    json += find.toJson( "find" ) + ","
//
//    if( alFormData == null ) {
//        json += "\"alFormData\":null,"
//    }
//    else {
//        json += "\"alFormData\":["
//
//        for( formData in alFormData!! )
//            json += "${formData.toJson()},"
//
//        if( alFormData!!.isNotEmpty() )
//            json = json.substring( 0, json.length - 1 )
//
//        json += "],"
//    }
//
//    json += sessionID.toJson( "sessionID" )
//
//    return "$json}"
//}
//
////----------------------------------------------------------------------------------------------------------------
//
//fun LogonRequest?.toJson( fieldName: String ): String {
//    var json = "\"$fieldName\":"
//
//    if( this == null ) json += "null"
//    else {
//        json += "{"
//
//        json += login.toJson( "login" ) + ","
//        json += password.toJson( "password" ) + ","
//
//        json += hmSystemProperties.toJson( "hmSystemProperties" )
//
//        json += "}"
//    }
//    return json
//}
//
////----------------------------------------------------------------------------------------------------------------
//
//fun FormData.toJson(): String {
//    var json = "{"
//
//    json += stringValue.toJson( "stringValue" ) + ","
//
//    json += textValue.toJson( "textValue" ) + ","
//
//    json += booleanValue.toJson( "booleanValue" ) + ","
//
//    json += alDateTimeValue.toJson( "alDateTimeValue" ) + ","
//
//    json += comboValue.toJson( "comboValue" ) + ","
//
//    json += fileID.toJson( "fileID" ) + ","
//
//    json += hmFileAdd?.mapKeys { it.key.toString() }.toJson( "hmFileAdd" ) + ","
//
////    if( hmFileAdd == null ) {
////        json += "\"hmFileAdd\":null,"
////    }
////    else {
////        json += "\"hmFileAdd\":{"
////        for( (key, value) in hmFileAdd )
////            json += "\"$key\": \"${value}\","
////        if( hmFileAdd.isNotEmpty() )
////            json = json.substring( 0, json.length - 1 )
////        json += "},"
////    }
//
////    json += alFileRemovedID.toJson( "alFileRemovedID" )
//
//    return "$json}"
//}
//
////----------------------------------------------------------------------------------------------------------------
//
//fun SaveUserPropertyRequest.toJson(): String {
//    var json = "{"
//
//    json += name.toJson( "name" ) + ","
//    json += value.toJson( "value" ) + ","
//
//    json += sessionID.toJson( "sessionID" )
//
//    return "$json}"
//}
//
//fun ChangePasswordRequest.toJson(): String {
//    var json = "{"
//
//    json += password.toJson( "password" ) + ","
//
//    json += sessionID.toJson( "sessionID" )
//
//    return "$json}"
//}
//
//fun LogoffRequest.toJson(): String {
//    var json = "{"
//
//    json += sessionID.toJson( "sessionID" )
//
//    return "$json}"
//}
