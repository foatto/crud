package foatto.mms.core_mms

import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnStatic
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.mAbstract

class ObjectSelector {

    private var fieldObject = "object_id"

    private var selectorAlias: String? = null
    private var selectorColumnTo: iColumn? = null
    private var selectorColumnFrom: iColumn? = null

    lateinit var columnObject: ColumnInt
        private set
    lateinit var columnObjectName: ColumnString
        private set
    lateinit var columnObjectModel: ColumnString
        private set
    lateinit var columnGroup: ColumnInt
        private set
    lateinit var columnGroupName: ColumnString
        private set
    lateinit var columnDepartment: ColumnInt
        private set
    lateinit var columnDepartmentName: ColumnString
        private set

    fun fillColumns(
        model: mAbstract, isRequired: Boolean, isSelector: Boolean,
        alTableHiddenColumn: MutableList<iColumn>?, alFormHiddenColumn: MutableList<iColumn>?, alFormColumn: MutableList<iColumn>?,
        hmParentColumn: MutableMap<String, iColumn>, aSingleObjectMode: Boolean, addedStaticColumnCount: Int
    ) {

        val columnObjectID = ColumnInt("MMS_object", "id")
        columnObject = ColumnInt(model.tableName, fieldObject, columnObjectID)

        columnObjectName = ColumnString("MMS_object", "name", "Наименование объекта", mAbstract.STRING_COLUMN_WIDTH)
        columnObjectName.isRequired = isRequired
        columnObjectModel = ColumnString("MMS_object", "model", "Модель", mAbstract.STRING_COLUMN_WIDTH)

        val columnGroupID = ColumnInt("MMS_group", "id")
        columnGroup = ColumnInt("MMS_object", "group_id", columnGroupID)
        columnGroupName = ColumnString("MMS_group", "name", "Группа", mAbstract.STRING_COLUMN_WIDTH)

        val columnDepartmentID = ColumnInt("MMS_department", "id")
        columnDepartment = ColumnInt("MMS_object", "department_id", columnDepartmentID)
        columnDepartmentName = ColumnString("MMS_department", "name", "Подразделение", mAbstract.STRING_COLUMN_WIDTH)

        if (isSelector) {
            columnObjectName.selectorAlias = if (selectorAlias == null) {
                "mms_object"
            } else {
                selectorAlias
            }
            //--- только для "родного" алиаса
            if (selectorAlias == null) {
                columnObjectName.addSelectorColumn(columnObject, columnObjectID)
            }
            else {
                columnObjectName.addSelectorColumn(selectorColumnTo!!, selectorColumnFrom!!)
            }
            columnObjectName.addSelectorColumn(columnObjectName)
            columnObjectName.addSelectorColumn(columnObjectModel)
            columnObjectName.addSelectorColumn(columnGroupName)
            columnObjectName.addSelectorColumn(columnDepartmentName)
        }

        //--------------------------------------------------------------------------------------------------------------

        if (alTableHiddenColumn != null) {
            alTableHiddenColumn.add(columnObject)
            alTableHiddenColumn.add(columnGroup)
            alTableHiddenColumn.add(columnDepartment)
        }

        if (aSingleObjectMode) {
            alTableHiddenColumn!!.add(columnObjectName)
            alTableHiddenColumn.add(columnObjectModel)
            alTableHiddenColumn.add(columnGroupName)
            alTableHiddenColumn.add(columnDepartmentName)
        } else if (addedStaticColumnCount >= 0) {
            model.addTableColumnVertNew(columnObjectName)
            columnObjectName.rowSpan = 3 + addedStaticColumnCount

            model.addTableColumnVertNew(columnObjectModel, columnGroupName, columnDepartmentName)

            val arr = Array(addedStaticColumnCount) { ColumnStatic("") }
            model.addTableColumnVertAdd(*arr)
        } else {
            model.addTableColumn(columnObjectName)
            model.addTableColumn(columnObjectModel)
            model.addTableColumn(columnGroupName)
            model.addTableColumn(columnDepartmentName)
        }

        if (alFormHiddenColumn != null) {
            alFormHiddenColumn.add(columnObject)
            alFormHiddenColumn.add(columnGroup)
            alFormHiddenColumn.add(columnDepartment)
        }

        if (alFormColumn != null) {
            if (aSingleObjectMode) {
                alFormHiddenColumn!!.add(columnObjectName)
                alFormHiddenColumn.add(columnObjectModel)
                alFormHiddenColumn.add(columnGroupName)
                alFormHiddenColumn.add(columnDepartmentName)
            } else {
                alFormColumn.add(columnObjectName)
                alFormColumn.add(columnObjectModel)
                alFormColumn.add(columnGroupName)
                alFormColumn.add(columnDepartmentName)
            }
        }

        hmParentColumn["mms_object"] = columnObject
        hmParentColumn["mms_group"] = columnGroup
        hmParentColumn["mms_department"] = columnDepartment
    }

    fun setSelectorAlias(aSelectorAlias: String, aSelectorColumnTo: iColumn, aSelectorColumnFrom: iColumn) {
        selectorAlias = aSelectorAlias
        selectorColumnTo = aSelectorColumnTo
        selectorColumnFrom = aSelectorColumnFrom
    }
}
