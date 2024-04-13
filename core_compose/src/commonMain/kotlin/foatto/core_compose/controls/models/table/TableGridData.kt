package foatto.core_compose.controls.models.table

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import foatto.core.link.TableCellType

class TableGridData(
    val cellType: TableCellType,
//    val cellStyleCommon: StyleScope.() -> Unit,
//    val backColor: CSSColorValue,
//    val elementStyle: StyleScope.() -> Unit,
    val tooltip: String,

//    val cellStyleAdd: StyleScope.() -> Unit,

    //--- CHECKBOX
    val booleanValue: Boolean? = null,

    //--- TEXT
    val textCellData: TableTextCellDataClient? = null,

    //--- BUTTON
    val alButtonCellData: List<TableButtonCellDataClient>? = null,

    //--- общие данные для BUTTON
    val alGridCellData: List<List<TableGridCellDataClient>>? = null,

    //--- для работы с row data popup menu
    val dataRow: Int,

    //var minWidth = 0 - not used yet
) {
    var componentWidth by mutableStateOf(0.dp)
}