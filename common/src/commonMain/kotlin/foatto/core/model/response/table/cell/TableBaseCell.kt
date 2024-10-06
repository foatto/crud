package foatto.core.model.response.table.cell

import kotlinx.serialization.Serializable

@Serializable
sealed class TableBaseCell {
    abstract val row: Int
    abstract val col: Int
    abstract val colSpan: Int
    abstract val dataRow: Int
    abstract val minWidth: Int
    abstract val align: TableCellAlign
    abstract val backColor: Int?
    abstract val backColorType: TableCellBackColorType
    abstract val foreColor: Int?
    abstract val isBoldText: Boolean
}
