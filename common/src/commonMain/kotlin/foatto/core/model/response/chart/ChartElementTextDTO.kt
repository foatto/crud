package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementTextDTO(
    val textX1: Int,
    val textX2: Int,
    val fillColorIndex: ChartColorIndex,
    val borderColorIndex: ChartColorIndex,
    val textColorIndex: ChartColorIndex,
    val text: String,
    val toolTip: String
)
