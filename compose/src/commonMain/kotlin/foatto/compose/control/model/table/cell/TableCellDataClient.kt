package foatto.compose.control.model.table.cell

import foatto.core.model.AppAction

class TableCellDataClient(
    val icon: String? = null,
    val image: String? = null,
    val text: String? = null,
    val action: AppAction? = null,
    val inNewTab: Boolean = false,
)