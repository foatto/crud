package foatto.core.model.response.composite

import foatto.core.model.response.HeaderData
import kotlinx.serialization.Serializable

@Serializable
class CompositeResponse(
    val tabCaption: String,
    val headerData: HeaderData,

    val cols: Int,
    val rows: Int,

    val blocks: List<CompositeBlock>,
)
