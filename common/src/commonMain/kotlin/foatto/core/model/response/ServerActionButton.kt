package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class ServerActionButton(
    val icon: String?,
    val text: String?,
    val tooltip: String?,
    val action: AppAction,
    val inNewTab: Boolean,
    val isForWideScreenOnly: Boolean,
)
