package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartElement(
    val type: ChartElementType,
    val axisYIndex: Int,
    val lineWidth: Int = 0,
    val isReversedY: Boolean,
) {
    var backs: MutableList<ChartElementBack> = mutableListOf()
    var lines: MutableList<ChartElementLine> = mutableListOf()
    var texts: MutableList<ChartElementText> = mutableListOf()

    fun isNotEmpty(): Boolean = backs.isNotEmpty() || lines.isNotEmpty() || texts.isNotEmpty()
}
