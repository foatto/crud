package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

class DataBinary(aColumn: iColumn) : DataAbstract(aColumn) {

    lateinit var binaryValue: AdvancedByteBuffer
        private set

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        binaryValue = rs.getByteBuffer(posRS++)
        return posRS
    }

    override fun loadFromDefault() {
        binaryValue = AdvancedByteBuffer(0)
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameId: String?, id: Int): Boolean = true

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        val tc = TableCell(row, col, column.rowSpan, column.colSpan)
        tc.minWidth = column.minWidth
        return tc
    }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char) =
        FormCell(FormCellType.STRING).apply {
            name = getFieldCellName(0)
            value = binaryValue.getHex(null, false).toString()
            column = 16
            itPassword = false
        }

    override fun getFieldSQLValue(index: Int): String = ""

    override fun setData(data: iData) {
        binaryValue = (data as DataBinary).binaryValue
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    //--- перекрыто для нормальной работы HashMap.get при передаче описаний полей/столбцов между модулями -------------
//
//    override fun hashCode(): Int = value.hashCode()
//
//    override fun equals( other: Any? ): Boolean {
//        if( super.equals( other ) ) return true  // if( this == obj ) return true;
//        if( other == null ) return false
//        if( other !is DataBinary ) return false
//
//        return value == other.value
//    }
}
