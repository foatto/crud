package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartAxisY(
    val title: String,
    var min: Float,
    var max: Float,
    val colorIndex: ChartColorIndex,
    val isReversedY: Boolean,
) {
    //--- set on client-side
    var prec: Int = 0
}