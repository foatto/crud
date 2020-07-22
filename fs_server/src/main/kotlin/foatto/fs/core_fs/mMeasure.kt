package foatto.fs.core_fs

import foatto.core.link.AppAction
import foatto.core.link.TableCellAlign
import foatto.core.util.getZoneId
import foatto.core_server.app.server.*
import foatto.core_server.app.server.column.*
import foatto.fs.core_fs.device.mDevice.Companion.getDeviceDescrMap
import foatto.sql.CoreAdvancedStatement

class mMeasure : mAbstract() {

    private val zoneId0 = getZoneId( 0 )

    lateinit var columnMeasureCurSize: ColumnInt
        private set
    lateinit var columnMeasureAllSize: ColumnInt
        private set

    lateinit var columnMeasureSensorCount: ColumnInt
        private set
    lateinit var columnMeasureSensorInfo1: ColumnString
        private set
    lateinit var columnMeasureSensorInfo2: ColumnString
        private set
    lateinit var columnMeasureSensorInfo3: ColumnString
        private set
    lateinit var columnMeasureSensorInfo4: ColumnString
        private set

    lateinit var columnMeasureError: ColumnString
        private set

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {
        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val tmDeviceType = getDeviceDescrMap()

        //----------------------------------------------------------------------------------------------------------------------------------------

        tableName = "FS_measure"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt( "SYSTEM_users", "id" )
        columnUser = ColumnInt( tableName, "user_id", columnUserID, userConfig.userID )
        val columnUserName = ColumnString( "SYSTEM_users", "full_name", "Владелец", STRING_COLUMN_WIDTH )
        if( userConfig.isAdmin ) {
            columnUserName.selectorAlias = "system_user_people"
            columnUserName.addSelectorColumn( columnUser!!, columnUserID )
            columnUserName.addSelectorColumn( columnUserName )
        }

        val columnObjectID = ColumnInt( "FS_object", "id" )
        val columnObject = ColumnInt( tableName, "object_id", columnObjectID )

        val columnObjectName = ColumnString( "FS_object", "name", "Наименование объекта", STRING_COLUMN_WIDTH )
        val columnObjectInfo = ColumnString( "FS_object", "info", "Дополнительная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize )
            columnObjectInfo.rowSpan = 5

            columnObjectName.selectorAlias = "fs_object"
            columnObjectName.addSelectorColumn( columnObject, columnObjectID )
            columnObjectName.addSelectorColumn( columnObjectName )
            columnObjectName.addSelectorColumn( columnObjectInfo )

        val columnDeviceID = ColumnInt( "FS_device", "id" )
        val columnDevice = ColumnInt( tableName, "device_id", columnDeviceID )

        val columnDeviceSerial = ColumnString( "FS_device", "serial_no", "Серийный номер прибора", STRING_COLUMN_WIDTH )
        val columnDeviceType = ColumnComboBox( "FS_device", "type", "Тип прибора", STRING_COLUMN_WIDTH )
            tmDeviceType.forEach { columnDeviceType.addChoice( it.key, it.value ) }
        val columnDeviceVersion = ColumnString( "FS_device", "version", "Версия прибора", STRING_COLUMN_WIDTH )
        val columnDeviceSpec = ColumnString( "FS_device", "spec", "Спецификация прибора", STRING_COLUMN_WIDTH )

            columnDeviceSerial.selectorAlias = "fs_device"
            columnDeviceSerial.addSelectorColumn( columnDevice, columnDeviceID )
            columnDeviceSerial.addSelectorColumn( columnDeviceSerial )
            columnDeviceSerial.addSelectorColumn( columnDeviceType )
            columnDeviceSerial.addSelectorColumn( columnDeviceVersion )
            columnDeviceSerial.addSelectorColumn( columnDeviceSpec )

        val columnMeasureBegTime = ColumnDateTimeInt( tableName, "beg_time", "Дата/время измерения", true, zoneId0 )

        columnMeasureCurSize = ColumnInt( tableName, "cur_size", "Загружено", STRING_COLUMN_WIDTH )
            columnMeasureCurSize.tableAlign = TableCellAlign.RIGHT
        columnMeasureAllSize = ColumnInt( tableName, "all_size", "Всего", STRING_COLUMN_WIDTH )
            columnMeasureAllSize.tableAlign = TableCellAlign.RIGHT

        columnMeasureSensorCount = ColumnInt( tableName, "_sensor_count", "Кол-во датчиков", STRING_COLUMN_WIDTH )
            columnMeasureSensorCount.isVirtual = true

        columnMeasureSensorInfo1 = ColumnString( tableName, "_sensor_info_1", "Датчики", STRING_COLUMN_WIDTH)
            columnMeasureSensorInfo1.isVirtual = true

        columnMeasureSensorInfo2 = ColumnString( tableName, "_sensor_info_2", "Время", STRING_COLUMN_WIDTH)
            columnMeasureSensorInfo2.isVirtual = true

        columnMeasureSensorInfo3 = ColumnString( tableName, "_sensor_info_3", "Данные", STRING_COLUMN_WIDTH)
            columnMeasureSensorInfo3.isVirtual = true

        columnMeasureSensorInfo4 = ColumnString( tableName, "_sensor_info_4", "Тарировка", STRING_COLUMN_WIDTH)
            columnMeasureSensorInfo4.isVirtual = true

        columnMeasureError = ColumnString( tableName, "_error", "Ошибка", STRING_COLUMN_WIDTH)
            columnMeasureError.isVirtual = true
            columnMeasureError.rowSpan = 6
        
        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add( columnID!! )
        alTableHiddenColumn.add( columnUser!! )
        alTableHiddenColumn.add( columnObject )
        alTableHiddenColumn.add( columnDevice )

        alTableGroupColumn.add( columnMeasureBegTime )

        addTableColumnVertNew( ColumnStatic( "Объект:" ), ColumnStatic( "" ), ColumnStatic( "" ), ColumnStatic( "" ), ColumnStatic( "" ), ColumnStatic( "" ) )
        addTableColumnVertNew( columnObjectName, columnObjectInfo )
        addTableColumnVertNew( ColumnStatic( "Серийный №:" ), ColumnStatic( "Тип прибора:" ), ColumnStatic( "Версия:" ), ColumnStatic( "Спецификация:" ),
                               ColumnStatic( "" ), columnMeasureSensorInfo1 )
        addTableColumnVertNew( columnDeviceSerial, columnDeviceType, columnDeviceVersion, columnDeviceSpec,
                               ColumnStatic( "" ), columnMeasureSensorInfo2 )
        addTableColumnVertNew( ColumnStatic( "Загружено:" ), ColumnStatic( "Всего:" ), ColumnStatic( "" ), ColumnStatic( "Кол-во датчиков:" ),
                               ColumnStatic( "" ), columnMeasureSensorInfo3 )
        addTableColumnVertNew( columnMeasureCurSize, columnMeasureAllSize, ColumnStatic( "" ), columnMeasureSensorCount,
                               ColumnStatic( "" ), columnMeasureSensorInfo4 )
        addTableColumnVertNew( columnMeasureError )

        alFormHiddenColumn.add( columnID!! )
        alFormHiddenColumn.add( columnUser!! )
        alFormHiddenColumn.add( columnObject )
        alFormHiddenColumn.add( columnDevice )

        alFormColumn.add( columnUserName )
        alFormColumn.add( columnObjectName )
        alFormColumn.add( columnObjectInfo )
        alFormColumn.add( columnDeviceSerial )
        alFormColumn.add( columnDeviceType )
        alFormColumn.add( columnDeviceVersion )
        alFormColumn.add( columnDeviceSpec )
        alFormColumn.add( columnMeasureBegTime )
        alFormColumn.add( columnMeasureCurSize )
        alFormColumn.add( columnMeasureAllSize )
        alFormColumn.add( columnMeasureSensorCount )
        alFormColumn.add( columnMeasureSensorInfo1 )
        alFormColumn.add( columnMeasureSensorInfo2 )
        alFormColumn.add( columnMeasureSensorInfo3 )
        alFormColumn.add( columnMeasureSensorInfo4 )
        alFormColumn.add( columnMeasureError )

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add( columnMeasureBegTime )
        alTableSortDirect.add( "DESC" )
        alTableSortColumn.add( columnObjectName )
        alTableSortDirect.add( "ASC" )
        alTableSortColumn.add( columnDeviceSerial )
        alTableSortDirect.add( "ASC" )

        //----------------------------------------------------------------------------------------

        hmParentColumn[ "system_user" ] = columnUser!!
        hmParentColumn[ "fs_object" ] = columnObject
        hmParentColumn[ "fs_device" ] = columnDevice

        //----------------------------------------------------------------------------------------------------------------------

//        alChildData.add(ChildData("Отчёты...", "mms_report_summary", columnID!!, AppAction.FORM, true))
//        alChildData.add(ChildData("Отчёты...", "mms_report_day_work", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_work_shift", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_waybill", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_waybill_compare", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_summary_without_waybill", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_inc_waybill", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_liquid_dec", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_work_detail", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_over_speed", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_parking", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_object_zone", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_moving_detail", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_over_weight", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_over_turn", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_over_pressure", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_over_temperature", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_over_voltage", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Отчёты...", "mms_report_data_out", columnID!!, AppAction.FORM))
//
        alChildData.add( ChildData( "fs_graphic_measure", columnID!!, AppAction.FORM, true ) )

//        alChildData.add(ChildData("Графики...", "mms_graphic_liquid", columnID!!, AppAction.FORM, true))
//        alChildData.add(ChildData("Графики...", "mms_graphic_weight", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Графики...", "mms_graphic_turn", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Графики...", "mms_graphic_pressure", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Графики...", "mms_graphic_temperature", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Графики...", "mms_graphic_voltage", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Графики...", "mms_graphic_power", columnID!!, AppAction.FORM))
//        alChildData.add(ChildData("Графики...", "mms_graphic_speed", columnID!!, AppAction.FORM))
    }
}
