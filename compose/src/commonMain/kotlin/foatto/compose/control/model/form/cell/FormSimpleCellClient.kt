package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.system.form.cells.FormSimpleCell

class FormSimpleCellClient(
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    focusId: Int,
    val data: FormSimpleCell,
) : FormBaseCellClient(
    cellName = cellName,
    align = align,
    isVisible = isVisible,
    focusId = focusId,
) {
    var current: MutableState<String> = mutableStateOf(data.value)
}