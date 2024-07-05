package foatto.core.model.response.system.table.cell

import kotlinx.serialization.Serializable

@Serializable
class TableSimpleCell(
    override val row: Int,
    override val col: Int,
    override val colSpan: Int = 1,
    override val dataRow: Int,
    override val minWidth: Int = 0,
    override val align: TableCellAlign = TableCellAlign.CENTER,
    override val backColor: Int? = null,
    override val backColorType: TableCellBackColorType = TableCellBackColorType.DEFAULT,
    override val foreColor: Int? = null,
    override val fontStyle: Int = 0,

    val iconName: String? = null,
    val imageUrl: String? = null,
    val text: String? = null,

    val isWordWrap: Boolean = true,
) : TableBaseCell()
//            if (cs.isPassword) {
//                "********"
//            } else if (text.isEmpty()) {
//                cs.emptyValueString
//            } else {
//                text
//            }
