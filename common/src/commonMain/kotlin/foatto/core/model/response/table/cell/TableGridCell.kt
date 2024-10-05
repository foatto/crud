package foatto.core.model.response.table.cell

import kotlinx.serialization.Serializable

@Serializable
class TableGridCell(
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

    val values: List<List<String>>,
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