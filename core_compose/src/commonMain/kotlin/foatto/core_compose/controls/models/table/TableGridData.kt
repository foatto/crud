package foatto.core_compose.controls.models.table

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foatto.core.link.TableCellType
import foatto.core_compose.models.MenuDataClient

class TableGridData(
    val cellType: TableCellType,

    val backColor: Color,
    val textColor: Color,

    val align: Alignment,
    val isBoldText: Boolean,

    val tooltip: String,

    val colSpan: Int,

    //--- CHECKBOX
    val booleanValue: Boolean? = null,

    //--- TEXT
    val textCellData: TableCellDataClient? = null,

    //--- BUTTON
    val alButtonCellData: List<TableCellDataClient>? = null,

    //--- общие данные для BUTTON
    val alGridCellData: List<List<TableCellDataClient>>? = null,

    //--- для работы с row data popup menu
    val dataRow: Int,

    //var minWidth = 0 - not used yet
) {
    var componentWidth: Dp by mutableStateOf(0.dp)

    var alCurrentPopupData by mutableStateOf<List<MenuDataClient>?>(null)
    var isShowPopupMenu by mutableStateOf(false)

}