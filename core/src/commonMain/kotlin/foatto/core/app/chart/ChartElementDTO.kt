package foatto.core.app.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElementDTO(
    val type: ChartElementTypeDTO,
    val axisYIndex: Int,
    val lineWidth: Int = 0,
    val isReversedY: Boolean,
) {
    var alGBD: List<ChartElementBackDTO> = listOf()
    var alGLD: List<ChartElementLineDTO> = listOf()
    var alGTD: List<ChartElementTextDTO> = listOf()

    fun isNotEmpty(): Boolean = alGBD.isNotEmpty() || alGLD.isNotEmpty() || alGTD.isNotEmpty()
}
