package foatto.core.model.response.table

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class AddActionButton(
    val icon: String?,
    val text: String?,
    val tooltip: String?,
    val action: AppAction,
)
