package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.column.iColumn

interface iData {

    val column: iColumn
    //--- показывать пустышку вместо ячейки таблицы
    var isShowEmptyTableCell: Boolean
    val errorText: String?
    val fieldSQLCount: Int

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int ): Int
    fun loadFromDefault()
    fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int ): Boolean

    fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int ): TableCell

    fun getFormCell( rootDirName: String, stm: CoreAdvancedStatement): FormCell

    fun getFieldSQLValue(index: Int ): String

    fun preSave( rootDirName: String, stm: CoreAdvancedStatement)

    fun preDelete( rootDirName: String, stm: CoreAdvancedStatement)

    fun setData(data: iData )

    fun clone(): Any
}
