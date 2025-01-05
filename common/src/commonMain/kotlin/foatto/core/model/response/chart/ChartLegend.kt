package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartLegend(
    val colorIndex: ChartColorIndex,
    val isBack: Boolean,
    val descr: String,
)