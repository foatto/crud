package foatto.core.app.graphic

import foatto.core.app.xy.geom.XyPoint

//import kotlinx.serialization.Serializable

//@Serializable
class GraphicDataContainer( val type: ElementType,
                            val axisYIndex: Int,
                            val lineWidth: Int = 0  // только для GraphicLineData
//                            val alGLD: Array<GraphicLineData>,
//                            val alGPD: Array<GraphicPointData>,
//                            val alGTD: Array<GraphicTextData>

) {

    enum class ElementType { LINE, POINT, TEXT }

    lateinit var alGLD: Array<GraphicLineData>
    lateinit var alGPD: Array<GraphicPointData>
    lateinit var alGTD: Array<GraphicTextData>

    fun itNotEmpty() = alGLD.isNotEmpty() || alGPD.isNotEmpty() || alGTD.isNotEmpty()
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//@Serializable
class GraphicLineData( val x: Int, val y: Double, val colorIndex: GraphicColorIndex, val coord: XyPoint? = null )
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
