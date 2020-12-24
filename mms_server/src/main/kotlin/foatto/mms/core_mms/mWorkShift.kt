package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mWorkShift : mAbstract() {

    private lateinit var os: ObjectSelector
    lateinit var columnIsAutoWorkShift: ColumnBoolean
        private set

    val columnObject: ColumnInt
        get() = os.columnObject

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- это "путевой лист" или "рабочая смена"? (mms_waybill vs. mms_work_shift)
        val isWaybill = aliasConfig.alias == "mms_waybill"

        val parentObjectID = hmParentData["mms_object"]

        //--- определим опцию автосоздания рабочих смен
        var isAutoWorkShift: Boolean? = false
        if(!isWaybill && parentObjectID != null) {
            val rs = stm.executeQuery(" SELECT is_auto_work_shift FROM MMS_object WHERE id = $parentObjectID ")
            if(rs.next()) isAutoWorkShift = rs.getInt(1) != 0
            rs.close()
        }

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_work_shift"

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

        val columnShiftNo = ColumnString(
            tableName, "shift_no",
            if(isWaybill) "Номер путевого листа" else "", STRING_COLUMN_WIDTH
        )

        val columnShiftBegDoc = ColumnDateTimeInt(tableName, "beg_dt", "Начало", false, zoneId)
        val columnShiftEndDoc = ColumnDateTimeInt(tableName, "end_dt", "Окончание", false, zoneId)
        val columnShiftBegFact = ColumnDateTimeInt(tableName, "beg_dt_fact", "Начало факт.", false, zoneId)
        val columnShiftEndFact = ColumnDateTimeInt(tableName, "end_dt_fact", "Окончание факт.", false, zoneId)

        val columnWorkerID = ColumnInt("MMS_worker", "id")
        val columnWorker = ColumnInt(tableName, "worker_id", columnWorkerID)
        val columnWorkerTabNo = ColumnString("MMS_worker", "tab_no", "Табельный номер", STRING_COLUMN_WIDTH)
        val columnWorkerName = ColumnString("MMS_worker", "name", "Ф.И.О.", STRING_COLUMN_WIDTH)

        columnWorkerTabNo.selectorAlias = "mms_worker"
        columnWorkerTabNo.addSelectorColumn(columnWorker, columnWorkerID)
        columnWorkerTabNo.addSelectorColumn(columnWorkerTabNo)
        columnWorkerTabNo.addSelectorColumn(columnWorkerName)

        val columnRun = ColumnDouble(
            tableName, "run",
            if(isWaybill) "Пробег [км]" else "", 10, 1, 0.0
        )

        columnIsAutoWorkShift = ColumnBoolean(
            tableName, "_is_auto_work_shift",
            "Автоматическое создание рабочих смен", isAutoWorkShift
        )
        columnIsAutoWorkShift.isVirtual = true

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnWorker)

        if(parentObjectID == null)
            alTableGroupColumn.add(columnShiftBegDoc)
        else
            addTableColumn(columnShiftBegDoc)
        addTableColumn(columnShiftEndDoc)
        addTableColumn(columnShiftBegFact)
        addTableColumn(columnShiftEndFact)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnWorker)

        alFormColumn.add(columnUserName)

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1)

        //----------------------------------------------------------------------------------------------------------------------

        if(isWaybill) {
            addTableColumn(columnShiftNo)
            addTableColumn(columnWorkerTabNo)
            addTableColumn(columnWorkerName)
            addTableColumn(columnRun)
        } else {
            alTableHiddenColumn.add(columnShiftNo)
            alTableHiddenColumn.add(columnWorkerTabNo)
            alTableHiddenColumn.add(columnWorkerName)
            alTableHiddenColumn.add(columnRun)
        }

        (if(isWaybill) alFormColumn else alFormHiddenColumn).add(columnShiftNo)
        alFormColumn.add(columnShiftBegDoc)
        alFormColumn.add(columnShiftEndDoc)
        alFormColumn.add(columnShiftBegFact)
        alFormColumn.add(columnShiftEndFact)
        (if(isWaybill) alFormColumn else alFormHiddenColumn).add(columnWorkerTabNo)
        (if(isWaybill) alFormColumn else alFormHiddenColumn).add(columnWorkerName)
        (if(isWaybill) alFormColumn else alFormHiddenColumn).add(columnRun)
        (if(isWaybill) alFormHiddenColumn else alFormColumn).add(columnIsAutoWorkShift)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnShiftBegDoc)
        alTableSortDirect.add("DESC")
        alTableSortColumn.add(os.columnObjectName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_work_shift_work", columnID!!, true))
        alChildData.add(ChildData("mms_work_shift_liquid", columnID!!))
        //--- запустится один из них
        alChildData.add(ChildData("Отчёты...", "mms_report_work_shift", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill_compare", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_dec", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_work_detail", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_speed", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_parking", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_object_zone", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_moving_detail", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_weight", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_turn", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_pressure", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_temperature", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_over_voltage", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_data_out", columnID!!, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnID!!, alChildData)

        alChildData.add(ChildData("mms_show_object", columnID!!, AppAction.FORM, true))
        if(isWaybill)
            alChildData.add(ChildData("mms_show_trace", columnID!!, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_work_shift_data", "shift_id", DependData.DELETE))

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
    }
}
