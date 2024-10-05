package foatto.core.model.response.table.cell

import kotlinx.serialization.Serializable

@Serializable
class TableSimpleCell(
    override val row: Int,
    override val col: Int,
    override val colSpan: Int = 1,
    override val dataRow: Int,
    override val minWidth: Int = 0,
    override val align: TableCellAlign = TableCellAlign.LEFT,
    override val backColor: Int? = null,
    override val backColorType: TableCellBackColorType = TableCellBackColorType.DEFAULT,
    override val foreColor: Int? = null,
    override val isBoldText: Boolean = false,

    val name: String,

    val isWordWrap: Boolean = true,
) : TableBaseCell()
//            if (cs.isPassword) {
//                "********"
//            } else if (text.isEmpty()) {
//                cs.emptyValueString
//            } else {
//                text
//            }
