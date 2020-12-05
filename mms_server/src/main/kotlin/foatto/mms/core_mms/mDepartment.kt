package foatto.mms.core_mms

import foatto.app.CoreSpringController
import foatto.core.link.AppAction
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mDepartment : mAbstract() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

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
        alChildData.add(ChildData("mms_downtime", columnID!!))
        //--- обычно показывается один из двух модулей
        alChildData.add(ChildData("mms_work_shift", columnID!!))
        alChildData.add(ChildData("mms_waybill", columnID!!))

        alChildData.add(ChildData("Отчёты...", "mms_report_summary", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты...", "mms_report_day_work", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_work_shift", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill_compare", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc_waybill", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_dec", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_equip_service", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_speed", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_parking", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_object_zone", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_downtime", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_weight", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_turn", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_pressure", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_temperature", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_voltage", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_trouble", columnID!!, AppAction.FORM))

        alChildData.add(ChildData("mms_show_object", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("mms_show_trace", columnID!!, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_object", "department_id"))
    }

}
