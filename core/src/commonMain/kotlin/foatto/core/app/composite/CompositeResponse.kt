package foatto.core.app.composite

import kotlinx.serialization.Serializable

@Serializable
class CompositeResponse(
    val title: String,
    val cols: Int,
    val rows: Int,
    val blocks: List<CompositeBlockDTO>,
)
