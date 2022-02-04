package foatto.core_server.app.system

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mUserRole : mAbstract() {

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SYSTEM_user_role"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnRoleID = ColumnInt("SYSTEM_role", "id")
        val columnRole = ColumnInt(modelTableName, "role_id", columnRoleID)
        val columnRoleName = ColumnString("SYSTEM_role", "name", "Наименование роли", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            selectorAlias = "system_role"
            addSelectorColumn(columnRole, columnRoleID)
            addSelectorColumn(this)
        }

        val columnLoginID = ColumnInt("SYSTEM_users", "id")
        val columnLogin = ColumnInt(modelTableName, "user_id", columnLoginID)
        val columnLoginName = ColumnString("SYSTEM_users", "full_name", "Пользователь", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            selectorAlias = "system_user_people"
            addSelectorColumn(columnLogin, columnLoginID)
            addSelectorColumn(this)
        }

        //----------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnRole)
        alTableHiddenColumn.add(columnLogin)

        alTableGroupColumn.add(columnRoleName)

        addTableColumn(columnLoginName)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnRole)
        alFormHiddenColumn.add(columnLogin)

        alFormColumn.add(columnRoleName)
        alFormColumn.add(columnLoginName)

        //---------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnRoleName)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnLoginName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_role"] = columnRole
        hmParentColumn["system_user"] = columnLogin
        hmParentColumn["system_user_people"] = columnLogin
        hmParentColumn["system_user_division"] = columnLogin
    }

}
