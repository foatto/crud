package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.form.cells.FormBooleanCell

class FormBooleanCellClient(
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    focusId: Int,
    val data: FormBooleanCell,
) : FormBaseCellClient(
    cellName = cellName,
    align = align,
    isVisible = isVisible,
    focusId = focusId,
) {
    val current: MutableState<Boolean> = mutableStateOf(data.value)
}