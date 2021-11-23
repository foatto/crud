package foatto.mms.core_mms.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate

class mWorkShift : mAbstractReport() {

    companion object {

        val GROUP_BY_DATE = 0
        val GROUP_BY_OBJECT = 1
    }

    lateinit var uodg: UODGSelector
        private set

    lateinit var columnReportBegDate: ColumnDate3Int
        private set
    lateinit var columnReportEndDate: ColumnDate3Int
        private set
    lateinit var columnReportGroupType: ColumnComboBox
        private set

    lateinit var columnWorkShift: ColumnInt
        private set
    lateinit var columnWorker: ColumnInt
        private set
    lateinit var columnAddBefore: ColumnInt
        private set
    lateinit var columnAddAfter: ColumnInt
        private set

    lateinit var sros: SummaryReportOptionSelector
        private set

    lateinit var sos: SumOptionSelector
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- это отчёт по путевым листам или рабочим сменам? (mms_waybill vs. mms_work_shift)
        val isWaybillReport = aliasConfig.alias == "mms_report_waybill"

        //--- отдельно возьмём парентов по сменам,
        //--- т.к. в трёх исходных парентах ещё может быть журнал посменных работ,
        //--- а он по селектору на два парента (рабочие смены и путёвки) не сработает
        //--- обработка перехода от рабочих смен, от путёвок, от журнала сменных работ
        val parentShift: Int? = hmParentData["mms_work_shift"] ?: hmParentData["mms_waybill"] ?: hmParentData["mms_shift_work"]

        //        //--- это переход с объекта или общий список?
        //        boolean isCommonList = hmParentData.get( "mms_object" ) == null;

        //--- отдельная обработка перехода от журнала (суточных) пробегов
        val arrADR = MMSFunction.getDayWorkParent(stm, hmParentData)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnWorkShiftID = ColumnInt("MMS_work_shift", "id")
        columnWorkShift = ColumnInt(modelTableName, "work_shift_id", columnWorkShiftID).apply {
            defaultValue = parentShift
        }
        val columnShiftNo = ColumnString("MMS_work_shift", "shift_no", if (isWaybillReport) "Номер путевого листа" else "", STRING_COLUMN_WIDTH)
        val columnShiftBegDoc = ColumnDateTimeInt("MMS_work_shift", "beg_dt", if (isWaybillReport) "Начало" else "Начало смены", false, zoneId)
        val columnShiftEndDoc = ColumnDateTimeInt("MMS_work_shift", "end_dt", if (isWaybillReport) "Окончание" else "Окончание смены", false, zoneId)
        val columnShiftBegFact = ColumnDateTimeInt("MMS_work_shift", "beg_dt_fact", if (isWaybillReport) "Начало факт." else "Начало смены факт.", false, zoneId)
        val columnShiftEndFact = ColumnDateTimeInt("MMS_work_shift", "end_dt_fact", if (isWaybillReport) "Окончание факт." else "Окончание смены факт.", false, zoneId)

        (if (isWaybillReport) columnShiftNo else columnShiftBegDoc).apply {
            selectorAlias = if (isWaybillReport) "mms_waybill" else "mms_work_shift"
            addSelectorColumn(columnWorkShift, columnWorkShiftID)
            addSelectorColumn(columnShiftNo)        // not "this"!
            addSelectorColumn(columnShiftBegDoc)    // not "this"!
            addSelectorColumn(columnShiftEndDoc)
            addSelectorColumn(columnShiftBegFact)
            addSelectorColumn(columnShiftEndFact)
        }

        val columnWorkerID = ColumnInt("MMS_worker", "id")
        columnWorker = ColumnInt(modelTableName, "worker_id", columnWorkerID)
        val columnWorkerName = ColumnString("MMS_worker", "name", "Ф.И.О.", STRING_COLUMN_WIDTH)

        val columnWorkerTabNo = ColumnString("MMS_worker", "tab_no", "Табельный номер", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "mms_worker"
            addSelectorColumn(columnWorker, columnWorkerID)
            addSelectorColumn(this)
            addSelectorColumn(columnWorkerName)
        }

        columnReportBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Начало периода").apply {
            if (arrADR != null) default = LocalDate.of(arrADR[0], arrADR[1], arrADR[2])
            isVirtual = true
        }

        columnReportEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Конец периода").apply {
            if (arrADR != null) default = LocalDate.of(arrADR[0], arrADR[1], arrADR[2])
            isVirtual = true
        }

        columnAddBefore = ColumnInt(modelTableName, "add_before", "Добавить к началу путёвки [мин]", 10, 0).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }
        columnAddAfter = ColumnInt(modelTableName, "add_after", "Добавить к концу путёвки [мин]", 10, 0).apply {
            isVirtual = true
            setSavedDefault(userConfig)
        }

        columnReportGroupType = ColumnComboBox(modelTableName, "object_date_group_type", "Группировка", GROUP_BY_OBJECT).apply {
            addChoice(GROUP_BY_OBJECT, "По объектам")
            addChoice(GROUP_BY_DATE, "По датам")
            isVirtual = true
            setSavedDefault(userConfig)
        }

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnWorkShift)
        alFormHiddenColumn.add(columnWorker)

        //----------------------------------------------------------------------------------------------------------------------

        (if (isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnShiftNo)
        alFormColumn.add(columnShiftBegDoc)
        alFormColumn.add(columnShiftEndDoc)
        alFormColumn.add(columnShiftBegFact)
        alFormColumn.add(columnShiftEndFact)

        uodg = UODGSelector()
        uodg.fillColumns(modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        (if (isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnWorkerTabNo)
        (if (isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnWorkerName)
        alFormColumn.add(columnReportBegDate)
        alFormColumn.add(columnReportEndDate)
        (if (isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnAddBefore)
        (if (isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnAddAfter)
        //--- при выводе отчёт по путевому листу нет смысла группировать по времени,
        //--- т.к. помимо временного периода путёвки отличаются ещё номером и водителем
        (if (isWaybillReport) alFormHiddenColumn else alFormColumn).add(columnReportGroupType)

        sros = SummaryReportOptionSelector()
        sros.fillColumns(userConfig, modelTableName, alFormColumn)

        sos = SumOptionSelector()
        sos.fillColumns(userConfig, modelTableName, alFormColumn)

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["mms_work_shift"] = columnWorkShift
        hmParentColumn["mms_waybill"] = columnWorkShift
        hmParentColumn["mms_worker"] = columnWorker
    }
}
