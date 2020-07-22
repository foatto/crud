package foatto.mms.core_mms.xy

import foatto.core.link.FormPinMode
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.FormColumnVisibleData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.*
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate
import java.time.LocalTime

class mShowObject : mAbstract() {

    lateinit var uodg: UODGSelector
        protected set
    lateinit var columnShowRangeType: ColumnRadioButton
        private set
    lateinit var columnShowBegDate: ColumnDate3Int
        private set
    lateinit var columnShowBegTime: ColumnTime3Int
        private set
    lateinit var columnShowEndDate: ColumnDate3Int
        private set
    lateinit var columnShowEndTime: ColumnTime3Int
        private set
    lateinit var columnShowZoneType: ColumnComboBox
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun getSaveButonCaption( aAliasConfig: AliasConfig ) = "Показать"

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val isShowObjectOnly = aliasConfig.alias == "mms_show_object"

        //----------------------------------------------------------------------------------------------------------------------

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent( stm, zoneId, hmParentData, false )

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------

        columnShowRangeType = ColumnRadioButton( tableName, "show_range_type", "Показать траекторию", if( isShowObjectOnly ) 0 else -1 )
            columnShowRangeType.addChoice( 0, "Не показывать" )  // хоть и не порядку значений, зато логичнее выглядит :)
            columnShowRangeType.addChoice( -1, "За указанный период" )
            columnShowRangeType.addChoice( 5 * 60, "За последние 5 минут" )
            columnShowRangeType.addChoice( 15 * 60, "За последние 15 минут" )
            columnShowRangeType.addChoice( 30 * 60, "За последние 30 минут" )
            columnShowRangeType.addChoice( 60 * 60, "За последний час" )
            columnShowRangeType.addChoice( 2 * 60 * 60, "За последние 2 часа" )
            columnShowRangeType.addChoice( 3 * 60 * 60, "За последние 3 часа" )
            columnShowRangeType.addChoice( 6 * 60 * 60, "За последние 6 часов" )
            columnShowRangeType.isVirtual = true

        columnShowBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода")
            columnShowBegDate.default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
            columnShowBegDate.isVirtual = true
            columnShowBegDate.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( -1 ) ) )
        columnShowBegTime = ColumnTime3Int(tableName, "beg_ho", "beg_mi", null, "Время начала периода")
            columnShowBegTime.default = LocalTime.of(arrDT[3], arrDT[4], arrDT[5])
            columnShowBegTime.isVirtual = true
            columnShowBegTime.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( -1 ) ) )
            columnShowBegTime.formPinMode = FormPinMode.ON

        columnShowEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Дата окончания периода")
            columnShowEndDate.default = LocalDate.of(arrDT[6], arrDT[7], arrDT[8])
            columnShowEndDate.isVirtual = true
            columnShowEndDate.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( -1 ) ) )
        columnShowEndTime = ColumnTime3Int(tableName, "end_ho", "end_mi", null, "Время окончания периода")
            columnShowEndTime.default = LocalTime.of(arrDT[9], arrDT[10], arrDT[11])
            columnShowEndTime.isVirtual = true
            columnShowEndTime.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( -1 ) ) )
            columnShowEndTime.formPinMode = FormPinMode.ON

        columnShowZoneType = ColumnComboBox( tableName, "show_zone_type", "Показывать геозоны", cShowAbstractObject.ZONE_SHOW_NONE )
            columnShowZoneType.addChoice( cShowAbstractObject.ZONE_SHOW_NONE, "нет" )
            columnShowZoneType.addChoice( cShowAbstractObject.ZONE_SHOW_ACTUAL, "актуальные" )
            columnShowZoneType.addChoice( cShowAbstractObject.ZONE_SHOW_ALL, "все" )
            columnShowZoneType.isVirtual = true
            columnShowZoneType.setSavedDefault( userConfig )

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add( columnID!! )

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns( tableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn )

        //----------------------------------------------------------------------------------------------------------------------

        ( if( isShowObjectOnly ) alFormHiddenColumn else alFormColumn ).add( columnShowRangeType )
        ( if( isShowObjectOnly ) alFormHiddenColumn else alFormColumn ).add( columnShowBegDate )
        ( if( isShowObjectOnly ) alFormHiddenColumn else alFormColumn ).add( columnShowBegTime )
        ( if( isShowObjectOnly ) alFormHiddenColumn else alFormColumn ).add( columnShowEndDate )
        ( if( isShowObjectOnly ) alFormHiddenColumn else alFormColumn ).add( columnShowEndTime )
        alFormColumn.add( columnShowZoneType )
    }
}
