package foatto.core.app.graphic

class GraphicElement(
    val graphicTitle: String,
    val alIndexColor: Array<Pair<GraphicColorIndex, Int>>,
    val graphicHeight: Double,
    val alAxisYData: Array<AxisYData>,
    var alGDC: Array<GraphicDataContainer>,
)

class AxisYData(
    val title: String,
    var min: Double,
    var max: Double,
    val colorIndex: GraphicColorIndex,
    val itReversedY: Boolean,
)