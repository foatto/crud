package foatto.compose.control.model.table.cell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foatto.compose.model.MenuDataClient

abstract class TableBaseCellClient(
    val colSpan: Int,
    val dataRow: Int?,
    val minWidth: Int,
    val align: Alignment,
    val backColor: Color,
    val textColor: Color,
    val isBoldText: Boolean,
) {
    var componentWidth: Dp by mutableStateOf(0.dp)

    var alCurrentPopupData by mutableStateOf<List<MenuDataClient>?>(null)
    var isShowPopupMenu by mutableStateOf(false)
}
