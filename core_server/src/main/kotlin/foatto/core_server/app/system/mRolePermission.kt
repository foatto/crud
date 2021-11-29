package foatto.core_server.app.system

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mRolePermission : mAbstract() {

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SYSTEM_role_permission"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnRoleID = ColumnInt("SYSTEM_role", "id")
        val columnRole = ColumnInt(modelTableName, "role_id", columnRoleID)
        val columnRoleName = ColumnString("SYSTEM_role", "name", "Наименование роли", STRING_COLUMN_WIDTH)
        val columnPermissionID = ColumnInt("SYSTEM_permission", "id")
        val columnPermission = ColumnInt(modelTableName, "permission_id", columnPermissionID)
        val columnPermissionDescr = ColumnString("SYSTEM_permission", "descr", "Право доступа", STRING_COLUMN_WIDTH)
        val columnClassID = ColumnInt("SYSTEM_alias", "id")
        val columnClass = ColumnInt("SYSTEM_permission", "class_id", columnClassID)
        val columnClassDescr = ColumnString("SYSTEM_alias", "descr", "Класс", STRING_COLUMN_WIDTH)
        val columnPermissionValue = ColumnBoolean(modelTableName, "permission_value", "Значение")

        //----------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnRole)
        alTableHiddenColumn.add(columnPermission)
        alTableHiddenColumn.add(columnClass)

        addTableColumn(columnClassDescr)
        addTableColumn(columnPermissionDescr)
        addTableColumn(columnRoleName)
        addTableColumn(columnPermissionValue)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnRole)
        alFormHiddenColumn.add(columnPermission)
        alFormHiddenColumn.add(columnClass)

        alFormColumn.add(columnClassDescr)
        alFormColumn.add(columnPermissionDescr)
        alFormColumn.add(columnRoleName)
        alFormColumn.add(columnPermissionValue)

        //---------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnClassDescr)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnPermissionDescr)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnRoleName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_role"] = columnRole
        hmParentColumn["system_permission"] = columnPermission
        hmParentColumn["system_alias"] = columnClass
    }
}
