package foatto.core.app.graphic

class GraphicActionResponse(
    //--- answer on GET_COORDS
    val begTime: Int? = null,
    val endTime: Int? = null,

    //--- answer on GET_ELEMENTS
    val alElement: Array<Pair<String, GraphicElement>>? = null,
    val alVisibleElement: Array<Pair<String, String>>? = null
)

