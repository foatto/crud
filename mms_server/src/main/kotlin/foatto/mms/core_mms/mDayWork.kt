package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnGrid
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

    lateinit var columnWork: ColumnGrid
        private set

    lateinit var columnEnergo: ColumnGrid
        private set

    lateinit var columnAllSumEnergo: ColumnGrid
        private set

    lateinit var columnLiquid: ColumnGrid
        private set

    lateinit var columnAllSumLiquid: ColumnGrid
        private set

    lateinit var columnLevel: ColumnGrid
        private set

    lateinit var columnLevelLiquid: ColumnGrid
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
        columnWork = ColumnGrid(tableName, "_work", "Работа оборудования [час]").apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }

        columnEnergo = ColumnGrid(tableName, "_energo", "Расход/генерация э/энергии").apply {
            isVirtual = true
            isSearchable = false
        }

//        columnGroupSumEnergoName = ColumnString(tableName, "_group_energo_name", "Э/энергия", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumEnergoValue = ColumnString(tableName, "_group_energo_value", "Расход/Генерация", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }

        columnAllSumEnergo = ColumnGrid(tableName, "_all_energo", "Расход/генерация э/энергии").apply {
            isVirtual = true
            isSearchable = false
        }

        columnLiquid = ColumnGrid(tableName, "_liquid_name", "Расход топлива").apply {
            isVirtual = true
            isSearchable = false
        }

//        columnGroupSumLiquidName = ColumnString(tableName, "_group_liquid_name", "Топливо", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumLiquidValue = ColumnString(tableName, "_group_liquid_value", "Расход", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }

        columnAllSumLiquid = ColumnGrid(tableName, "_all_liquid", "Расход топлива").apply {
            isVirtual = true
            isSearchable = false
        }

        columnLevel = ColumnGrid(tableName, "_level", "Ёмкость, Нач.остаток, Кон.остаток").apply {
            isVirtual = true
            isSearchable = false
            rowSpan = 3
        }

        columnLevelLiquid = ColumnGrid(tableName, "_level_liquid_name", "Топливо, Заправка, Слив").apply {
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
        addTableColumnVertNew(columnWork)
        addTableColumnVertNew(columnEnergo,  /*columnGroupSumEnergoName, */ ColumnStatic(""), columnAllSumEnergo)
        addTableColumnVertNew(columnLiquid,  /*columnGroupSumLiquidName, */ ColumnStatic(""), columnAllSumLiquid)
        addTableColumnVertNew(columnLevel)
        addTableColumnVertNew(columnLevelLiquid)

        alFormColumn.add(columnDate)
        alFormColumn.add(columnRun)
        //alFormColumn.add(columnWork)

        //alFormColumn.add(columnEnergo)
//        alFormColumn.add(columnGroupSumEnergoName)
//        alFormColumn.add(columnGroupSumEnergoValue)
        //alFormColumn.add(columnAllSumEnergo)

        //alFormColumn.add(columnLiquid)
//        alFormColumn.add(columnGroupSumLiquidName)
//        alFormColumn.add(columnGroupSumLiquidValue)
        //alFormColumn.add(columnAllSumLiquid)

        //alFormColumn.add(columnLevel)

        //alFormColumn.add(columnLevelLiquid)

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
