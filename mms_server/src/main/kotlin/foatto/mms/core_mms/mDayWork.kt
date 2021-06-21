package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnStatic
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mDayWork : mAbstract() {

    private lateinit var os: ObjectSelector

    lateinit var columnDate: ColumnDate3Int
        private set

    lateinit var columnRun: ColumnString
        private set

    lateinit var columnWorkName: ColumnString
        private set
    lateinit var columnWorkValue: ColumnString
        private set

    lateinit var columnEnergoName: ColumnString
        private set
    lateinit var columnEnergoValue: ColumnString
        private set

    //    lateinit var columnGroupSumEnergoName: ColumnString
//        private set
//    lateinit var columnGroupSumEnergoValue: ColumnString
//        private set
    lateinit var columnAllSumEnergoName: ColumnString
        private set
    lateinit var columnAllSumEnergoValue: ColumnString
        private set

    lateinit var columnLiquidName: ColumnString
        private set
    lateinit var columnLiquidValue: ColumnString
        private set

    //    lateinit var columnGroupSumLiquidName: ColumnString
//        private set
//    lateinit var columnGroupSumLiquidValue: ColumnString
//        private set
    lateinit var columnAllSumLiquidName: ColumnString
        private set
    lateinit var columnAllSumLiquidValue: ColumnString
        private set

    lateinit var columnLevelName: ColumnString
        private set
    lateinit var columnLevelBeg: ColumnString
        private set
    lateinit var columnLevelEnd: ColumnString
        private set

    lateinit var columnLevelLiquidName: ColumnString
        private set
    lateinit var columnLevelLiquidInc: ColumnString
        private set
    lateinit var columnLevelLiquidDec: ColumnString
        private set

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_day_work"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")
        columnUser = ColumnInt(tableName, "user_id")

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Дата")

        columnRun = ColumnString(tableName, "_run", "Пробег [км]", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }
        columnWorkName = ColumnString(tableName, "_work_name", "Оборудование", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }
        columnWorkValue = ColumnString(tableName, "_work_hour", "Работа [час]", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }

        columnEnergoName = ColumnString(tableName, "_energo_name", "Э/энергия", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
//            rowSpan = 2
        }
        columnEnergoValue = ColumnString(tableName, "_energo_value", "Расход/Генерация", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
//            rowSpan = 2
        }
//        columnGroupSumEnergoName = ColumnString(tableName, "_group_energo_name", "Э/энергия", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumEnergoValue = ColumnString(tableName, "_group_energo_value", "Расход/Генерация", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
        columnAllSumEnergoName = ColumnString(tableName, "_all_energo_name", "Э/энергия", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
        }
        columnAllSumEnergoValue = ColumnString(tableName, "_all_energo_value", "Расход/Генерация", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
        }

        columnLiquidName = ColumnString(tableName, "_liquid_name", "Топливо", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
//            rowSpan = 2
        }
        columnLiquidValue = ColumnString(tableName, "_liquid_value", "Расход", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
//            rowSpan = 2
        }
//        columnGroupSumLiquidName = ColumnString(tableName, "_group_liquid_name", "Топливо", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumLiquidValue = ColumnString(tableName, "_group_liquid_value", "Расход", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
        columnAllSumLiquidName = ColumnString(tableName, "_all_liquid_name", "Топливо", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
        }
        columnAllSumLiquidValue = ColumnString(tableName, "_all_liquid_value", "Расход", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
        }

        columnLevelName = ColumnString(tableName, "_level_name", "Ёмкость", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }
        columnLevelBeg = ColumnString(tableName, "_level_beg", "Нач.остаток", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }
        columnLevelEnd = ColumnString(tableName, "_level_end", "Кон.остаток", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }

        columnLevelLiquidName = ColumnString(tableName, "_level_liquid_name", "Топливо", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }
        columnLevelLiquidInc = ColumnString(tableName, "_level_liquid_inc", "Заправка", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }
        columnLevelLiquidDec = ColumnString(tableName, "_level_liquid_dec", "Слив", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnUser!!)

        alTableGroupColumn.add(columnDate)

        alFormHiddenColumn.add(columnID)
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

        addTableColumnVertNew(columnRun)
        addTableColumnVertNew(columnWorkName)
        addTableColumnVertNew(columnWorkValue)
        addTableColumnVertNew(columnEnergoName,  /*columnGroupSumEnergoName, */ ColumnStatic(""), columnAllSumEnergoName)
        addTableColumnVertNew(columnEnergoValue, /*columnGroupSumEnergoValue,*/ ColumnStatic(""), columnAllSumEnergoValue)
        addTableColumnVertNew(columnLiquidName,  /*columnGroupSumLiquidName, */ ColumnStatic(""), columnAllSumLiquidName)
        addTableColumnVertNew(columnLiquidValue, /*columnGroupSumLiquidValue,*/ ColumnStatic(""), columnAllSumLiquidValue)
        addTableColumnVertNew(columnLevelName)
        addTableColumnVertNew(columnLevelBeg)
        addTableColumnVertNew(columnLevelEnd)
        addTableColumnVertNew(columnLevelLiquidName)
        addTableColumnVertNew(columnLevelLiquidInc)
        addTableColumnVertNew(columnLevelLiquidDec)

        alFormColumn.add(columnDate)
        alFormColumn.add(columnRun)
        alFormColumn.add(columnWorkName)
        alFormColumn.add(columnWorkValue)

        alFormColumn.add(columnEnergoName)
        alFormColumn.add(columnEnergoValue)
//        alFormColumn.add(columnGroupSumEnergoName)
//        alFormColumn.add(columnGroupSumEnergoValue)
        alFormColumn.add(columnAllSumEnergoName)
        alFormColumn.add(columnAllSumEnergoValue)

        alFormColumn.add(columnLiquidName)
        alFormColumn.add(columnLiquidValue)
//        alFormColumn.add(columnGroupSumLiquidName)
//        alFormColumn.add(columnGroupSumLiquidValue)
        alFormColumn.add(columnAllSumLiquidName)
        alFormColumn.add(columnAllSumLiquidValue)

        alFormColumn.add(columnLevelName)
        alFormColumn.add(columnLevelBeg)
        alFormColumn.add(columnLevelEnd)

        alFormColumn.add(columnLevelLiquidName)
        alFormColumn.add(columnLevelLiquidInc)
        alFormColumn.add(columnLevelLiquidDec)

        //----------------------------------------------------------------------------------------------------------------------

        alTableSortColumn.add(columnDate)
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
            alChildData.add(ChildData("mms_show_trace", columnID, AppAction.FORM, true, true))
            alChildData.add(ChildData("mms_show_object", columnID, AppAction.FORM))
        }

        MMSFunction.fillChildDataForPeriodicReports(columnID, alChildData)
        MMSFunction.fillChildDataForLiquidIncDecReports(columnID, alChildData, withIncWaybillReport = true, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_work_detail", columnID, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnID, alChildData, withMovingDetailReport = true)
        MMSFunction.fillChildDataForEnergoOverReports(columnID, alChildData)
        MMSFunction.fillChildDataForOverReports(columnID, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_data_out", columnID, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnID, alChildData)

        if (!isMovingMode) {
            alChildData.add(ChildData("mms_show_object", columnID, AppAction.FORM, true))
            alChildData.add(ChildData("mms_show_trace", columnID, AppAction.FORM))
        }
    }
}
