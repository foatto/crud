package foatto.core.model.response.system.table.cells

import kotlinx.serialization.Serializable

@Serializable
class TableGridCell(
    override val row: Int,
    override val col: Int,
    override val rowSpan: Int = 1,
    override val colSpan: Int = 1,
    override val dataRow: Int,
    override val tooltip: String,
    override val minWidth: Int = 0,
    override val align: TableCellAlign = TableCellAlign.LEFT,
    override val backColor: Int? = null,
    override val backColorType: TableCellBackColorType = TableCellBackColorType.DEFAULT,
    override val foreColor: Int? = null,
    override val fontStyle: Int = 0,

    val values: List<List<TableGridCellData>>,
    val isWordWrap: Boolean = true,
) : TableBaseCell()

/*
    //--- add a cell of GRID
    fun addGridCellData(
        aIcon: String = "",
        aImage: String = "",
        aText: String = "",
        aNewRow: Boolean = false,
    ) {
        if (aNewRow) {
            alGridCellData = alGridCellData.toMutableList().apply {
                add(listOf())
            }
        }
        alGridCellData[alGridCellData.lastIndex] = alGridCellData[alGridCellData.lastIndex].toMutableList().apply {
            add(
                TableGridCellData(
                    icon = aIcon,
                    image = aImage,
                    text = aText,
                )
            )
        }
    }
 */