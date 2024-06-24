package foatto.compose.control.model.table.cell

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

class TableBooleanCellClient(
    colSpan: Int,
    dataRow: Int?,
    align: Alignment,
    backColor: Color,
    textColor: Color,
    isBoldText: Boolean,
    tooltip: String,

    val value: Boolean,
) : TableBaseCellClient(
    colSpan = colSpan,
    dataRow = dataRow,
    align = align,
    backColor = backColor,
    textColor = textColor,
    isBoldText = isBoldText,
    tooltip = tooltip,
)