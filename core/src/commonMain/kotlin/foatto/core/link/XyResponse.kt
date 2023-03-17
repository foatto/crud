package foatto.core.link

import kotlinx.serialization.Serializable

@Serializable
class XyResponse(
    val documentConfig: XyDocumentConfig,
    val startParamId: String,
    val shortTitle: String,
    val fullTitle: String,
    val arrServerActionButton: Array<XyServerActionButton>,
)

@Serializable
class XyDocumentConfig(
    val name: String,
    val descr: String,
    val serverClassName: String,
    val clientType: XyDocumentClientType,
    val isScaleAlign: Boolean,
    val alElementConfig: Array<Pair<String, XyElementConfig>>
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

@Serializable
class XyServerActionButton(
    val caption: String,
    val tooltip: String,
    val icon: String,
    val url: String,
    val isForWideScreenOnly: Boolean,
)

enum class XyDocumentClientType {
    MAP,
    STATE
}

enum class XyElementClientType {
    BITMAP,
    ICON,
    MARKER,
    POLY,
    SVG_TEXT,
    HTML_TEXT,
    TRACE,
    ZONE,
}

