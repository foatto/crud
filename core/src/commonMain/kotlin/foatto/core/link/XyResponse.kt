package foatto.core.link

import kotlinx.serialization.Serializable

@Serializable
class XyResponse(
    val documentConfig: XyDocumentConfig,
    val startParamId: String,
    val title: String = "",
    val tooltip: String = "",
    val alServerActionButton: List<ServerActionButton>? = null,
)

@Serializable
class XyDocumentConfig(
    val name: String,
    val descr: String,
    val serverClassName: String,
    val clientType: XyDocumentClientType,
    val isScaleAlign: Boolean,
    val hmElementConfig: Map<String, XyElementConfig>,
)

@Serializable
class XyElementConfig(
    val name: String,
    val clientType: XyElementClientType,
    val layer: Int,
    val scaleMin: Int,
    val scaleMax: Int,
    val descrForAction: String,
    val isRotatable: Boolean,
    val isMoveable: Boolean,
//    val isCopyable: Boolean,
    val isEditablePoint: Boolean
//    val isEditableText: Boolean,
)

enum class XyDocumentClientType {
    MAP,
    SCHEME,
}

enum class XyElementClientType {
    ARC,
    BITMAP,
    ICON,
    MARKER,
    POLY,
    TEXT,
    SVG_TEXT,   //!!!убрать после отключение compose_html
    HTML_TEXT,  //!!!убрать после отключение compose_html
    TRACE,
    ZONE,
}

