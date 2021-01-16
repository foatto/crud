package foatto.core.app.graphic

import foatto.core.app.xy.geom.XyPoint

class GraphicDataContainer(
    val type: ElementType,
    val axisYIndex: Int,
    val lineWidth: Int = 0,
) {
    enum class ElementType { LINE, POINT, TEXT }

    var alGLD: Array<GraphicLineData> = arrayOf()
    var alGPD: Array<GraphicPointData> = arrayOf()
    var alGTD: Array<GraphicTextData> = arrayOf()

    fun itNotEmpty() = alGLD.isNotEmpty() || alGPD.isNotEmpty() || alGTD.isNotEmpty()
}

class GraphicLineData(
    val x: Int,
    val y: Double,
    var colorIndex: GraphicColorIndex,
    val coord: XyPoint? = null
)

class GraphicPointData(
    val x: Int,
    val y: Double,
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

