package foatto.mms.core_mms

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.UserRelation
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.mAbstract

class UODGSelector {

    lateinit var columnObjectUser: ColumnComboBox
        private set
    lateinit var columnObject: ColumnInt
        private set
    lateinit var columnGroup: ColumnInt
        private set
    lateinit var columnDepartment: ColumnInt
        private set

    fun fillColumns(tableUODG: String, userConfig: UserConfig, hmParentColumn: MutableMap<String, iColumn>, alFormHiddenColumn: MutableList<iColumn>, alFormColumn: MutableList<iColumn>) {

        columnObjectUser = ColumnComboBox(tableUODG, "object_user_id", "По владельцу объектов", 0)
        columnObjectUser.addChoice(0, "(все доступные)")
        columnObjectUser.addChoice(userConfig.userId, "(только свои)")
        for (uID in userConfig.getUserIDList(UserRelation.WORKER))
            columnObjectUser.addChoice(uID, UserConfig.hmUserFullNames[uID]!!)

        val columnObjectID = ColumnInt("MMS_object", "id")
        columnObject = ColumnInt(tableUODG, "object_id", columnObjectID)
        val columnObjectName = ColumnString("MMS_object", "name", "Наименование объекта", mAbstract.STRING_COLUMN_WIDTH)
        val columnObjectModel = ColumnString("MMS_object", "model", "Модель", mAbstract.STRING_COLUMN_WIDTH)
        columnObjectName.selectorAlias = "mms_object"
        columnObjectName.addSelectorColumn(columnObject, columnObjectID)
        columnObjectName.addSelectorColumn(columnObjectName)
        columnObjectName.addSelectorColumn(columnObjectModel)

        val columnGroupID = ColumnInt("MMS_group", "id")
        columnGroup = ColumnInt(tableUODG, "group_id", columnGroupID)
        val columnGroupName = ColumnString("MMS_group", "name", "Группа", mAbstract.STRING_COLUMN_WIDTH)
        columnGroupName.selectorAlias = "mms_group"
        columnGroupName.addSelectorColumn(columnGroup, columnGroupID)
        columnGroupName.addSelectorColumn(columnGroupName)

        val columnDepartmentID = ColumnInt("MMS_department", "id")
        columnDepartment = ColumnInt(tableUODG, "department_id", columnDepartmentID)
        val columnDepartmentName = ColumnString("MMS_department", "name", "Подразделение", mAbstract.STRING_COLUMN_WIDTH)
        columnDepartmentName.selectorAlias = "mms_department"
        columnDepartmentName.addSelectorColumn(columnDepartment, columnDepartmentID)
        columnDepartmentName.addSelectorColumn(columnDepartmentName)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnObject)
        alFormHiddenColumn.add(columnGroup)
        alFormHiddenColumn.add(columnDepartment)

        alFormColumn.add(columnObjectUser)
        alFormColumn.add(columnObjectName)
        alFormColumn.add(columnObjectModel)
        alFormColumn.add(columnGroupName)
        alFormColumn.add(columnDepartmentName)

        hmParentColumn["mms_object"] = columnObject
        hmParentColumn["mms_group"] = columnGroup
        hmParentColumn["mms_department"] = columnDepartment
    }
}
