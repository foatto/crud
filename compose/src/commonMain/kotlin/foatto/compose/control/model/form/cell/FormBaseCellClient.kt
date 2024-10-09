package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

abstract class FormBaseCellClient(
    val cellName: String,
    val minWidth: Int,
    val align: Arrangement.Horizontal,
    val isVisible: MutableState<Boolean> = mutableStateOf(true),
) {
    val alSubFocusId: MutableList<Int> = mutableListOf()
    var error: String? by mutableStateOf(null)

    var componentWidth: Dp by mutableStateOf(0.dp)
}