package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.system.form.cells.FormComboCell

class FormComboCellClient(
    id: Int,
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormComboCell,
) : FormBaseCellClient(
    id = id,
    cellName = cellName,
    align = align,
    isVisible = isVisible,
) {
    val current: MutableState<String> = mutableStateOf(data.value)
}