package foatto.core.app.graphic

//import kotlinx.serialization.Serializable

//@Serializable
class GraphicElement(
    val graphicTitle: String,
    val alIndexColor: Array<Pair<GraphicColorIndex,Int>>,
    val graphicHeight: Double,
    val alAxisYData: Array<AxisYData>,
    val alGDC: Array<GraphicDataContainer>
)

//@Serializable
class AxisYData( val title: String, val min: Double, val max: Double, val colorIndex: GraphicColorIndex )