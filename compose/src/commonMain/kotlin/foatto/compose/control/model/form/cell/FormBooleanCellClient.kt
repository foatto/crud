package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.system.form.cells.FormBooleanCell

class FormBooleanCellClient(
    id: Int,
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormBooleanCell,
) : FormBaseCellClient(
    id = id,
    cellName = cellName,
    align = align,
    isVisible = isVisible,
) {
    val current: MutableState<Boolean> = mutableStateOf(data.value)
}