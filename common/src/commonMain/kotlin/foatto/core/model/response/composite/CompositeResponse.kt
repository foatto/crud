package foatto.core.model.response.composite

import kotlinx.serialization.Serializable

@Serializable
class CompositeResponse(
    val tabCaption: String,
    val headers: List<String>,

    val cols: Int,
    val rows: Int,

    val blocks: List<CompositeBlock>,
)
