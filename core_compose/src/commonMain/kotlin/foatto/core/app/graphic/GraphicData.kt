package foatto.core.app.graphic

import foatto.core.app.xy.geom.XyPoint
import kotlinx.serialization.Serializable

@Serializable
class GraphicDataContainer(
    val type: ElementType,
    val axisYIndex: Int,
    val lineWidth: Int = 0,
    val itReversedY: Boolean,
) {
    @Serializable
    enum class ElementType { BACK, LINE, TEXT }

    var alGBD: Array<GraphicBackData> = arrayOf()
    var alGLD: Array<GraphicLineData> = arrayOf()
    var alGTD: Array<GraphicTextData> = arrayOf()

    fun itNotEmpty() = alGBD.isNotEmpty() || alGLD.isNotEmpty() || alGTD.isNotEmpty()
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

