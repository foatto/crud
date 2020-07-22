package foatto.core.app.graphic

////import kotlinx.serialization.Serializable

////@Serializable
class GraphicActionResponse(
    //--- ответ на GET_COORDS
    val begTime: Int? = null,
    val endTime: Int? = null,

    //--- ответ на GET_ELEMENTS
    val alElement: Array<Pair<String, GraphicElement>>? = null,
    val alVisibleElement: Array<Pair<String, String>>? = null
)

