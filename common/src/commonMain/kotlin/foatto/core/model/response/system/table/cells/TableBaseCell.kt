package foatto.core.model.response.system.table.cells

import kotlinx.serialization.Serializable

@Serializable
sealed class TableBaseCell {
    abstract val row: Int
    abstract val col: Int
    abstract val rowSpan: Int
    abstract val colSpan: Int
    abstract val dataRow: Int
    abstract val tooltip: String
    abstract val minWidth: Int?
    abstract val align: TableCellAlign
    abstract val backColor: Int?
    abstract val backColorType: TableCellBackColorType
    abstract val foreColor: Int?
    abstract val fontStyle: Int
}
