package foatto.core.app.composite

import foatto.core.link.ChartResponse
import foatto.core.link.XyResponse
import kotlinx.serialization.Serializable

@Serializable
class CompositeBlockDTO(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,

    val xyResponse: XyResponse? = null,
    val chartResponse: ChartResponse? = null,
)