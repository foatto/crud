package foatto.core.app.graphic

import kotlinx.serialization.Serializable

@Serializable
class GraphicActionResponse(
    //--- answer on GET_ELEMENTS
    val hmIndexColor: Map<GraphicColorIndex, Int> = emptyMap(),
    //--- именно List - важен порядок элементов
    val alElement: List<Pair<String, GraphicElement>> = emptyList(),
    val alVisibleElement: List<Triple<String, String, Boolean>> = emptyList(),
    val alLegend: List<Triple<Int, Boolean, String>> = emptyList(),   // color-index, is-back, descr
)

