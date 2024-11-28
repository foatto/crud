package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.response.form.cells.FormDateTimeCell

class FormDateTimeCellClient(
    cellName: String,
    minWidth: Int,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormDateTimeCell,
) : FormBaseCellClient(
    cellName = cellName,
    minWidth = minWidth,
    align = align,
    isVisible = isVisible,
) {
    val current: MutableState<Int?> = mutableStateOf(data.value)
}