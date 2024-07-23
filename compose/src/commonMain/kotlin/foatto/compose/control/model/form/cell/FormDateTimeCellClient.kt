package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.form.cells.FormDateTimeCell

class FormDateTimeCellClient(
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    focusId: Int,
    val data: FormDateTimeCell,
) : FormBaseCellClient(
    cellName = cellName,
    align = align,
    isVisible = isVisible,
    focusId = focusId,
) {
    val current: List<MutableState<String>> = data.value.map { s -> mutableStateOf(s) }
}