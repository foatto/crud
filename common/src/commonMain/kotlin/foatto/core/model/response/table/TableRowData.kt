package foatto.core.model.response.table

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class TableRowData(
    val formAction: AppAction? = null,
    val rowAction: AppAction? = null,
    val isRowUrlInNewTab: Boolean = false,
    val gotoAction: AppAction? = null,
    val isGotoUrlInNewTab: Boolean = false,
    val alPopupData: List<TablePopupData> = listOf()
)
