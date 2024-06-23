package foatto.compose.control.model.table

import foatto.core.model.AppAction

class TableCellDataClient(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
    val action: AppAction? = null,
    val inNewTab: Boolean = false,
)