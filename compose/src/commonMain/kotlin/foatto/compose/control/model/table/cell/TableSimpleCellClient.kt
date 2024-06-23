package foatto.compose.control.model.table.cell

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import foatto.core.model.response.system.table.cell.TableSimpleCell

class TableSimpleCellClient(
    dataRow: Int?,
    backColor: Color,
    align: Alignment,
    colSpan: Int,
    val data: TableSimpleCell,
) : TableBaseCellClient(
    dataRow = dataRow,
    backColor = backColor,
    align = align,
    colSpan = colSpan,
)