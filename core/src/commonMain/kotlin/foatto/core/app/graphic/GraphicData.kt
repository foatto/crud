package foatto.core.app.graphic

import foatto.core.app.xy.geom.XyPoint
//import kotlinx.serialization.Serializable

//@Serializable
class GraphicDataContainer( val type: ElementType,
                            val axisYIndex: Int,
                            val lineWidth: Int = 0  // только для GraphicLineData
) {

    enum class ElementType { LINE, POINT, TEXT }

    val alGLD = mutableListOf<GraphicLineData>()
    val alGPD = mutableListOf<GraphicPointData>()
    val alGTD = mutableListOf<GraphicTextData>()

    fun itNotEmpty() = alGLD.isNotEmpty() || alGPD.isNotEmpty() || alGTD.isNotEmpty()
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//@Serializable
class GraphicLineData( val x: Int, val y: Double, var colorIndex: GraphicColorIndex, val coord: XyPoint? = null )
//@Serializable
class GraphicPointData( val x: Int, val y: Double, val colorIndex: GraphicColorIndex )
//@Serializable
class GraphicTextData(
    val textX1: Int,
    val textX2: Int,
    val fillColorIndex: GraphicColorIndex,
    val borderColorIndex: GraphicColorIndex,
    val textColorIndex: GraphicColorIndex,
    val text: String,
    val toolTip: String
)

