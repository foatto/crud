package foatto.compose.control.model.table.cell

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import foatto.core.model.response.system.table.cell.TableButtonCell

class TableButtonCellClient(
    dataRow: Int?,
    backColor: Color,
    align: Alignment,
    colSpan: Int,
    val data: TableButtonCell,
) : TableBaseCellClient(
    dataRow = dataRow,
    backColor = backColor,
    align = align,
    colSpan = colSpan,
)
