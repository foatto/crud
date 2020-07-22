@file:JvmName("mPermission")
package foatto.core_server.app.system

import foatto.core_server.app.server.*
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.sql.CoreAdvancedStatement

class mPermission : mAbstract() {

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SYSTEM_permission"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnClassID = ColumnInt("SYSTEM_alias", "id")
        val columnClass = ColumnInt(tableName, "class_id", columnClassID)
        val columnClassDescr = ColumnString("SYSTEM_alias", "descr", "Класс", STRING_COLUMN_WIDTH)
        val columnPermissionName = ColumnString(tableName, "name", "Наименование", STRING_COLUMN_WIDTH)
        val columnPermissionDescr = ColumnString(tableName, "descr", "Описание", STRING_COLUMN_WIDTH)

        columnClassDescr.selectorAlias = "system_alias"
        columnClassDescr.addSelectorColumn(columnClass, columnClassID)
        columnClassDescr.addSelectorColumn(columnClassDescr)

        //---------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnClass)

        alTableGroupColumn.add(columnClassDescr)

        addTableColumn(columnPermissionName)
        addTableColumn(columnPermissionDescr)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnClass)

        alFormColumn.add(columnClassDescr)
        alFormColumn.add(columnPermissionName)
        alFormColumn.add(columnPermissionDescr)

        //---------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnClassDescr)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnPermissionDescr)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_alias"] = columnClass

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("system_role_permission", columnID!!))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_role_permission", "permission_id"))
    }
}
