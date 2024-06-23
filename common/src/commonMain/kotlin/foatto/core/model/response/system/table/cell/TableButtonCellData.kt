package foatto.core.model.response.system.table.cell

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class TableButtonCellData(
    val iconName: String? = null,
    val imageUrl: String? = null,
    val text: String? = null,
    val action: AppAction? = null,
    val inNewTab: Boolean = false,
)
