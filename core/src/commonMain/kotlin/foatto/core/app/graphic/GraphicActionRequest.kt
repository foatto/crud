package foatto.core.app.graphic

class GraphicActionRequest(
    val documentTypeName: String,
    val action: GraphicAction,
    val startParamID: String,

    val graphicCoords: Pair<Int, Int>? = null,
    val viewSize: Pair<Int, Int>? = null
) {
    var sessionID: Long = 0
}

enum class GraphicAction { GET_COORDS, GET_ELEMENTS }
