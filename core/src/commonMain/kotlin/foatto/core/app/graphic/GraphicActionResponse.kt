package foatto.core.app.graphic

class GraphicActionResponse(
    //--- answer on GET_COORDS
    val begTime: Int? = null,
    val endTime: Int? = null,

    //--- answer on GET_ELEMENTS
    val alIndexColor: Array<Pair<GraphicColorIndex, Int>> = emptyArray(),
    val alElement: Array<Pair<String, GraphicElement>> = emptyArray(),
    val alVisibleElement: Array<Pair<String, String>> = emptyArray(),
    val alLegend: Array<Triple<Int, Boolean, String>> = emptyArray(),   // color-index, is-back, descr
)

