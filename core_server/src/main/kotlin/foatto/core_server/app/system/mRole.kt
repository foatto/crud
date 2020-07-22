package foatto.core_server.app.system

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mRole : mAbstract() {

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SYSTEM_role"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnRoleName = ColumnString(tableName, "name", "Наименование", STRING_COLUMN_WIDTH)
        columnRoleName.isRequired = true
        columnRoleName.setUnique(true, null)

        //----------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)

        addTableColumn(columnRoleName)

        alFormHiddenColumn.add(columnID!!)

        alFormColumn.add(columnRoleName)

        //---------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnRoleName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("system_role_permission", columnID!!))
        alChildData.add(ChildData("system_user_role", columnID!!))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_role_permission", "role_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_user_role", "role_id"))
    }
}
