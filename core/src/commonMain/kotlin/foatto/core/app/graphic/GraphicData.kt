package foatto.core.app.graphic

import foatto.core.app.xy.geom.XyPoint
import kotlinx.serialization.Serializable

@Serializable
class GraphicDataContainer(
    val type: ElementType,
    val axisYIndex: Int,
    val lineWidth: Int = 0,
    val isReversedY: Boolean,
) {
    @Serializable
    enum class ElementType { BACK, LINE, TEXT }

    var alGBD: List<GraphicBackData> = listOf()
    var alGLD: List<GraphicLineData> = listOf()
    var alGTD: List<GraphicTextData> = listOf()

    fun isNotEmpty(): Boolean = alGBD.isNotEmpty() || alGLD.isNotEmpty() || alGTD.isNotEmpty()
}

@Serializable
class GraphicBackData(
    val x1: Int,
    val x2: Int,
    val color: Int,
)

@Serializable
class GraphicLineData(
    val x: Int,
    var y: Double,
    var colorIndex: GraphicColorIndex,
    val coord: XyPoint? = null
)

@Serializable
class GraphicTextData(
    val textX1: Int,
    val textX2: Int,
    val fillColorIndex: GraphicColorIndex,
    val borderColorIndex: GraphicColorIndex,
    val textColorIndex: GraphicColorIndex,
    val text: String,
    val toolTip: String
)

