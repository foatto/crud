package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mShiftWork : mAbstract() {

    private lateinit var os: ObjectSelector
    lateinit var columnShiftBegDoc: ColumnDateTimeInt
        private set
    lateinit var columnShiftEndDoc: ColumnDateTimeInt
        private set
    lateinit var columnShiftBegFact: ColumnDateTimeInt
        private set
    lateinit var columnShiftEndFact: ColumnDateTimeInt
        private set
    lateinit var columnObjectShiftWorkRun: ColumnString
        private set
    lateinit var columnObjectShiftWorkHourName: ColumnString
        private set
    lateinit var columnObjectShiftWorkHourValue: ColumnString
        private set
    lateinit var columnObjectShiftWorkLevelName: ColumnString
        private set
    lateinit var columnObjectShiftWorkLevelBeg: ColumnString
        private set
    lateinit var columnObjectShiftWorkLevelEnd: ColumnString
        private set
    lateinit var columnObjectShiftWorkLiquidName: ColumnString
        private set
    lateinit var columnObjectShiftWorkLiquidValue: ColumnString
        private set

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //        //--- это "путевой лист" или "рабочая смена"? (mms_waybill vs. mms_work_shift)
        //        boolean isWaybill = aliasConfig.getAlias().equals( "mms_waybill" );

        val parentObjectId = hmParentData["mms_object"]

        //        //--- определим опцию автосоздания рабочих смен
        //        Boolean isAutoWorkShift = false;
        //        if( ! isWaybill && parentObjectId != null ) {
        //            CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
        //                " SELECT is_auto_work_shift FROM MMS_object WHERE id = " ).append( parentObjectId ) );
        //            if( rs.next() ) isAutoWorkShift = rs.getInt( 1 ) != 0;
        //            rs.close();
        //        }

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_work_shift"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")
        columnUser = ColumnInt(tableName, "user_id")

        //----------------------------------------------------------------------------------------------------------------------

        //        ColumnString columnShiftNo = new ColumnString( tableName, "shift_no",
        //                                                isWaybill ? "Номер путевого листа" : "", STRING_COLUMN_WIDTH );

        columnShiftBegDoc = ColumnDateTimeInt(tableName, "beg_dt", "Начало", false, zoneId)
        columnShiftEndDoc = ColumnDateTimeInt(tableName, "end_dt", "Окончание", false, zoneId)
        columnShiftBegFact = ColumnDateTimeInt(tableName, "beg_dt_fact", "Начало факт.", false, zoneId)
        columnShiftEndFact = ColumnDateTimeInt(tableName, "end_dt_fact", "Окончание факт.", false, zoneId)

        //            ColumnInt columnWorkerID = new ColumnInt( "MMS_worker", "id" );
        //        ColumnInt columnWorker = new ColumnInt( tableName, "worker_id", columnWorkerID );
        //        ColumnString columnWorkerTabNo = new ColumnString( "MMS_worker", "tab_no", "Табельный номер", STRING_COLUMN_WIDTH );
        //        ColumnString columnWorkerName = new ColumnString( "MMS_worker", "name", "Ф.И.О.", STRING_COLUMN_WIDTH );
        //
        //            columnWorkerTabNo.setSelectorAlias( "mms_worker" );
        //            columnWorkerTabNo.addSelectorColumn( columnWorker, columnWorkerID );
        //            columnWorkerTabNo.addSelectorColumn( columnWorkerTabNo );
        //            columnWorkerTabNo.addSelectorColumn( columnWorkerName );

        //        ColumnDouble columnRun = new ColumnDouble( tableName, "run",
        //                                                              isWaybill ? "Пробег [км]" : "", 10, 1, 0.0 );

        columnObjectShiftWorkRun = ColumnString(tableName, "_run", "Пробег [км]", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkRun.isVirtual = true
        columnObjectShiftWorkRun.isSearchable = false
        columnObjectShiftWorkRun.rowSpan = if(parentObjectId == null) 3 else 4
        columnObjectShiftWorkHourName = ColumnString(tableName, "_work_name", "Оборудование", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkHourName.isVirtual = true
        columnObjectShiftWorkHourName.isSearchable = false
        columnObjectShiftWorkHourName.rowSpan = if(parentObjectId == null) 3 else 4
        columnObjectShiftWorkHourValue = ColumnString(tableName, "_work_hour", "Работа [час]", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkHourValue.isVirtual = true
        columnObjectShiftWorkHourValue.isSearchable = false
        columnObjectShiftWorkHourValue.rowSpan = if(parentObjectId == null) 3 else 4
        columnObjectShiftWorkLiquidName = ColumnString(tableName, "_liquid_name", "Топливо", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkLiquidName.isVirtual = true
        columnObjectShiftWorkLiquidName.isSearchable = false
        columnObjectShiftWorkLiquidName.rowSpan = if (parentObjectId == null) 3 else 4
        columnObjectShiftWorkLiquidValue = ColumnString(tableName, "_liquid_value", "Расход", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkLiquidValue.isVirtual = true
        columnObjectShiftWorkLiquidValue.isSearchable = false
        columnObjectShiftWorkLiquidValue.rowSpan = if(parentObjectId == null) 3 else 4
        columnObjectShiftWorkLevelName = ColumnString(tableName, "_level_name", "Ёмкость", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkLevelName.isVirtual = true
        columnObjectShiftWorkLevelName.isSearchable = false
        columnObjectShiftWorkLevelName.rowSpan = if(parentObjectId == null) 3 else 4
        columnObjectShiftWorkLevelBeg = ColumnString(tableName, "_level_beg", "Нач.остаток", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkLevelBeg.isVirtual = true
        columnObjectShiftWorkLevelBeg.isSearchable = false
        columnObjectShiftWorkLevelBeg.rowSpan = if(parentObjectId == null) 3 else 4
        columnObjectShiftWorkLevelEnd = ColumnString(tableName, "_level_end", "Кон.остаток", STRING_COLUMN_WIDTH)
        columnObjectShiftWorkLevelEnd.isVirtual = true
        columnObjectShiftWorkLevelEnd.isSearchable = false
        columnObjectShiftWorkLevelEnd.rowSpan = if(parentObjectId == null) 3 else 4

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnUser!!)
        //        alTableHiddenColumn.add( columnWorker );

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnUser!!)
        //        alFormHiddenColumn.add( columnWorker );

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(
            this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, if(parentObjectId == null) 0 else 1
        )

        //----------------------------------------------------------------------------------------------------------------------

        //        if( isWaybill ) addTableColumn( columnShiftNo );
        //        else alTableHiddenColumn.add( columnShiftNo );
        if(parentObjectId == null) {
            alTableGroupColumn.add(columnShiftBegDoc)
            addTableColumnVertNew(columnShiftEndDoc, columnShiftBegFact, columnShiftEndFact)
        } else addTableColumnVertNew(columnShiftBegDoc, columnShiftEndDoc, columnShiftBegFact, columnShiftEndFact)
        //            addTableColumnVertNew( columnShiftEndDoc );
        //            addTableColumnVertNew( columnShiftBegFact );
        //            addTableColumnVertNew( columnShiftEndFact );
        //        if( isWaybill ) {
        //            addTableColumn( columnShiftNo );
        //            addTableColumn( columnWorkerTabNo );
        //            addTableColumn( columnWorkerName );
        //            addTableColumn( columnRun );
        //        }
        //        else {
        //            alTableHiddenColumn.add( columnShiftNo );
        //            alTableHiddenColumn.add( columnWorkerTabNo );
        //            alTableHiddenColumn.add( columnWorkerName );
        //            alTableHiddenColumn.add( columnRun );
        //        }
        addTableColumnVertNew(columnObjectShiftWorkRun)
        addTableColumnVertNew(columnObjectShiftWorkHourName)
        addTableColumnVertNew(columnObjectShiftWorkHourValue)
        addTableColumnVertNew(columnObjectShiftWorkLiquidName)
        addTableColumnVertNew(columnObjectShiftWorkLiquidValue)
        addTableColumnVertNew(columnObjectShiftWorkLevelName)
        addTableColumnVertNew(columnObjectShiftWorkLevelBeg)
        addTableColumnVertNew(columnObjectShiftWorkLevelEnd)

        //        ( isWaybill ? alFormColumn : alFormHiddenColumn ).add( columnShiftNo );
        alFormColumn.add(columnShiftBegDoc)
        alFormColumn.add(columnShiftEndDoc)
        alFormColumn.add(columnShiftBegFact)
        alFormColumn.add(columnShiftEndFact)
        //        ( isWaybill ? alFormColumn : alFormHiddenColumn ).add( columnWorkerTabNo );
        //        ( isWaybill ? alFormColumn : alFormHiddenColumn ).add( columnWorkerName );
        //        ( isWaybill ? alFormColumn : alFormHiddenColumn ).add( columnRun );
        //        ( isWaybill ? alFormHiddenColumn : alFormColumn ).add( columnIsAutoWorkShift );
        alFormColumn.add(columnObjectShiftWorkRun)
        alFormColumn.add(columnObjectShiftWorkHourName)
        alFormColumn.add(columnObjectShiftWorkHourValue)
        alFormColumn.add(columnObjectShiftWorkLiquidName)
        alFormColumn.add(columnObjectShiftWorkLiquidValue)
        alFormColumn.add(columnObjectShiftWorkLevelName)
        alFormColumn.add(columnObjectShiftWorkLevelBeg)
        alFormColumn.add(columnObjectShiftWorkLevelEnd)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnShiftBegDoc)
        alTableSortDirect.add("DESC")
        alTableSortColumn.add(os.columnObjectName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("Отчёты", "mms_report_work_shift", columnID, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты", "mms_report_work_detail", columnID, AppAction.FORM))
        alChildData.add(ChildData("Отчёты", "mms_report_data_out", columnID, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnID, alChildData)

        alChildData.add(ChildData("mms_show_object", columnID, AppAction.FORM, true))
        //        alChildData.add( new ChildData( "mms_show_trace", columnID, AppAction.FORM ) );
    }
}
