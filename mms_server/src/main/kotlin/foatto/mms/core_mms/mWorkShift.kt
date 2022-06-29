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
import foatto.core_server.app.server.mAbstractUserSelector
import foatto.sql.CoreAdvancedStatement

class mWorkShift : mAbstractUserSelector() {

    private lateinit var os: ObjectSelector
    lateinit var columnIsAutoWorkShift: ColumnBoolean
        private set

    val columnObject: ColumnInt
        get() = os.columnObject

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

        //--- это "путевой лист" или "рабочая смена"? (mms_waybill vs. mms_work_shift)
        val isWaybill = aliasConfig.alias == "mms_waybill"

        val parentObjectId = hmParentData["mms_object"]

        //--- определим опцию автосоздания рабочих смен
        var isAutoWorkShift: Boolean? = false
        if(!isWaybill && parentObjectId != null) {
            val rs = stm.executeQuery(" SELECT is_auto_work_shift FROM MMS_object WHERE id = $parentObjectId ")
            if(rs.next()) isAutoWorkShift = rs.getInt(1) != 0
            rs.close()
        }

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_work_shift"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserName = addUserSelector(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        val columnShiftNo = ColumnString(
            modelTableName, "shift_no",
            if(isWaybill) "Номер путевого листа" else "", STRING_COLUMN_WIDTH
        )

        val columnShiftBegDoc = ColumnDateTimeInt(modelTableName, "beg_dt", "Начало", false, zoneId)
        val columnShiftEndDoc = ColumnDateTimeInt(modelTableName, "end_dt", "Окончание", false, zoneId)
        val columnShiftBegFact = ColumnDateTimeInt(modelTableName, "beg_dt_fact", "Начало факт.", false, zoneId)
        val columnShiftEndFact = ColumnDateTimeInt(modelTableName, "end_dt_fact", "Окончание факт.", false, zoneId)

        val columnWorkerID = ColumnInt("MMS_worker", "id")
        val columnWorker = ColumnInt(modelTableName, "worker_id", columnWorkerID)
        val columnWorkerTabNo = ColumnString("MMS_worker", "tab_no", "Табельный номер", STRING_COLUMN_WIDTH)
        val columnWorkerName = ColumnString("MMS_worker", "name", "Ф.И.О.", STRING_COLUMN_WIDTH)

        columnWorkerTabNo.selectorAlias = "mms_worker"
        columnWorkerTabNo.addSelectorColumn(columnWorker, columnWorkerID)
        columnWorkerTabNo.addSelectorColumn(columnWorkerTabNo)
        columnWorkerTabNo.addSelectorColumn(columnWorkerName)

        val columnRun = ColumnDouble(
            modelTableName, "run",
            if(isWaybill) "Пробег [км]" else "", 10, 1, 0.0
        )

        columnIsAutoWorkShift = ColumnBoolean(
            modelTableName, "_is_auto_work_shift",
            "Автоматическое создание рабочих смен", isAutoWorkShift
        )
        columnIsAutoWorkShift.isVirtual = true

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnWorker)

        if(parentObjectId == null)
            alTableGroupColumn.add(columnShiftBegDoc)
        else
            addTableColumn(columnShiftBegDoc)
        addTableColumn(columnShiftEndDoc)
        addTableColumn(columnShiftBegFact)
        addTableColumn(columnShiftEndFact)

        alFormHiddenColumn.add(columnId)
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

        addTableSort(columnShiftBegDoc, false)
        addTableSort(os.columnObjectName, true)

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_work_shift_work", columnId, true))
        alChildData.add(ChildData("mms_work_shift_liquid", columnId))
        //--- запустится только один из этих двух
        alChildData.add(ChildData("Отчёты", "mms_report_work_shift", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты", "mms_report_waybill", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты", "mms_report_waybill_compare", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForLiquidIncDecReports(columnId, alChildData, withIncWaybillReport = false, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_work_detail", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnId, alChildData, withMovingDetailReport = true)
        MMSFunction.fillChildDataForEnergoOverReports(columnId, alChildData)
        MMSFunction.fillChildDataForOverReports(columnId, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_data_out", columnId, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnId, alChildData)

        alChildData.add(ChildData("mms_show_object", columnId, AppAction.FORM, true))
        if (isWaybill)
            alChildData.add(ChildData("mms_show_trace", columnId, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_work_shift_data", "shift_id", DependData.DELETE))

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
    }
}
