package foatto.core.app.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartActionRequest(
    val documentTypeName: String,
    val action: ChartAction,
    val startParamId: String,

    val graphicCoords: Pair<Int, Int>? = null,
    val viewSize: Pair<Int, Int>? = null
) {
    var sessionId: Long = 0
}

@Serializable
enum class ChartAction { GET_ELEMENTS }
