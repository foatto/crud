package foatto.compose.control.model.table.cell

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import foatto.core.model.response.system.table.cell.TableBooleanCell

class TableBooleanCellClient(
    dataRow: Int?,
    backColor: Color,
    align: Alignment,
    colSpan: Int,
    val data: TableBooleanCell,
) : TableBaseCellClient(
    dataRow = dataRow,
    backColor = backColor,
    align = align,
    colSpan = colSpan,
)
