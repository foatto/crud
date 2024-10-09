package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.form.cells.FormBooleanCell

class FormBooleanCellClient(
    cellName: String,
    minWidth: Int,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormBooleanCell,
) : FormBaseCellClient(
    cellName = cellName,
    minWidth = minWidth,
    align = align,
    isVisible = isVisible,
) {
    val current: MutableState<Boolean> = mutableStateOf(data.value)
}