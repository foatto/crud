package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class FormLabelCellClient(
    cellName: String,
    minWidth: Int,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val caption: String,
) : FormBaseCellClient(
    cellName = cellName,
    minWidth = minWidth,
    align = align,
    isVisible = isVisible,
) {
    val current: MutableState<String> = mutableStateOf(caption)
}