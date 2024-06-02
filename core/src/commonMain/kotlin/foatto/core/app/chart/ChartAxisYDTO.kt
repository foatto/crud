package foatto.core.app.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartAxisYDTO(
    val title: String,
    var min: Float,
    var max: Float,
    val colorIndex: ChartColorIndex,
    val isReversedY: Boolean,
) {
    //--- set on client-side
    var prec: Int = 0
}