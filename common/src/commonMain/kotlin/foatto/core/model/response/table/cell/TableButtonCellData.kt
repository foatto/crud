package foatto.core.model.response.table.cell

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class TableButtonCellData(
    val name: String,
    val action: AppAction? = null,
    val inNewTab: Boolean = false,
)
