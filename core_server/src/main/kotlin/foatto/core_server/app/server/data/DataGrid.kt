package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.link.TableCellType
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

class DataGrid(aColumn: iColumn) : DataAbstract(aColumn) {

//    private val cg = column as ColumnGrid

    private var alData = mutableListOf<MutableList<String>>()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int = aPosRS

    override fun loadFromDefault() {}

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameId: String?, id: Int) = true

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        if (isShowEmptyTableCell) {
            return TableCell(row, col, column.rowSpan, column.colSpan)
        }

        val tc = TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aAlign = column.tableAlign,
            aMinWidth = 0,
            aTooltip = column.caption,
            aCellType = TableCellType.GRID,
        )

        alData.forEach { arrRow ->
            arrRow.forEachIndexed { index, data ->
                tc.addGridCellData(
                    aText = data,
                    aNewRow = index == 0
                )
            }
        }

        return tc
    }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char) = FormCell(FormCellType.STRING)

    override fun getFieldSQLValue(index: Int) = ""

    override fun setData(data: iData) {
        alData = (data as DataGrid).alData
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun addRowData(vararg arrRowData: String) {
        alData.add(arrRowData.toMutableList())
    }
}
