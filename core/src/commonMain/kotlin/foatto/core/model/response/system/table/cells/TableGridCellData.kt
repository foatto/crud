package foatto.core.model.response.system.table.cells

import kotlinx.serialization.Serializable

@Serializable
class TableGridCellData(
    val iconName: String? = null,
    val imageUrl: String? = null,
    val text: String? = null,
)
