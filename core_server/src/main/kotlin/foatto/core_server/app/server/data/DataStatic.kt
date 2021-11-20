package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core_server.app.server.column.ColumnStatic
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

class DataStatic(aColumn: iColumn) : DataAbstract(aColumn) {

    override val fieldSQLCount: Int
        get() = 0

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int) = 0

    override fun loadFromDefault() {}
    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String?, id: Int) = true

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell =
        if (isShowEmptyTableCell) TableCell(row, col, column.rowSpan, column.colSpan)
        else TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aAlign = column.tableAlign,
            aMinWidth = column.minWidth,
            aIsWordWrap = column.isWordWrap,
            aTooltip = "",

            aText = (column as ColumnStatic).staticValue
        )

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char) = FormCell(FormCellType.STRING)

    override fun getFieldSQLValue(index: Int): String = ""

    override fun setData(data: iData) {}
}
