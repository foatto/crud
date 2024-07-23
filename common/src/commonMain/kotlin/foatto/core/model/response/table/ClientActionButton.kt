package foatto.core.model.response.table

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class ClientActionButton(
    val icon: String?,
    val text: String?,
    val tooltip: String?,
    val action: AppAction,
    val alParam: List<Pair<String, String>>,
    val isForWideScreenOnly: Boolean,
)
