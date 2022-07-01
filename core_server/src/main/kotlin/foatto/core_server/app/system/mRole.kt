package foatto.core_server.app.system

import foatto.core_server.app.iApplication
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
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SYSTEM_role"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnRoleName = ColumnString(modelTableName, "name", "Наименование", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnRoleName)

        //----------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

        addTableColumn(columnRoleName)

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnRoleName)

        //---------------------------------------------------------------------

        addTableSort(columnRoleName, true)

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("system_role_permission", columnId))
        alChildData.add(ChildData("system_user_role", columnId))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_role_permission", "role_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_user_role", "role_id"))
    }
}
