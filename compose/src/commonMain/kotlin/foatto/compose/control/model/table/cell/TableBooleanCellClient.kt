package foatto.compose.control.model.table.cell

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class TableBooleanCellClient(
    colSpan: Int,
    dataRow: Int?,
    minWidth: Int,
    align: Alignment,
    backColor: Color,
    textColor: Color,
    isBoldText: Boolean,

    val value: Boolean,
) : TableBaseCellClient(
    colSpan = colSpan,
    dataRow = dataRow,
    minWidth = minWidth,
    align = align,
    backColor = backColor,
    textColor = textColor,
    isBoldText = isBoldText,
)