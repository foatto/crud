@file:JvmName("mDeviceCommand")
package foatto.mms.core_mms.device

import foatto.core_server.app.server.*
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.column.ColumnString
import foatto.mms.core_mms.ds.MMSHandler
import foatto.sql.CoreAdvancedStatement

class mDeviceCommand : mAbstract() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_device_command"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------------------------

        val columnDeviceType = ColumnRadioButton( tableName, "type", "Тип устройства" )
            MMSHandler.fillDeviceTypeColumn( columnDeviceType )

        val columnName = ColumnString( tableName, "name", "Наименование", STRING_COLUMN_WIDTH )
            columnName.setUnique( true, "" )
        val columnDescr = ColumnString( tableName, "descr", "Описание", STRING_COLUMN_WIDTH )
            columnDescr.setUnique( true, "" )

        val columnCommand = ColumnString( tableName, "cmd", "Команда", 12, STRING_COLUMN_WIDTH, textFieldMaxSize )

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add( columnID!! )

        addTableColumn( columnDeviceType )
        addTableColumn( columnName )
        addTableColumn( columnDescr )
        addTableColumn( columnCommand )

        alFormHiddenColumn.add( columnID!! )

        alFormColumn.add( columnDeviceType )
        alFormColumn.add( columnName )
        alFormColumn.add( columnDescr )
        alFormColumn.add( columnCommand )

        //----------------------------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add( columnDescr )
        alTableSortDirect.add( "ASC" )

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData.add( ChildData( "mms_device_command_history", columnID!! ) )

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData.add( DependData( "MMS_device_command_history", "command_id" ) )
        alDependData.add( DependData( "MMS_device", "cmd_on_id" ) )
        alDependData.add( DependData( "MMS_device", "cmd_off_id" ) )
    }
}
