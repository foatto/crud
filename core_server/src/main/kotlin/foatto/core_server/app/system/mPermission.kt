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

class mPermission : mAbstract() {

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SYSTEM_permission"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnClassID = ColumnInt("SYSTEM_alias", "id")
        val columnClass = ColumnInt(modelTableName, "class_id", columnClassID)
        val columnClassDescr = ColumnString("SYSTEM_alias", "descr", "Класс", STRING_COLUMN_WIDTH)
        val columnPermissionName = ColumnString(modelTableName, "name", "Наименование", STRING_COLUMN_WIDTH)
        val columnPermissionDescr = ColumnString(modelTableName, "descr", "Описание", STRING_COLUMN_WIDTH)

        columnClassDescr.selectorAlias = "system_alias"
        columnClassDescr.addSelectorColumn(columnClass, columnClassID)
        columnClassDescr.addSelectorColumn(columnClassDescr)

        //---------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnClass)

        alTableGroupColumn.add(columnClassDescr)

        addTableColumn(columnPermissionName)
        addTableColumn(columnPermissionDescr)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnClass)

        alFormColumn.add(columnClassDescr)
        alFormColumn.add(columnPermissionName)
        alFormColumn.add(columnPermissionDescr)

        //---------------------------------------------------------------------

        addTableSort(columnClassDescr, true)
        addTableSort(columnPermissionDescr, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_alias"] = columnClass

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("system_role_permission", columnId))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_role_permission", "permission_id"))
    }
}
