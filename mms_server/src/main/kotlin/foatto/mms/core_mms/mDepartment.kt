package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractUserSelector
import foatto.sql.CoreAdvancedStatement

class mDepartment : mAbstractUserSelector() {

    override fun init(
        application: iApplication,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_department"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserName = addUserSelector(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        val columnDepartmentName = ColumnString(modelTableName, "name", "Подразделение", STRING_COLUMN_WIDTH)
        columnDepartmentName.isRequired = true
        //columnDepartmentName.setUnique( true, null ) - у разных корпоративных клиентов могут быть совпадающие значения

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)

        addTableColumn(columnDepartmentName)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)

        alFormColumn.add(columnUserName)
        alFormColumn.add(columnDepartmentName)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnDepartmentName, true)

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_object", columnId, true))
        alChildData.add(ChildData("mms_day_work", columnId))
        alChildData.add(ChildData("mms_shift_work", columnId))
        //--- обычно показывается один из двух модулей
        alChildData.add(ChildData("mms_work_shift", columnId))
        alChildData.add(ChildData("mms_waybill", columnId))

        MMSFunction.fillChildDataForPeriodicReports(columnId, alChildData)
        MMSFunction.fillChildDataForLiquidIncDecReports(columnId, alChildData, withIncWaybillReport = true, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_equip_service", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnId, alChildData, withMovingDetailReport = false)
        alChildData.add(ChildData("Отчёты", "mms_report_downtime", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForEnergoOverReports(columnId, alChildData)
        MMSFunction.fillChildDataForOverReports(columnId, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_trouble", columnId, AppAction.FORM))

        alChildData.add(ChildData("mms_show_object", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("mms_show_trace", columnId, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_object", "department_id"))
    }

}
