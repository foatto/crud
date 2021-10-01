package foatto.core.app.graphic

import foatto.core.app.xy.geom.XyPoint

class GraphicDataContainer(
    val type: ElementType,
    val axisYIndex: Int,
    val lineWidth: Int = 0,
    val itReversedY: Boolean,
) {
    enum class ElementType { BACK, LINE, POINT, TEXT }

    var alGBD: Array<GraphicBackData> = arrayOf()
    var alGLD: Array<GraphicLineData> = arrayOf()
    var alGPD: Array<GraphicPointData> = arrayOf()
    var alGTD: Array<GraphicTextData> = arrayOf()

    fun itNotEmpty() = alGBD.isNotEmpty() || alGLD.isNotEmpty() || alGPD.isNotEmpty() || alGTD.isNotEmpty()
}

class GraphicBackData(
    val x1: Int,
    val x2: Int,
    val color: Int,
)

class GraphicLineData(
    val x: Int,
    var y: Double,
    var colorIndex: GraphicColorIndex,
    val coord: XyPoint? = null
)

class GraphicPointData(
    val x: Int,
    var y: Double,
    val colorIndex: GraphicColorIndex
)

class GraphicTextData(
    val textX1: Int,
    val textX2: Int,
    val fillColorIndex: GraphicColorIndex,
    val borderColorIndex: GraphicColorIndex,
    val textColorIndex: GraphicColorIndex,
    val text: String,
    val toolTip: String
)

