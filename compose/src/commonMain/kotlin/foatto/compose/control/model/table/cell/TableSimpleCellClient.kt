package foatto.compose.control.model.table.cell

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class TableSimpleCellClient(
    colSpan: Int,
    dataRow: Int?,
    minWidth: Int,
    align: Alignment,
    backColor: Color,
    textColor: Color,
    isBoldText: Boolean,

    val data: TableCellDataClient,
) : TableBaseCellClient(
    colSpan = colSpan,
    dataRow = dataRow,
    minWidth = minWidth,
    align = align,
    backColor = backColor,
    textColor = textColor,
    isBoldText = isBoldText,
)