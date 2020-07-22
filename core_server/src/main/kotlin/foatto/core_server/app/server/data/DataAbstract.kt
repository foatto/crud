package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedStatement

abstract class DataAbstract(aColumn: iColumn) : iData, Cloneable {

    override val column: iColumn = aColumn

    //--- показывать пустышку вместо ячейки таблицы
    override var isShowEmptyTableCell = false
    override var errorText: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun preSave(rootDirName: String, stm: CoreAdvancedStatement) {}
    override fun preDelete(rootDirName: String, stm: CoreAdvancedStatement) {}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected open fun getFieldCellName(index: Int): String = getFieldName(index).replace(".", "___")

    protected fun getFieldName(index: Int): String = "${column.tableName}.${column.alFieldName[index]}"

    override fun clone(): Any = super.clone()
}
