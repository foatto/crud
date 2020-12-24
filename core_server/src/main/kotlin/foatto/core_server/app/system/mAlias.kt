package foatto.core_server.app.system

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mAlias : mAbstract() {

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SYSTEM_alias"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnClassAlias = ColumnString(tableName, "name", "Алиас", STRING_COLUMN_WIDTH)
        columnClassAlias.isRequired = true
        columnClassAlias.setUnique(true, null)
        val columnClassControlName = ColumnString(tableName, "control_name", "Имя control-класса", STRING_COLUMN_WIDTH)
        columnClassControlName.isRequired = true
        val columnClassModelName = ColumnString(tableName, "model_name", "Имя model-класса", STRING_COLUMN_WIDTH)
        columnClassModelName.isRequired = true
        val columnClassDescr = ColumnString(tableName, "descr", "Описание", STRING_COLUMN_WIDTH)
        columnClassDescr.isRequired = true
        val columnClassAuthorization = ColumnBoolean(tableName, "authorization_need", "Авторизация", false)
        val columnClassShowRowNo = ColumnBoolean(tableName, "show_row_no", "Столбец с номером строки", false)
        val columnClassShowUserColumn = ColumnBoolean(tableName, "show_user_column", "Столбец с именем пользователя", false)
        val columnClassPageSize = ColumnInt(tableName, "table_page_size", "Размер страницы", 3, 0)
        val columnClassNewable = ColumnBoolean(tableName, "newable", "Отметка новых строк", false)
        val columnClassNewAutoRead = ColumnBoolean(tableName, "new_auto_read", "Автопрочитка новых строк", false)
        val columnClassDefaultParentUser = ColumnBoolean(tableName, "default_parent_user", "Фильтр на текущего пользователя по умолчанию", false)

        alTableHiddenColumn.add(columnID!!)

        addTableColumn(columnClassAlias)
        addTableColumn(columnClassControlName)
        addTableColumn(columnClassModelName)
        addTableColumn(columnClassDescr)
        addTableColumn(columnClassAuthorization)
        addTableColumn(columnClassShowRowNo)
        addTableColumn(columnClassShowUserColumn)
        addTableColumn(columnClassPageSize)
        addTableColumn(columnClassNewable)
        addTableColumn(columnClassNewAutoRead)
        addTableColumn(columnClassDefaultParentUser)

        alFormHiddenColumn.add(columnID!!)

        alFormColumn.add(columnClassAlias)
        alFormColumn.add(columnClassControlName)
        alFormColumn.add(columnClassModelName)
        alFormColumn.add(columnClassDescr)
        alFormColumn.add(columnClassAuthorization)
        alFormColumn.add(columnClassShowRowNo)
        alFormColumn.add(columnClassShowUserColumn)
        alFormColumn.add(columnClassPageSize)
        alFormColumn.add(columnClassNewable)
        alFormColumn.add(columnClassNewAutoRead)
        alFormColumn.add(columnClassDefaultParentUser)

        //---------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnClassAlias)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("system_role_permission", columnID!!))
        alChildData.add(ChildData("system_permission", columnID!!))

        //----------------------------------------------------------------------------------------

        //--- иначе система не дойдет до автоматического стирания cPermission & cRolePermission
        //        alDependTable.add( "SYSTEM_permission" );
        //        alDependFieldID.add( "class_id" );
    }
}
