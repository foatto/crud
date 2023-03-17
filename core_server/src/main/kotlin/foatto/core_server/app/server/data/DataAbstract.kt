package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection

abstract class DataAbstract(aColumn: iColumn) : iData, Cloneable {

    override val column: iColumn = aColumn

    //--- показывать пустышку вместо ячейки таблицы
    override var isShowEmptyTableCell = false

    protected var errorText: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun preSave(rootDirName: String, conn: CoreAdvancedConnection) {}
    override fun preDelete(rootDirName: String, conn: CoreAdvancedConnection) {}
    override fun getError(): String? = errorText

    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected open fun getFieldCellName(index: Int): String = getFieldName(index).replace(".", "___")

    protected fun getFieldName(index: Int): String = "${column.columnTableName}.${column.alFieldName[index]}"

    override fun clone(): Any = super.clone()
}
