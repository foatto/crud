package foatto.core.app.graphic

import kotlinx.serialization.Serializable

@Serializable
class GraphicActionResponse(
    //--- answer on GET_COORDS
    val begTime: Int? = null,
    val endTime: Int? = null,

    //--- answer on GET_ELEMENTS
    val arrIndexColor: Array<Pair<GraphicColorIndex, Int>> = emptyArray(),
    val arrElement: Array<Pair<String, GraphicElement>> = emptyArray(),
    val arrVisibleElement: Array<Triple<String, String, Boolean>> = emptyArray(),
    val arrLegend: Array<Triple<Int, Boolean, String>> = emptyArray(),   // color-index, is-back, descr
)

