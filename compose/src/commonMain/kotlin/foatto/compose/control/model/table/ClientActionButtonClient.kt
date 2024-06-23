package foatto.compose.control.model.table

import foatto.core.model.AppAction

class ClientActionButtonClient(
    val name: String,
    val tooltip: String? = null,
    val action: AppAction,
    val params: List<Pair<String, String>>,
    val isForWideScreenOnly: Boolean,
)