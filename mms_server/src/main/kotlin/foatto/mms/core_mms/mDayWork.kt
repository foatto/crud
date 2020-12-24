package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mDayWork : mAbstract() {

    private lateinit var os: ObjectSelector
    lateinit var columnObjectDayWorkDate: ColumnDate3Int
        private set
    lateinit var columnObjectDayWorkRun: ColumnString
        private set
    lateinit var columnObjectDayWorkHourName: ColumnString
        private set
    lateinit var columnObjectDayWorkHourValue: ColumnString
        private set
    lateinit var columnObjectDayWorkLiquidName: ColumnString
        private set
    lateinit var columnObjectDayWorkLiquidValue: ColumnString
        private set
    lateinit var columnObjectDayWorkLevelName: ColumnString
        private set
    lateinit var columnObjectDayWorkLevelBeg: ColumnString
        private set
    lateinit var columnObjectDayWorkLevelEnd: ColumnString
        private set

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_day_work"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")
        columnUser = ColumnInt(tableName, "user_id")

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnObjectDayWorkDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Дата")

        columnObjectDayWorkRun = ColumnString(tableName, "_run", "Пробег [км]", STRING_COLUMN_WIDTH)
        columnObjectDayWorkRun.isVirtual = true
        columnObjectDayWorkRun.isSearchable = false
        columnObjectDayWorkRun.rowSpan = 3
        columnObjectDayWorkHourName = ColumnString(tableName, "_work_name", "Оборудование", STRING_COLUMN_WIDTH)
        columnObjectDayWorkHourName.isVirtual = true
        columnObjectDayWorkHourName.isSearchable = false
        columnObjectDayWorkHourName.rowSpan = 3
        columnObjectDayWorkHourValue = ColumnString(tableName, "_work_hour", "Работа [час]", STRING_COLUMN_WIDTH)
        columnObjectDayWorkHourValue.isVirtual = true
        columnObjectDayWorkHourValue.isSearchable = false
        columnObjectDayWorkHourValue.rowSpan = 3
        columnObjectDayWorkLiquidName = ColumnString(tableName, "_liquid_name", "Топливо", STRING_COLUMN_WIDTH)
        columnObjectDayWorkLiquidName.isVirtual = true
        columnObjectDayWorkLiquidName.isSearchable = false
        columnObjectDayWorkLiquidName.rowSpan = 3
        columnObjectDayWorkLiquidValue = ColumnString(tableName, "_liquid_value", "Расход", STRING_COLUMN_WIDTH)
        columnObjectDayWorkLiquidValue.isVirtual = true
        columnObjectDayWorkLiquidValue.isSearchable = false
        columnObjectDayWorkLiquidValue.rowSpan = 3
        columnObjectDayWorkLevelName = ColumnString(tableName, "_level_name", "Ёмкость", STRING_COLUMN_WIDTH)
        columnObjectDayWorkLevelName.isVirtual = true
        columnObjectDayWorkLevelName.isSearchable = false
        columnObjectDayWorkLevelName.rowSpan = 3
        columnObjectDayWorkLevelBeg = ColumnString(tableName, "_level_beg", "Нач.остаток", STRING_COLUMN_WIDTH)
        columnObjectDayWorkLevelBeg.isVirtual = true
        columnObjectDayWorkLevelBeg.isSearchable = false
        columnObjectDayWorkLevelBeg.rowSpan = 3
        columnObjectDayWorkLevelEnd = ColumnString(tableName, "_level_end", "Кон.остаток", STRING_COLUMN_WIDTH)
        columnObjectDayWorkLevelEnd.isVirtual = true
        columnObjectDayWorkLevelEnd.isSearchable = false
        columnObjectDayWorkLevelEnd.rowSpan = 3

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)

        alTableGroupColumn.add(columnObjectDayWorkDate)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)

        //----------------------------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(
            model = this,
            isRequired = true,
            isSelector = true,
            alTableHiddenColumn = alTableHiddenColumn,
            alFormHiddenColumn = alFormHiddenColumn,
            alFormColumn = alFormColumn,
            hmParentColumn = hmParentColumn,
            aSingleObjectMode = false,
            addedStaticColumnCount = 0
        )

        //----------------------------------------------------------------------------------------------------------------------------------------

        addTableColumnVertNew(columnObjectDayWorkRun)
        addTableColumnVertNew(columnObjectDayWorkHourName)
        addTableColumnVertNew(columnObjectDayWorkHourValue)
        addTableColumnVertNew(columnObjectDayWorkLiquidName)
        addTableColumnVertNew(columnObjectDayWorkLiquidValue)
        addTableColumnVertNew(columnObjectDayWorkLevelName)
        addTableColumnVertNew(columnObjectDayWorkLevelBeg)
        addTableColumnVertNew(columnObjectDayWorkLevelEnd)

        alFormColumn.add(columnObjectDayWorkDate)
        alFormColumn.add(columnObjectDayWorkRun)
        alFormColumn.add(columnObjectDayWorkHourName)
        alFormColumn.add(columnObjectDayWorkHourValue)
        alFormColumn.add(columnObjectDayWorkLiquidName)
        alFormColumn.add(columnObjectDayWorkLiquidValue)
        alFormColumn.add(columnObjectDayWorkLevelName)
        alFormColumn.add(columnObjectDayWorkLevelBeg)
        alFormColumn.add(columnObjectDayWorkLevelEnd)

        //----------------------------------------------------------------------------------------------------------------------

        alTableSortColumn.add(columnObjectDayWorkDate)
        alTableSortDirect.add("DESC")
        alTableSortColumn.add(os.columnObjectName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        //--- define the user of mobile objects only
        val hsPermissionDW = userConfig.userPermission["mms_day_work"]
        val hsPermissionSW = userConfig.userPermission["mms_shift_work"]
        val isMovingMode = hsPermissionDW != null && hsPermissionDW.contains(cStandart.PERM_ACCESS) &&
            (hsPermissionSW == null || !hsPermissionSW.contains(cStandart.PERM_ACCESS))

        if (isMovingMode) {
            alChildData.add(ChildData("mms_show_trace", columnID!!, AppAction.FORM, true, true))
            alChildData.add(ChildData("mms_show_object", columnID!!, AppAction.FORM))
        }

        alChildData.add(ChildData("Отчёты...", "mms_report_summary", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты...", "mms_report_day_work", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_work_shift", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_waybill_compare", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc_waybill", columnID!!, AppAction.FORM))
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

        if (!isMovingMode) {
            alChildData.add(ChildData("mms_show_object", columnID!!, AppAction.FORM, true))
            alChildData.add(ChildData("mms_show_trace", columnID!!, AppAction.FORM))
        }
    }
}
