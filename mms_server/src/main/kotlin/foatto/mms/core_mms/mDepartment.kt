package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mDepartment : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_department"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(tableName, "user_id", columnUserID, userConfig.userID)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Владелец подразделения", STRING_COLUMN_WIDTH)
        if(userConfig.isAdmin) {
            //columnUserName.setRequired(  true  ); - может быть ничья/общая
            columnUserName.selectorAlias = "system_user_people"
            columnUserName.addSelectorColumn(columnUser!!, columnUserID)
            columnUserName.addSelectorColumn(columnUserName)
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnDepartmentName = ColumnString(tableName, "name", "Подразделение", STRING_COLUMN_WIDTH)
        columnDepartmentName.isRequired = true
        //columnDepartmentName.setUnique( true, null ) - у разных корпоративных клиентов могут быть совпадающие значения

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)

        addTableColumn(columnDepartmentName)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)

        alFormColumn.add(columnUserName)
        alFormColumn.add(columnDepartmentName)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnDepartmentName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_object", columnID!!, true))
        alChildData.add(ChildData("mms_day_work", columnID!!))
        alChildData.add(ChildData("mms_shift_work", columnID!!))
        //--- обычно показывается один из двух модулей
        alChildData.add(ChildData("mms_work_shift", columnID!!))
        alChildData.add(ChildData("mms_waybill", columnID!!))

        MMSFunction.fillChildDataForPeriodicReports(columnID!!, alChildData)
        MMSFunction.fillChildDataForLiquidIncDecReports(columnID!!, alChildData, withIncWaybillReport = true, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_equip_service", columnID!!, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnID!!, alChildData, withMovingDetailReport = false)
        alChildData.add(ChildData("Отчёты", "mms_report_downtime", columnID!!, AppAction.FORM))
        MMSFunction.fillChildDataForEnergoOverReports(columnID!!, alChildData)
        MMSFunction.fillChildDataForOverReports(columnID!!, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_trouble", columnID!!, AppAction.FORM))

        alChildData.add(ChildData("mms_show_object", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("mms_show_trace", columnID!!, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_object", "department_id"))
    }

}
