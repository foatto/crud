package foatto.core.link

class XyResponse(
    val documentConfig: XyDocumentConfig,
    val startParamId: String,
    val shortTitle: String,
    val fullTitle: String,
    val arrServerActionButton: Array<XyServerActionButton>,
)

class XyDocumentConfig(
    val name: String,
    val descr: String,
    val serverClassName: String,
    val clientType: XyDocumentClientType,
    val itScaleAlign: Boolean,
    val alElementConfig: Array<Pair<String, XyElementConfig>>
)

class XyElementConfig(
    val name: String,
    val clientType: XyElementClientType,
    val layer: Int,
    val scaleMin: Int,
    val scaleMax: Int,
    val descrForAction: String,
    val itRotatable: Boolean,
    val itMoveable: Boolean,
//    val itCopyable: Boolean,
    val itEditablePoint: Boolean
//    val itEditableText: Boolean,
)

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
