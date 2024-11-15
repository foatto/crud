package foatto.core.model.response.xy

import kotlinx.serialization.Serializable

@Serializable
enum class XyElementClientType {
    ARC,
    BITMAP,
    ICON,
    MARKER,
    POLY,
    TEXT,
    TRACE,
    ZONE,
}
