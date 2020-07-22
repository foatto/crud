package foatto.mms.core_mms

import foatto.app.CoreSpringController
import foatto.core.link.AppAction
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.FormColumnVisibleData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mObject : mAbstract() {

    lateinit var columnDisabled: ColumnBoolean
    lateinit var columnObjectName: ColumnString

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_object"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(tableName, "user_id", columnUserID, userConfig.userID)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Владелец", STRING_COLUMN_WIDTH)
        if(userConfig.isAdmin) {
            columnUserName.selectorAlias = "system_user_people"
            columnUserName.addSelectorColumn(columnUser!!, columnUserID)
            columnUserName.addSelectorColumn(columnUserName)
        }

        columnDisabled = ColumnBoolean(tableName, "is_disabled", "Отключен", false)
        val columnDisableReason = ColumnString(tableName, "disable_reason", "Причина отключения", STRING_COLUMN_WIDTH)
        columnDisableReason.addFormVisible(FormColumnVisibleData(columnDisabled, true, intArrayOf(1)))

        columnObjectName = ColumnString(tableName, "name", "Наименование", STRING_COLUMN_WIDTH)
        columnObjectName.isRequired = true
        //columnObjectName.setUnique(  true, null  ); - у разных клиентов/пользователей могут быть объекты с одинаковыми названиями

        val columnObjectModel = ColumnString(tableName, "model", "Модель", STRING_COLUMN_WIDTH)

        val columnGroupID = ColumnInt("MMS_group", "id")
        val columnGroup = ColumnInt(tableName, "group_id", columnGroupID)
        val columnGroupName = ColumnString("MMS_group", "name", "Группа", STRING_COLUMN_WIDTH)
        columnGroupName.selectorAlias = "mms_group"
        columnGroupName.addSelectorColumn(columnGroup, columnGroupID)
        columnGroupName.addSelectorColumn(columnGroupName)

        val columnDepartmentID = ColumnInt("MMS_department", "id")
        val columnDepartment = ColumnInt(tableName, "department_id", columnDepartmentID)
        val columnDepartmentName = ColumnString("MMS_department", "name", "Подразделение", STRING_COLUMN_WIDTH)
        columnDepartmentName.selectorAlias = "mms_department"
        columnDepartmentName.addSelectorColumn(columnDepartment, columnDepartmentID)
        columnDepartmentName.addSelectorColumn(columnDepartmentName)

        val columnObjectInfo = ColumnString(tableName, "info", "Дополнительная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        val columnEmail = ColumnString(tableName, "e_mail", "E-mail для оповещения", STRING_COLUMN_WIDTH)

        //--- data_version = 0 - старая версия, по 1 байту на номер порта и кол-во байт данных на этом порту
        //--- data_version = 1 - текущая/новая версия, по 2 байта на номер порта и кол-во байт данных на этом порту
        val columnDataVersion = ColumnInt(tableName, "data_version", 1)

        val columnIsAutoWorkShift = ColumnInt(tableName, "is_auto_work_shift", 0)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnDisabled)   // нужен для раскраски
        alTableHiddenColumn.add(columnGroup)
        alTableHiddenColumn.add(columnDepartment)
        alTableHiddenColumn.add(columnDataVersion)
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
        alFormHiddenColumn.add(columnDataVersion)
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

        //--- поля для сортировки
        alTableSortColumn.add(columnObjectName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["mms_group"] = columnGroup
        hmParentColumn["mms_department"] = columnDepartment

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("Журналы...", "mms_day_work", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("Журналы...", "mms_shift_work", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("Журналы...", "mms_downtime", columnID!!, AppAction.TABLE))
        //--- обычно показывается один из двух модулей
        alChildData.add(ChildData("Журналы...", "mms_work_shift", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("Журналы...", "mms_waybill", columnID!!, AppAction.TABLE))

        alChildData.add(ChildData("Отчёты...", "mms_report_summary", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_day_work", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_work_shift", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill_compare", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_summary_without_waybill", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc_waybill", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_dec", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_equip_service", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_work_detail", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_speed", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_parking", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_object_zone", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_moving_detail", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_downtime", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_weight", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_turn", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_pressure", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_temperature", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_voltage", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_trouble", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_data_out", columnID!!, AppAction.FORM))

        alChildData.add(ChildData("Графики...", "mms_graphic_liquid", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Графики...", "mms_graphic_weight", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Графики...", "mms_graphic_turn", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Графики...", "mms_graphic_pressure", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Графики...", "mms_graphic_temperature", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Графики...", "mms_graphic_voltage", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Графики...", "mms_graphic_power", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Графики...", "mms_graphic_speed", columnID!!, AppAction.FORM))

        alChildData.add(ChildData("Карты...", "mms_show_object", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Карты...", "mms_show_trace", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Карты...", "mms_object_zone", columnID!!, AppAction.TABLE))

        alChildData.add(ChildData("mms_show_state", columnID!!, AppAction.FORM))

        alChildData.add(ChildData("mms_sensor", columnID!!, AppAction.TABLE, true))
        alChildData.add(ChildData("mms_equip", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("mms_log_session", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("mms_data", columnID!!, AppAction.TABLE))
        alChildData.add(ChildData("mms_device", columnID!!, AppAction.TABLE))

        alChildData.add(ChildData("Служебные...", "mms_device_command_history", columnID!!, AppAction.TABLE))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("VC_camera", "object_id"))

        alDependData.add(DependData("MMS_day_work", "object_id", DependData.DELETE))
        alDependData.add(DependData("MMS_downtime", "object_id", DependData.DELETE))
        //--- каскадная процедура удаления, реализована в cObject.postDelete
        //alDependData.add(  new DependData(  "MMS_work_shift", "object_id", DependData.DELETE  )  );
        //--- каскадная процедура удаления, реализована в cObject.postDelete
        //alDependData.add(  new DependData(  "MMS_sensor", "object_id", DependData.DELETE  )  );
        alDependData.add(DependData("MMS_object_zone", "object_id", DependData.DELETE))

        alDependData.add(DependData("MMS_device", "object_id", DependData.SET, 0))
        alDependData.add(DependData("MMS_device_command_history", "object_id", DependData.DELETE))
    }

}
