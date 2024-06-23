package foatto.core.app.composite

import foatto.core.link.ServerActionButton
import kotlinx.serialization.Serializable

@Serializable
class CompositeResponse(
    val title: String,
    val tooltip: String,

    val alServerActionButton: List<ServerActionButton>,

    val cols: Int,
    val rows: Int,

    val blocks: List<CompositeBlockDTO>,
)
