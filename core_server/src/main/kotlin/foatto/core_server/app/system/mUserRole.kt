@file:JvmName("mUserRole")
package foatto.core_server.app.system

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mUserRole : mAbstract() {

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SYSTEM_user_role"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnRoleID = ColumnInt("SYSTEM_role", "id")
        val columnRole = ColumnInt(tableName, "role_id", columnRoleID)
        val columnRoleName = ColumnString("SYSTEM_role", "name", "Наименование роли", STRING_COLUMN_WIDTH)
        columnRoleName.isRequired = true

        val columnLoginID = ColumnInt("SYSTEM_users", "id")
        val columnLogin = ColumnInt(tableName, "user_id", columnLoginID)
        val columnLoginName = ColumnString("SYSTEM_users", "full_name", "Пользователь", STRING_COLUMN_WIDTH)
        columnLoginName.isRequired = true

        columnRoleName.selectorAlias = "system_role"
        columnRoleName.addSelectorColumn(columnRole, columnRoleID)
        columnRoleName.addSelectorColumn(columnRoleName)

        columnLoginName.selectorAlias = "system_user_people"
        columnLoginName.addSelectorColumn(columnLogin, columnLoginID)
        columnLoginName.addSelectorColumn(columnLoginName)

        //----------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnRole)
        alTableHiddenColumn.add(columnLogin)

        alTableGroupColumn.add(columnRoleName)

        addTableColumn(columnLoginName)

        alFormHiddenColumn.add(columnID!!)
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
