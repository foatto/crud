package foatto.core.link

////import kotlinx.serialization.Serializable

////@Serializable
class XyResponse( val documentConfig: XyDocumentConfig, val startParamID: String, val shortTitle: String, val fullTitle: String, val parentObjectID: Int, val parentObjectInfo: String )

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

////@Serializable
class XyDocumentConfig(
    val name: String, val descr: String,
    val serverClassName: String,
    val clientType: XyDocumentClientType,
    val itScaleAlign: Boolean,
    val alElementConfig: List<Pair<String, XyElementConfig>> ) {

    fun minScale() = alElementConfig.minBy { it.second.scaleMin }!!.second.scaleMin
    fun maxScale() = alElementConfig.maxBy { it.second.scaleMax }!!.second.scaleMax
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

////@Serializable
class XyElementConfig(
    val name: String,
    val clientType: XyElementClientType,
    val layer: Int, val scaleMin: Int, val scaleMax: Int,
    val descrForAction: String,
//    val itCloseable: Boolean, - нафига???
    val itRotatable: Boolean,
    val itMoveable: Boolean,
//    val itCopyable: Boolean,
    val itEditablePoint: Boolean
//    val itEditableText: Boolean,
)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

enum class XyDocumentClientType {
    MAP,
    STATE
}

enum class XyElementClientType {
    BITMAP,
    ICON,
    MARKER,
    POLY,
    TEXT,
    TRACE,
    ZONE
}

