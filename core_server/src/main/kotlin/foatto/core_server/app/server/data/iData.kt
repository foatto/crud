package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

interface iData {

    val column: iColumn

    //--- показывать пустышку вместо ячейки таблицы
    var isShowEmptyTableCell: Boolean
    val fieldSQLCount: Int

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int
    fun loadFromDefault()
    fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String?, id: Int): Boolean

    fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell

    fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell

    fun getFieldSQLValue(index: Int): String

    fun preSave(rootDirName: String, stm: CoreAdvancedStatement)

    fun preDelete(rootDirName: String, stm: CoreAdvancedStatement)

    fun setData(data: iData)

    fun getError(): String?

    fun clone(): Any
}
