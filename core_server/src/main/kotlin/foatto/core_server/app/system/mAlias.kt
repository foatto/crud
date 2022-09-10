package foatto.core_server.app.system

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection

class mAlias : mAbstract() {

    override fun init(
        application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SYSTEM_alias"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnClassAlias = ColumnString(modelTableName, "name", "Алиас", STRING_COLUMN_WIDTH)
        columnClassAlias.isRequired = true
        val columnClassControlName = ColumnString(modelTableName, "control_name", "Имя control-класса", STRING_COLUMN_WIDTH)
        columnClassControlName.isRequired = true
        val columnClassModelName = ColumnString(modelTableName, "model_name", "Имя model-класса", STRING_COLUMN_WIDTH)
        columnClassModelName.isRequired = true
        val columnClassDescr = ColumnString(modelTableName, "descr", "Описание", STRING_COLUMN_WIDTH)
        columnClassDescr.isRequired = true
        val columnClassAuthorization = ColumnBoolean(modelTableName, "authorization_need", "Авторизация", false)
        val columnClassShowRowNo = ColumnBoolean(modelTableName, "show_row_no", "Столбец с номером строки", false)
        val columnClassShowUserColumn = ColumnBoolean(modelTableName, "show_user_column", "Столбец с именем пользователя", false)
        val columnClassPageSize = ColumnInt(modelTableName, "table_page_size", "Размер страницы", 3, 0)
        val columnClassNewable = ColumnBoolean(modelTableName, "newable", "Отметка новых строк", false)
        val columnClassNewAutoRead = ColumnBoolean(modelTableName, "new_auto_read", "Автопрочитка новых строк", false)
        val columnClassDefaultParentUser = ColumnBoolean(modelTableName, "default_parent_user", "Фильтр на текущего пользователя по умолчанию", false)

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnClassAlias)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

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

        alFormHiddenColumn.add(columnId)

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

        addTableSort(columnClassAlias, true)

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("system_role_permission", columnId))
        alChildData.add(ChildData("system_permission", columnId))

        //----------------------------------------------------------------------------------------

        //--- иначе система не дойдет до автоматического стирания cPermission & cRolePermission
        //        alDependTable.add( "SYSTEM_permission" );
        //        alDependFieldID.add( "class_id" );
    }
}
