package foatto.core.app.graphic

import kotlinx.serialization.Serializable

@Serializable
class GraphicElement(
    val graphicTitle: String,
    val alLegend: Array<Triple<Int, Boolean, String>>,   // color-index, is-back, descr
    val graphicHeight: Double,
    val alAxisYData: Array<AxisYData>,
    var alGDC: Array<GraphicDataContainer>,
)

@Serializable
class AxisYData(
    val title: String,
    var min: Double,
    var max: Double,
    val colorIndex: GraphicColorIndex,
    val itReversedY: Boolean,
) {
    //--- set on client-side
    var prec: Int = 0
}