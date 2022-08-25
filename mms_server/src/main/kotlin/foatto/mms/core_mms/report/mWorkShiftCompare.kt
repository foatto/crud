package foatto.mms.core_mms.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
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

class mWorkShiftCompare : mAbstractReport() {

    companion object {
        const val TIME_TYPE_DOC = 0
        const val TIME_TYPE_FACT = 1
        const val TIME_TYPE_DAY = 2
    }

    lateinit var uodg: UODGSelector

    lateinit var columnReportBegDate: ColumnDate3Int
    lateinit var columnReportEndDate: ColumnDate3Int

    lateinit var columnWorkShift: ColumnInt
    lateinit var columnWorker: ColumnInt
    lateinit var columnAddBefore: ColumnInt
    lateinit var columnAddAfter: ColumnInt
    lateinit var columnTimeType: ColumnComboBox
    lateinit var columnMaxDiff: ColumnInt
    lateinit var columnOutOverDiffOnly: ColumnBoolean
    lateinit var columnOutRunWithoutKoef: ColumnBoolean

    lateinit var columnSumWorker: ColumnBoolean
    lateinit var sos: SumOptionSelector

    //    private ColumnBoolean columnIsDayWaybill = null;
    //        columnIsDayWaybill = new ColumnBoolean( tableName, "cwc_sum_day_waybill", "Суммировать путёвки за сутки" );

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- это отчёт по путевым листам или рабочим сменам? (mms_waybill vs. mms_work_shift)
        val isWaybillReport = aliasConfig.alias == "mms_report_waybill_compare"

        //--- отдельная обработка перехода от журнала (суточных) пробегов
        val arrADR = MMSFunction.getDayWorkParent(stm, hmParentData)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnWorkShiftID = ColumnInt("MMS_work_shift", "id")
        columnWorkShift = ColumnInt(modelTableName, "work_shift_id", columnWorkShiftID)
        val columnShiftNo = ColumnString("MMS_work_shift", "shift_no", if(isWaybillReport) "Номер путевого листа" else "", STRING_COLUMN_WIDTH)
        val columnShiftBegDoc = ColumnDateTimeInt("MMS_work_shift", "beg_dt", "Начало", false, zoneId)
        val columnShiftEndDoc = ColumnDateTimeInt("MMS_work_shift", "end_dt", "Окончание", false, zoneId)
        val columnShiftBegFact = ColumnDateTimeInt("MMS_work_shift", "beg_dt_fact", "Начало факт.", false, zoneId)
        val columnShiftEndFact = ColumnDateTimeInt("MMS_work_shift", "end_dt_fact", "Окончание факт.", false, zoneId)

        (if(isWaybillReport) columnShiftNo else columnShiftBegDoc).selectorAlias = if(isWaybillReport) "mms_waybill" else "mms_work_shift"
        (if(isWaybillReport) columnShiftNo else columnShiftBegDoc).addSelectorColumn(columnWorkShift, columnWorkShiftID)
        (if(isWaybillReport) columnShiftNo else columnShiftBegDoc).addSelectorColumn(columnShiftNo)
        (if(isWaybillReport) columnShiftNo else columnShiftBegDoc).addSelectorColumn(columnShiftBegDoc)
        (if(isWaybillReport) columnShiftNo else columnShiftBegDoc).addSelectorColumn(columnShiftEndDoc)
        (if(isWaybillReport) columnShiftNo else columnShiftBegDoc).addSelectorColumn(columnShiftBegFact)
        (if(isWaybillReport) columnShiftNo else columnShiftBegDoc).addSelectorColumn(columnShiftEndFact)

        val columnWorkerID = ColumnInt("MMS_worker", "id")
        columnWorker = ColumnInt(modelTableName, "worker_id", columnWorkerID)
        val columnWorkerTabNo = ColumnString("MMS_worker", "tab_no", "Табельный номер", STRING_COLUMN_WIDTH)
        val columnWorkerName = ColumnString("MMS_worker", "name", "Ф.И.О.", STRING_COLUMN_WIDTH)

        columnWorkerTabNo.selectorAlias = "mms_worker"
        columnWorkerTabNo.addSelectorColumn(columnWorker, columnWorkerID)
        columnWorkerTabNo.addSelectorColumn(columnWorkerTabNo)
        columnWorkerTabNo.addSelectorColumn(columnWorkerName)

        columnReportBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Начало периода")
        if(arrADR != null) columnReportBegDate.default = LocalDate.of(arrADR[0], arrADR[1], arrADR[2])
        columnReportBegDate.isVirtual = true
        columnReportEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Конец периода")
        if(arrADR != null) columnReportEndDate.default = LocalDate.of(arrADR[0], arrADR[1], arrADR[2])
        columnReportEndDate.isVirtual = true

        columnTimeType = ColumnComboBox(modelTableName, "waybill_time_range_type", "Используемое время начала/окончания", 0)
        columnTimeType.addChoice(TIME_TYPE_DOC, "Заявленное")
        columnTimeType.addChoice(TIME_TYPE_FACT, "Фактическое")
        columnTimeType.addChoice(TIME_TYPE_DAY, "Начало/окончание суток")
        columnTimeType.isVirtual = true
        columnTimeType.setSavedDefault(userConfig)

        columnAddBefore = ColumnInt(modelTableName, "add_before", "Добавить к началу [мин]", 10, 0)
        columnAddBefore.isVirtual = true
        columnAddBefore.setSavedDefault(userConfig)
        columnAddAfter = ColumnInt(modelTableName, "add_after", "Добавить к концу [мин]", 10, 0)
        columnAddAfter.isVirtual = true
        columnAddAfter.setSavedDefault(userConfig)

        columnMaxDiff = ColumnInt(modelTableName, "max_diff", "Допустимое отклонение [%]", 10, 0)
        columnMaxDiff.isVirtual = true
        columnMaxDiff.setSavedDefault(userConfig)

        columnOutOverDiffOnly = ColumnBoolean(modelTableName, "over_diff_out_mode", "Показывать только большие отклонения", false)
        columnOutOverDiffOnly.isVirtual = true
        columnOutOverDiffOnly.setSavedDefault(userConfig)

        columnOutRunWithoutKoef = ColumnBoolean(modelTableName, "over_run_without_koef", "Показывать пробег без коэффициентов", false)
        columnOutRunWithoutKoef.isVirtual = true
        columnOutRunWithoutKoef.setSavedDefault(userConfig)

        columnSumWorker = ColumnBoolean(modelTableName, "sum_worker", "Выводить суммы по водителям", true)
        columnSumWorker.isVirtual = true
        columnSumWorker.setSavedDefault(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnWorkShift)
        alFormHiddenColumn.add(columnWorker)

        //----------------------------------------------------------------------------------------------------------------------

        (if(isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnShiftNo)
        alFormColumn.add(columnShiftBegDoc)
        alFormColumn.add(columnShiftEndDoc)
        alFormColumn.add(columnShiftBegFact)
        alFormColumn.add(columnShiftEndFact)

        uodg = UODGSelector()
        uodg.fillColumns(application, modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        (if(isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnWorkerTabNo)
        (if(isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnWorkerName)
        alFormColumn.add(columnReportBegDate)
        alFormColumn.add(columnReportEndDate)
        alFormColumn.add(columnTimeType)
        alFormColumn.add(columnAddBefore)
        alFormColumn.add(columnAddAfter)
        alFormColumn.add(columnMaxDiff)
        alFormColumn.add(columnOutOverDiffOnly)
        alFormColumn.add(columnOutRunWithoutKoef)

        sos = SumOptionSelector()
        sos.fillColumns(userConfig, modelTableName, alFormColumn)

        (if(isWaybillReport) alFormColumn else alFormHiddenColumn).add(columnSumWorker)

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["mms_work_shift"] = columnWorkShift
        hmParentColumn["mms_waybill"] = columnWorkShift
        hmParentColumn["mms_worker"] = columnWorker
    }
}
