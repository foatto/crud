package foatto.compose.control.model.table.cell

import foatto.core.model.AppAction

class TableCellDataClient(
    val name: String,
    val action: AppAction? = null,
    val inNewTab: Boolean = false,
)