package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnFile
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractUserSelector
import foatto.sql.CoreAdvancedConnection

class mObject : mAbstractUserSelector() {

    lateinit var columnDisabled: ColumnBoolean
    lateinit var columnObjectName: ColumnString

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_object"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserName = addUserSelector(userConfig)

        columnDisabled = ColumnBoolean(modelTableName, "is_disabled", "Отключен", false)
        val columnDisableReason = ColumnString(modelTableName, "disable_reason", "Причина отключения", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnDisabled, true, setOf(1))
        }

        columnObjectName = ColumnString(modelTableName, "name", "Наименование", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            //setUnique(  true, null  ); - different clients / users may have objects with the same names
        }

        val columnObjectModel = ColumnString(modelTableName, "model", "Модель", STRING_COLUMN_WIDTH)

        val columnGroupID = ColumnInt("MMS_group", "id")
        val columnGroup = ColumnInt(modelTableName, "group_id", columnGroupID)
        val columnGroupName = ColumnString("MMS_group", "name", "Группа", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "mms_group"
            addSelectorColumn(columnGroup, columnGroupID)
            addSelectorColumn(this)
        }

        val columnDepartmentID = ColumnInt("MMS_department", "id")
        val columnDepartment = ColumnInt(modelTableName, "department_id", columnDepartmentID)
        val columnDepartmentName = ColumnString("MMS_department", "name", "Подразделение", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "mms_department"
            addSelectorColumn(columnDepartment, columnDepartmentID)
            addSelectorColumn(this)
        }

        val columnObjectInfo = ColumnString(modelTableName, "info", "Дополнительная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        val columnEmail = ColumnString(modelTableName, "e_mail", "E-mail для оповещения", STRING_COLUMN_WIDTH)

        val columnFile = ColumnFile(application, modelTableName, "scheme_file_id", "Схема объекта")

        val columnIsAutoWorkShift = ColumnInt(modelTableName, "is_auto_work_shift", 0)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnId
        alTableHiddenColumn += columnUser!!
        alTableHiddenColumn += columnDisabled   // нужен для раскраски
        alTableHiddenColumn += columnGroup
        alTableHiddenColumn += columnDepartment
        alTableHiddenColumn += columnIsAutoWorkShift

        addTableColumn(columnObjectName)
        addTableColumn(columnObjectModel)
        addTableColumn(columnGroupName)
        addTableColumn(columnDepartmentName)
        addTableColumn(columnObjectInfo)
        addTableColumn(columnEmail)
        addTableColumn(columnFile)

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnUser!!
        alFormHiddenColumn += columnGroup
        alFormHiddenColumn += columnDepartment
        alFormHiddenColumn += columnIsAutoWorkShift

        alFormColumn += columnUserName
        alFormColumn += columnDisabled
        alFormColumn += columnDisableReason
        alFormColumn += columnObjectName
        alFormColumn += columnObjectModel
        alFormColumn += columnGroupName
        alFormColumn += columnDepartmentName
        alFormColumn += columnObjectInfo
        alFormColumn += columnEmail
        alFormColumn += columnFile

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnObjectName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["mms_group"] = columnGroup
        hmParentColumn["mms_department"] = columnDepartment

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("Журналы", "mms_day_work", columnId, AppAction.TABLE))
        alChildData.add(ChildData("Журналы", "mms_shift_work", columnId, AppAction.TABLE))
        //--- usually one of two modules is shown
        alChildData.add(ChildData("Журналы", "mms_work_shift", columnId, AppAction.TABLE))
        alChildData.add(ChildData("Журналы", "mms_waybill", columnId, AppAction.TABLE))

        MMSFunction.fillChildDataForPeriodicReports(columnId, alChildData)
        MMSFunction.fillChildDataForLiquidIncDecReports(columnId, alChildData, withIncWaybillReport = true, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_equip_service", columnId, AppAction.FORM))
        alChildData.add(ChildData("Отчёты", "mms_report_work_detail", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnId, alChildData, withMovingDetailReport = true)
        alChildData.add(ChildData("Отчёты", "mms_report_downtime", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForEnergoOverReports(columnId, alChildData)
        MMSFunction.fillChildDataForOverReports(columnId, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_trouble", columnId, AppAction.FORM))
        alChildData.add(ChildData("Отчёты", "mms_report_data_out", columnId, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnId, alChildData)

        alChildData.add(ChildData("Карты", "mms_show_object", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("Карты", "mms_show_trace", columnId, AppAction.FORM))
        alChildData.add(ChildData("Карты", "mms_object_zone", columnId, AppAction.TABLE))

        alChildData.add(ChildData("mms_show_state", columnId, AppAction.FORM, true))

        alChildData.add(ChildData("mms_sensor", columnId, AppAction.TABLE, true))
        alChildData.add(ChildData("mms_equip", columnId, AppAction.TABLE))
        alChildData.add(ChildData("mms_log_session", columnId, AppAction.TABLE))
        alChildData.add(ChildData("mms_data", columnId, AppAction.TABLE))
        alChildData.add(ChildData("mms_device", columnId, AppAction.TABLE))

        alChildData.add(ChildData("Служебные", "mms_device_command_history", columnId, AppAction.TABLE))

        //----------------------------------------------------------------------------------------

//        alDependData.add(DependData("VC_camera", "object_id"))

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
