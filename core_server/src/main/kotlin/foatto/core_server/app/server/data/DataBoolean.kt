package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

class DataBoolean(aColumn: iColumn) : DataAbstract(aColumn) {

    private val cb = column as ColumnBoolean

    var value = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        value = rs.getInt(posRS++) != 0
        return posRS
    }

    override fun loadFromDefault() {
        value = validate(cb.defaultValue)
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameId: String?, id: Int): Boolean {
        value = formData.booleanValue!!
        return true
    }

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell =
        if(isShowEmptyTableCell) {
            TableCell(row, col, column.rowSpan, column.colSpan)
        }
        else TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aAlign = column.tableAlign,
            aMinWidth = column.minWidth,
            aTooltip = column.caption,

            aBooleanValue = value
        )

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char) = FormCell(
        aBooleanName = getFieldCellName(0),
        aBooleanValue = value,
        aArrSwitch = cb.arrSwitchText,
    )

    override fun getFieldSQLValue(index: Int): String = if(value) {
        "1"
    } else {
        "0"
    }

    override fun setData(data: iData) {
        value = (data as DataBoolean).value
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun validate(obj: Any?): Boolean {
        if(obj == null) {
            return false
        }
        return if(obj is Number) {
            obj.toInt() != 0
        }
        else {
            obj as? Boolean ?: false
        }
    }
}
