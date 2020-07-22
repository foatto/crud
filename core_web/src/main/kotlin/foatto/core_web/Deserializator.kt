//package foatto.core_web
//
////{"code":"LOGON_NEED","redirect":null,"table":null,"form":null,"graphic":null,"xy":null,"hmUserProperty":null,"alMenuData":null}
//
//////--- pos - позиция первой кавычки
////fun getJsonParamName( json: String, pos: Int ): Pair<String,Int> {
////    val nextQuotePos = json.indexOf( '"', pos + 1 )
////    return Pair( json.substring( pos + 1, nextQuotePos ), nextQuotePos + 2 )
////}
////
////fun getJsonStringValue( json: String, pos: Int ): Pair<String?,Int> {
////    //--- null
////    if( json[ pos ] == 'n' ) {
////        return Pair( null, pos + 5 )
////    }
////    else {
////        val nextQuotePos = json.indexOf( '"', pos + 1 )
////        return Pair( json.substring( pos + 1, nextQuotePos ), nextQuotePos + 2 )
////    }
////}
//
//
////--- pos - позиция первой кавычки
//fun getJsonName( json: String, pos: Int ): Pair<String,Int> {
//    val nextQuotePos = json.indexOf( '"', pos + 1 )
//    return Pair( json.substring( pos + 1, nextQuotePos ), nextQuotePos + 2 )
//}
//
//fun getJsonValue( json: String, pos: Int ): Pair<Any?,Int> {
//    val jsonValue =
//        when( json[ pos ] ) {
//            //--- null
//            'n' -> {
//                Pair( null, pos + 4 )
//            }
//            //--- String
//            '"' -> {
//                val nextQuotePos = json.indexOf( '"', pos + 1 )
//                Pair( json.substring( pos + 1, nextQuotePos ), nextQuotePos + 1 )
//            }
//            //--- List
//            '[' -> {
//                parseJsonList( json, pos + 1 )
//            }
//            //--- Map
//            '{' -> {
//                parseJsonMap( json, pos + 1 )
//            }
//            //--- other value (number)
//            else -> {
//                val nextDelimiterPos = json.indexOfAny( charArrayOf( ',', ']', '}' ), pos + 1 )
//                Pair( json.substring( pos, nextDelimiterPos ), nextDelimiterPos )
//            }
//        }
//
//    return jsonValue
//}
//
//fun parseJson( json: String ): Map<String, Any?> {
//    val hmJson = mutableMapOf<String, Any?>()
//
//    var pos = 1
//
//    while( pos < json.length - 1 ) {
//        val ( paramName, valuePos ) = getJsonName( json, pos )
//        val ( paramValue, nextPos ) = getJsonValue( json, valuePos )
//
//        hmJson[ paramName ] = paramValue
//
//        pos = nextPos
//    }
//
//    return hmJson
//}
//
//fun parseJsonList( json: String, startPos: Int ): Pair<List<Any?>,Int> {
//    val alResult = mutableListOf<Any?>()
//
//    var pos = startPos
//
//    if( json[ pos ] == ']' )
//        pos++
//    else
//        while( true ) {
//            val ( paramValue, nextPos ) = getJsonValue( json, pos )
//            alResult.add( paramValue )
//
//            pos = nextPos + 1
//            if( json[ nextPos ] == ']' ) break
//        }
//
//    return Pair( alResult, pos )
//}