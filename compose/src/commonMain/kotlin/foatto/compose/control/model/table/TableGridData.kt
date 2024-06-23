package foatto.compose.control.model.table

import androidx.compose.ui.graphics.Color

class TableGridData(
//    val cellType: TableCellType,

//    val backColor: Color,
    val textColor: Color,

//    val align: Alignment,
    val isBoldText: Boolean,

    val tooltip: String,

//    val colSpan: Int,

    //--- CHECKBOX
//    val booleanValue: Boolean? = null,

    //--- TEXT
    val textCellData: TableCellDataClient? = null,

    //--- BUTTON
    val alButtonCellData: List<TableCellDataClient>? = null,

    //--- общие данные для BUTTON
    val alGridCellData: List<List<TableCellDataClient>>? = null,

//    //--- для работы с row data popup menu
//    val dataRow: Int,

    //var minWidth = 0 - not used yet
)