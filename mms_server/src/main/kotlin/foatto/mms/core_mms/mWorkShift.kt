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
import foatto.sql.CoreAdvancedConnection

class mWorkShift : mAbstractUserSelector() {

    private lateinit var os: ObjectSelector

    lateinit var columnShiftBegDoc: ColumnDateTimeInt
        private set
    lateinit var columnShiftEndDoc: ColumnDateTimeInt
        private set

    lateinit var columnIsAutoAddWorkShift: ColumnBoolean
        private set
    lateinit var columnIsAutoCreateWorkShift: ColumnBoolean
        private set

    val columnObject: ColumnInt
        get() = os.columnObject

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

        //--- это "путевой лист" или "рабочая смена"? (mms_waybill vs. mms_work_shift)
        val isWaybill = aliasConfig.name == "mms_waybill"

        val parentObjectId = hmParentData["mms_object"]

        //--- определим значение по умолчанию опции автоматического добавления рабочих смен
        val isAutoAddWorkShiftEnabled = !isWaybill && parentObjectId != null

        //--- определим опцию автосоздания рабочих смен
        val isAutoCreateWorkShiftEnabled = !isWaybill && parentObjectId != null
        var isAutoCreateWorkShift = false
        if (isAutoCreateWorkShiftEnabled) {
            val rs = conn.executeQuery(" SELECT is_auto_work_shift FROM MMS_object WHERE id = $parentObjectId ")
            if (rs.next()) {
                isAutoCreateWorkShift = rs.getInt(1) != 0
            }
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
            if (isWaybill) "Номер путевого листа" else "", STRING_COLUMN_WIDTH
        )

        columnShiftBegDoc = ColumnDateTimeInt(modelTableName, "beg_dt", "Начало", false, zoneId)
        columnShiftEndDoc = ColumnDateTimeInt(modelTableName, "end_dt", "Окончание", false, zoneId)
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
            aTableName = modelTableName,
            aFieldName = "run",
            aCaption = if (isWaybill) "Пробег [км]" else "",
            aCols = 10,
            aPrecision = 1,
            aDefaultValue = 0.0
        )

        columnIsAutoAddWorkShift = ColumnBoolean(
            aTableName = modelTableName,
            aFieldName = "_is_add_work_shift",
            aCaption = "Добавить рабочие смены до настоящего времени",
            aDefaultValue = false
        ).apply {
            isVirtual = true
        }

        columnIsAutoCreateWorkShift = ColumnBoolean(
            aTableName = modelTableName,
            aFieldName = "_is_auto_work_shift",
            aCaption = "Автоматически добавлять рабочие смены",
            aDefaultValue = isAutoCreateWorkShift
        ).apply {
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnWorker)

        if (parentObjectId == null) {
            alTableGroupColumn.add(columnShiftBegDoc)
        } else {
            addTableColumn(columnShiftBegDoc)
        }
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

        if (isWaybill) {
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

        (if (isWaybill) alFormColumn else alFormHiddenColumn).add(columnShiftNo)
        alFormColumn.add(columnShiftBegDoc)
        alFormColumn.add(columnShiftEndDoc)
        alFormColumn.add(columnShiftBegFact)
        alFormColumn.add(columnShiftEndFact)
        (if (isWaybill) alFormColumn else alFormHiddenColumn).add(columnWorkerTabNo)
        (if (isWaybill) alFormColumn else alFormHiddenColumn).add(columnWorkerName)
        (if (isWaybill) alFormColumn else alFormHiddenColumn).add(columnRun)
        (if (isAutoAddWorkShiftEnabled) alFormColumn else alFormHiddenColumn).add(columnIsAutoAddWorkShift)
        (if (isAutoCreateWorkShiftEnabled) alFormColumn else alFormHiddenColumn).add(columnIsAutoCreateWorkShift)

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
        if (isWaybill) {
            alChildData.add(ChildData("mms_show_trace", columnId, AppAction.FORM))
        }

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_work_shift_data", "shift_id", DependData.DELETE))

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
    }
}
