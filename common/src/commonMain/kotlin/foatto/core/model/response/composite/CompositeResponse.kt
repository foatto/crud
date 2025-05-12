package foatto.core.model.response.composite

import foatto.core.model.response.HeaderData
import kotlinx.serialization.Serializable

@Serializable
class CompositeResponse(
    val tabCaption: String,
    val headerData: HeaderData,

    val blocks: List<CompositeBlock>,

    val layoutSaveKey: String,
)
