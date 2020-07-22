package foatto.fs.core_fs.graphic

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mGraphicFS : mAbstract() {

//    lateinit var columnShowRangeType: ColumnRadioButton
//        private set
//    lateinit var columnShowBegDate: ColumnDate
//        private set
//    lateinit var columnShowBegTime: ColumnTime
//        private set
//    lateinit var columnShowEndDate: ColumnDate
//        private set
//    lateinit var columnShowEndTime: ColumnTime
//        private set

    lateinit var columnMeasure: ColumnInt
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun getSaveButonCaption( aAliasConfig: AliasConfig): String = "Показать"

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {
        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "FS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------

        columnMeasure = ColumnInt( tableName, "measure_id", "measure", STRING_COLUMN_WIDTH, hmParentData[ "fs_measure" ]!!.toInt() )

//        columnShowRangeType = ColumnRadioButton( tableName, "show_range_type", "За какой период показывать", 0 )
//            columnShowRangeType.addChoice( 0, "За указанный период" )
//            columnShowRangeType.addChoice( 15 * 60, "За последние 15 минут" )
//            columnShowRangeType.addChoice( 30 * 60, "За последние 30 минут" )
//            columnShowRangeType.addChoice( 60 * 60, "За последний час" )
//            columnShowRangeType.addChoice( 2 * 60 * 60, "За последние 2 часа" )
//            columnShowRangeType.addChoice( 3 * 60 * 60, "За последние 3 часа" )
//            columnShowRangeType.addChoice( 6 * 60 * 60, "За последние 6 часов" )
//            columnShowRangeType.isVirtual = true
//
//        columnShowBegDate = ColumnDate( tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода", 2010, 2100, timeZone )
////            columnShowBegDate.setDefaultDate( arrDT[ 0 ], arrDT[ 1 ], arrDT[ 2 ] )
//            columnShowBegDate.isVirtual = true
//            columnShowBegDate.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( 0 ) ) )
//        columnShowBegTime = ColumnTime( tableName, "beg_ho", "beg_mi", "Время начала периода", timeZone )
////            columnShowBegTime.setDefaultTime( arrDT[ 3 ], arrDT[ 4 ], arrDT[ 5 ] )
//            columnShowBegTime.isVirtual = true
//            columnShowBegTime.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( 0 ) ) )
//            columnShowBegTime.formPinMode = CoreFormCellInfo.FORM_PIN_ON
//
//        columnShowEndDate = ColumnDate( tableName, "end_ye", "end_mo", "end_da", "Дата окончания периода", 2010, 2100, timeZone )
////            columnShowEndDate.setDefaultDate( arrDT[ 6 ], arrDT[ 7 ], arrDT[ 8 ] )
//            columnShowEndDate.isVirtual = true
//            columnShowEndDate.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( 0 ) ) )
//        columnShowEndTime = ColumnTime( tableName, "end_ho", "end_mi", "Время окончания периода", timeZone )
////            columnShowEndTime.setDefaultTime( arrDT[ 9 ], arrDT[ 10 ], arrDT[ 11 ] )
//            columnShowEndTime.isVirtual = true
//            columnShowEndTime.addFormVisible( FormColumnVisibleData( columnShowRangeType, true, intArrayOf( 0 ) ) )
//            columnShowEndTime.formPinMode = CoreFormCellInfo.FORM_PIN_ON

        //----------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add( columnID!! )

        alFormColumn.add( columnMeasure )


        //----------------------------------------------------------------------------------------------------------------------

//        os = ObjectSelector()
//        os.fillColumns( this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1 )

        //----------------------------------------------------------------------------------------------------------------------

//        alFormColumn.add( columnShowRangeType )
//        alFormColumn.add( columnShowBegDate )
//        alFormColumn.add( columnShowBegTime )
//        alFormColumn.add( columnShowEndDate )
//        alFormColumn.add( columnShowEndTime )

//        hmParentColumn[ "fs_measure" ] = columnObject

    }
}

