package foatto.server

import foatto.core.model.response.xy.XyElementClientType
import foatto.core.model.response.xy.XyElementConfig

fun initXyElementConfig(level: Int, minScale: Int, maxScale: Int): MutableMap<String, XyElementConfig> {
    val hmElementConfig = mutableMapOf<String, XyElementConfig>()

    hmElementConfig[XyElementClientType.BITMAP.name] = XyElementConfig(
        name = XyElementClientType.BITMAP.name,
        clientType = XyElementClientType.BITMAP,
        layer = 0,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = false,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementClientType.ICON.name] = XyElementConfig(
        name = XyElementClientType.ICON.name,
        clientType = XyElementClientType.ICON,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = true,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementClientType.MARKER.name] = XyElementConfig(
        name = XyElementClientType.MARKER.name,
        clientType = XyElementClientType.MARKER,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = true,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementClientType.POLY.name] = XyElementConfig(
        name = XyElementClientType.POLY.name,
        clientType = XyElementClientType.POLY,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = false,
        isMoveable = true,
        isEditablePoint = true
    )

    hmElementConfig[XyElementClientType.TEXT.name] = XyElementConfig(
        name = XyElementClientType.TEXT.name,
        clientType = XyElementClientType.TEXT,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = true,
        isMoveable = true,
        isEditablePoint = false
    )

    hmElementConfig[XyElementClientType.TRACE.name] = XyElementConfig(
        name = XyElementClientType.TRACE.name,
        clientType = XyElementClientType.TRACE,
        layer = level,
        scaleMin = minScale,
        scaleMax = maxScale,
        descrForAction = "",
        isRotatable = false,
        isMoveable = true,
        isEditablePoint = true
    )

    return hmElementConfig
}
