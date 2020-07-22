package foatto.core.app.graphic

////import kotlinx.serialization.Serializable

////@Serializable
class GraphicActionResponse(
    //--- ответ на GET_COORDS
    val begTime: Int? = null,
    val endTime: Int? = null,

    //--- ответ на GET_ELEMENTS
    val alElement: List<Pair<String, GraphicElement>>? = null,
    val alVisibleElement: List<Pair<String, String>>? = null
)

