package foatto.core_web

import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.AppRequest
import foatto.core.link.ChangePasswordRequest
import foatto.core.link.FormData
import foatto.core.link.LogoffRequest
import foatto.core.link.LogonRequest
import foatto.core.link.SaveUserPropertyRequest

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

fun Pair<Number, Number>?.toJson(fieldName: String) =
    "\"$fieldName\":" +
        (this?.let {
            "{\"first\":$first," +
                "\"second\":$second}"
        }
            ?: "null")

fun Pair<String, String>?.toJson(fieldName: String) =
    "\"$fieldName\":" +
        (this?.let {
            "{${first.toJson("first")}," +
                "${second.toJson("second")}\"}"
        }
            ?: "null")

fun List<Number>?.toJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) json += "null"
    else {
        json += "["

        for (value in this)
            json += "\"$value\","

        if (this.isNotEmpty())
            json = json.substring(0, json.length - 1)

        json += "]"
    }
    return json
}

fun List<String>?.toJson(fieldName: String): String {
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

fun Map<String, String>?.toJson(fieldName: String): String {
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

fun AppRequest.toJson(): String {
    var json = "{"

    json += action.toJson("action") + ","

    json += logon.toJson("logon") + ","

    json += find.toJson("find") + ","

    if (alFormData == null) {
        json += "\"alFormData\":null,"
    } else {
        json += "\"alFormData\":["

        for (formData in alFormData!!)
            json += "${formData.toJson()},"

        if (alFormData!!.isNotEmpty())
            json = json.substring(0, json.length - 1)

        json += "],"
    }

    json += sessionId.toJson("sessionId")

    return "$json}"
}

//----------------------------------------------------------------------------------------------------------------

fun LogonRequest?.toJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) json += "null"
    else {
        json += "{"

        json += login.toJson("login") + ","
        json += password.toJson("password") + ","

        json += hmSystemProperties.toJson("hmSystemProperties")

        json += "}"
    }
    return json
}

//----------------------------------------------------------------------------------------------------------------

fun FormData.toJson(): String {
    var json = "{"

    json += stringValue.toJson("stringValue") + ","

    json += textValue.toJson("textValue") + ","

    json += booleanValue.toJson("booleanValue") + ","

    json += alDateTimeValue.toJson("alDateTimeValue") + ","

    json += comboValue.toJson("comboValue") + ","

    json += fileID.toJson("fileID") + ","

    json += hmFileAdd?.mapKeys { it.key.toString() }.toJson("hmFileAdd") + ","

//    if( hmFileAdd == null ) {
//        json += "\"hmFileAdd\":null,"
//    }
//    else {
//        json += "\"hmFileAdd\":{"
//        for( (key, value) in hmFileAdd )
//            json += "\"$key\": \"${value}\","
//        if( hmFileAdd.isNotEmpty() )
//            json = json.substring( 0, json.length - 1 )
//        json += "},"
//    }

    json += alFileRemovedID.toJson("alFileRemovedID")

    return "$json}"
}

//----------------------------------------------------------------------------------------------------------------

fun GraphicActionRequest.toJson(): String {
    var json = "{"

    json += documentTypeName.toJson("documentTypeName") + ","
    json += action.toString().toJson("action") + ","
    json += startParamId.toJson("startParamId") + ","

    json += graphicCoords.toJson("graphicCoords") + ","
    json += viewSize.toJson("viewSize") + ","

    json += sessionId.toJson("sessionId")

    return "$json}"
}

//----------------------------------------------------------------------------------------------------------------

fun XyActionRequest.toJson(): String {
    var json = "{"

    json += documentTypeName.toJson("documentTypeName") + ","
    json += action.toString().toJson("action") + ","
    json += startParamId.toJson("startParamId") + ","

    json += viewCoord.toJson("viewCoord") + ","

    json += elementId.toJson("elementId") + ","

    json += bitmapTypeName.toJson("bitmapTypeName") + ","

    json += objectId.toJson("objectId") + ","

    json += xyElement.toJson("xyElement") + ","

    json += alActionElementIds.toJson("alActionelementIds") + ","
    json += dx.toJson("dx") + ","
    json += dy.toJson("dy") + ","

    json += sessionId.toJson("sessionId") + ","

    json += hmParam.toJson("hmParam")

    return "$json}"
}

fun XyViewCoord?.toJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) {
        json += "null"
    } else {
        json += "{"
        json += scale.toJson("scale") + ","
        json += x1.toJson("x1") + ","
        json += y1.toJson("y1") + ","
        json += x2.toJson("x2") + ","
        json += y2.toJson("y2")
        json += "}"
    }

    return json
}

fun XyElement?.toJson(fieldName: String): String {
    var json = "\"$fieldName\":"

    if (this == null) {
        json += "null"
    } else {
        json += "{"

        json += typeName.toJson("typeName") + ","
        json += elementId.toJson("elementId") + ","
        json += objectId.toJson("objectId") + ","

        json += "\"alPoint\":["
        for (point in alPoint)
            json += point.toJson() + ","
        if (alPoint.isNotEmpty())
            json = json.substring(0, json.length - 1)
        json += "]" + ","

        json += itClosed.toJson("itClosed") + ","

        json += lineWidth.toJson("lineWidth") + ","

        json += drawColor.toJson("drawColor") + ","
        json += fillColor.toJson("fillColor") + ","

        json += anchorX.toString().toJson("anchorX") + ","
        json += anchorY.toString().toJson("anchorY") + ","

        json += rotateDegree.toJson("rotateDegree") + ","

        json += toolTipText.toJson("toolTipText") + ","
        json += itReadOnly.toJson("itReadOnly") + ","
        json += itActual.toJson("itActual") + ","

        json += imageName.toJson("imageName") + ","
        json += imageWidth.toJson("imageWidth") + ","
        json += imageHeight.toJson("imageHeight") + ","

        json += markerType.toString().toJson("markerType") + ","
        json += markerSize.toJson("markerSize") + ","
        json += markerSize2.toJson("markerSize2") + ","

        json += text.toJson("text") + ","
        json += textColor.toJson("textColor") + ","
        json += fontSize.toJson("fontSize") + ","
        json += itFontBold.toJson("itFontBold") + ","

        json += limitWidth.toJson("limitWidth") + ","
        json += limitHeight.toJson("limitHeight") + ","
        json += alignX.toString().toJson("alignX") + ","
        json += alignY.toString().toJson("alignY") + ","

        json += arrowPos.toString().toJson("arrowPos") + ","
        json += arrowLen.toJson("arrowLen") + ","
        json += arrowHeight.toJson("arrowHeight") + ","
        json += arrowLineWidth.toJson("arrowLineWidth") + ","

        json += alDrawColor.toList().toJson("alDrawColor") + ","
        json += alFillColor.toList().toJson("alFillColor") + ","
        json += alToolTip.toList().toJson("alToolTip")

        json += "}"
    }

    return json
}

fun XyPoint?.toJson(fieldName: String? = null): String {
    var json = if (fieldName == null) "" else "\"$fieldName\":"

    if (this == null) {
        json += "null"
    } else {
        json += "{"

        json += x.toJson("x") + ","
        json += y.toJson("y")

        json += "}"
    }

    return json
}

//----------------------------------------------------------------------------------------------------------------

fun SaveUserPropertyRequest.toJson(): String {
    var json = "{"

    json += name.toJson("name") + ","
    json += value.toJson("value") + ","

    json += sessionId.toJson("sessionId")

    return "$json}"
}

fun ChangePasswordRequest.toJson(): String {
    var json = "{"

    json += password.toJson("password") + ","

    json += sessionId.toJson("sessionId")

    return "$json}"
}

fun LogoffRequest.toJson(): String {
    var json = "{"

    json += sessionId.toJson("sessionId")

    return "$json}"
}
