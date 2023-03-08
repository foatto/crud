package foatto.core.app.graphic

class GraphicActionRequest(
    val documentTypeName: String,
    val action: GraphicAction,
    val startParamId: String,

    val graphicCoords: Pair<Int, Int>? = null,
    val viewSize: Pair<Int, Int>? = null
) {
    var sessionId: Long = 0
}

enum class GraphicAction { GET_ELEMENTS }
