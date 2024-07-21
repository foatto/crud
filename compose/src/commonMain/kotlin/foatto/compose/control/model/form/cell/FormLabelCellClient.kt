package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class FormLabelCellClient(
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val caption: String,
) : FormBaseCellClient(
    cellName = cellName,
    align = align,
    isVisible = isVisible,
    focusId = -1,
) {
    val current: MutableState<String> = mutableStateOf(caption)
}