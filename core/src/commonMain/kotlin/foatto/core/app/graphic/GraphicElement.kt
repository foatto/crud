package foatto.core.app.graphic

//import kotlinx.serialization.Serializable

//@Serializable
class GraphicElement(
    val graphicTitle: String,
    val alIndexColor: List<Pair<GraphicColorIndex,Int>>,
    val graphicHeight: Double,
    val alAxisYData: List<AxisYData>,
    var alGDC: List<GraphicDataContainer>
)

//@Serializable
class AxisYData( val title: String, val min: Double, val max: Double, val colorIndex: GraphicColorIndex )