package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.system.form.cells.FormDateTimeCell

class FormDateTimeCellClient(
    id: Int,
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormDateTimeCell,
) : FormBaseCellClient(
    id = id,
    cellName = cellName,
    align = align,
    isVisible = isVisible,
) {
    val current: List<MutableState<String>> = data.value.map { s -> mutableStateOf(s) }
}