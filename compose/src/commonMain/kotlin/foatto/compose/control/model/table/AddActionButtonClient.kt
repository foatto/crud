package foatto.compose.control.model.table

import foatto.core.model.AppAction

class AddActionButtonClient(
    val name: String,
    val tooltip: String? = null,
    val action: AppAction,
)