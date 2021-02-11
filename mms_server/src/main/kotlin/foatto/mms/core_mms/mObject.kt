package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mObject : mAbstract() {

    lateinit var columnDisabled: ColumnBoolean
    lateinit var columnObjectName: ColumnString

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_object"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(tableName, "user_id", columnUserID, userConfig.userID)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Владелец", STRING_COLUMN_WIDTH).apply {
            if (userConfig.isAdmin) {
                selectorAlias = "system_user_people"
                addSelectorColumn(columnUser!!, columnUserID)
                addSelectorColumn(this)
            }
        }

        columnDisabled = ColumnBoolean(tableName, "is_disabled", "Отключен", false)
        val columnDisableReason = ColumnString(tableName, "disable_reason", "Причина отключения", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnDisabled, true, setOf(1))
        }

        columnObjectName = ColumnString(tableName, "name", "Наименование", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            //setUnique(  true, null  ); - different clients / users may have objects with the same names
        }

        val columnObjectModel = ColumnString(tableName, "model", "Модель", STRING_COLUMN_WIDTH)

        val columnGroupID = ColumnInt("MMS_group", "id")
        val columnGroup = ColumnInt(tableName, "group_id", columnGroupID)
        val columnGroupName = ColumnString("MMS_group", "name", "Группа", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "mms_group"
            addSelectorColumn(columnGroup, columnGroupID)
            addSelectorColumn(this)
        }

        val columnDepartmentID = ColumnInt("MMS_department", "id")
        val columnDepartment = ColumnInt(tableName, "department_id", columnDepartmentID)
        val columnDepartmentName = ColumnString("MMS_department", "name", "Подразделение", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "mms_department"
            addSelectorColumn(columnDepartment, columnDepartmentID)
            addSelectorColumn(this)
        }

        val columnObjectInfo = ColumnString(tableName, "info", "Дополнительная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        val columnEmail = ColumnString(tableName, "e_mail", "E-mail для оповещения", STRING_COLUMN_WIDTH)

        val columnIsAutoWorkShift = ColumnInt(tableName, "is_auto_work_shift", 0)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnDisabled)   // нужен для раскраски
        alTableHiddenColumn.add(columnGroup)
        alTableHiddenColumn.add(columnDepartment)
        alTableHiddenColumn.add(columnIsAutoWorkShift)

        addTableColumn(columnObjectName)
        addTableColumn(columnObjectModel)
        addTableColumn(columnGroupName)
        addTableColumn(columnDepartmentName)
        addTableColumn(columnObjectInfo)
        addTableColumn(columnEmail)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnGroup)
        alFormHiddenColumn.add(columnDepartment)
        alFormHiddenColumn.add(columnIsAutoWorkShift)

        alFormColumn.add(columnUserName)
        alFormColumn.add(columnDisabled)
        alFormColumn.add(columnDisableReason)
        alFormColumn.add(columnObjectName)
        alFormColumn.add(columnObjectModel)
        alFormColumn.add(columnGroupName)
        alFormColumn.add(columnDepartmentName)
        alFormColumn.add(columnObjectInfo)
        alFormColumn.add(columnEmail)

        //----------------------------------------------------------------------------------------------------------------------

        alTableSortColumn.add(columnObjectName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["mms_group"] = columnGroup
        hmParentColumn["mms_department"] = columnDepartment

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("Журналы...", "mms_day_work", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("Журналы...", "mms_shift_work", columnID!!, AppAction.TABLE))
        //--- usually one of two modules is shown
        alChildData.add(ChildData("Журналы...", "mms_work_shift", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("Журналы...", "mms_waybill", columnID!!, AppAction.TABLE))

        MMSFunction.fillChildDataForPeriodicReports(columnID!!, alChildData)
        MMSFunction.fillChildDataForLiquidIncDecReports(columnID!!, alChildData, withIncWaybillReport = true, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_equip_service", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты", "mms_report_work_detail", columnID!!, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnID!!, alChildData, withMovingDetailReport = true)
        alChildData.add(ChildData("Отчёты", "mms_report_downtime", columnID!!, AppAction.FORM))
        MMSFunction.fillChildDataForEnergoOverReports(columnID!!, alChildData)
        MMSFunction.fillChildDataForOverReports(columnID!!, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_trouble", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты", "mms_report_data_out", columnID!!, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnID!!, alChildData)

        alChildData.add(ChildData("Карты...", "mms_show_object", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("Карты...", "mms_show_trace", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Карты...", "mms_object_zone", columnID!!, AppAction.TABLE))

        alChildData.add(ChildData("mms_show_state", columnID!!, AppAction.FORM, true))

        alChildData.add(ChildData("mms_sensor", columnID!!, AppAction.TABLE, true))
        alChildData.add(ChildData("mms_equip", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("mms_log_session", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("mms_data", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("mms_device", columnID!!, AppAction.TABLE))

        alChildData.add(ChildData("Служебные...", "mms_device_command_history", columnID!!, AppAction.TABLE))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("VC_camera", "object_id"))

        alDependData.add(DependData("MMS_day_work", "object_id", DependData.DELETE))
        //--- cascade deletion procedure, implemented in cObject.postDelete
        //alDependData.add(  new DependData(  "MMS_work_shift", "object_id", DependData.DELETE  )  );
        //--- cascade deletion procedure, implemented in cObject.postDelete
        //alDependData.add(  new DependData(  "MMS_sensor", "object_id", DependData.DELETE  )  );
        alDependData.add(DependData("MMS_object_zone", "object_id", DependData.DELETE))

        alDependData.add(DependData("MMS_device", "object_id", DependData.SET, 0))
        alDependData.add(DependData("MMS_device_command_history", "object_id", DependData.DELETE))
    }

}
