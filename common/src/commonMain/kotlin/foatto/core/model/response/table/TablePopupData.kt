package foatto.core.model.response.table

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class TablePopupData(
    val group: String? = null,
    val action: AppAction?,
    val text: String,
    val inNewTab: Boolean
)
