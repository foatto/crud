package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.server.column.iColumn

class DataBinary( aColumn: iColumn ) : DataAbstract( aColumn ) {

    lateinit var value: AdvancedByteBuffer
        private set

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int ): Int {
        var posRS = aPosRS
        value = rs.getByteBuffer( posRS++ )
        return posRS
    }

    override fun loadFromDefault() {
        value = AdvancedByteBuffer( 0 )
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int ): Boolean = true

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int): TableCell {
        val tc = TableCell( row, col, column.rowSpan, column.colSpan )
        tc.minWidth = column.minWidth
        return tc
    }

    override fun getFormCell( rootDirName: String, stm: CoreAdvancedStatement): FormCell {
        val fci = FormCell( FormCellType.STRING )
        fci.name = getFieldCellName( 0 )
        fci.value = value.getHex( null, false ).toString()
        fci.column = 16
        fci.itPassword = false

        return fci
    }

    override fun getFieldSQLValue(index: Int): String = ""

    override fun setData(data: iData ) {
        value = ( data as DataBinary ).value
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
