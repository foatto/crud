package foatto.fs.core_fs.device

import foatto.core_server.app.server.*
import foatto.core_server.app.server.column.*
import foatto.sql.CoreAdvancedStatement
import java.util.*

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class mDevice : mAbstract() {

    companion object {
        const val DEVICE_TYPE_F101_04 = 1
        const val DEVICE_TYPE_KW4 = 2
        const val DEVICE_TYPE_2D = 3
        const val DEVICE_TYPE_1D = 4
        const val DEVICE_TYPE_VALVE = 5
        const val DEVICE_TYPE_FLOW_METER = 6

        fun getDeviceDescrMap(): TreeMap<Int, String> {
            val tmDeviceType = TreeMap<Int,String>()

            tmDeviceType[ DEVICE_TYPE_F101_04 ] = "F101.04"
            tmDeviceType[ DEVICE_TYPE_KW4 ] = "KW4"
            tmDeviceType[ DEVICE_TYPE_2D ] = "2D"
            tmDeviceType[ DEVICE_TYPE_1D ] = "1D"
            tmDeviceType[ DEVICE_TYPE_VALVE ] = "Клапан"
            tmDeviceType[ DEVICE_TYPE_FLOW_METER ] = "Расходомер"

            return tmDeviceType
        }
    }

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {
        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val tmDeviceType = getDeviceDescrMap()

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "FS_device"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt( "SYSTEM_users", "id" )
        columnUser = ColumnInt( tableName, "user_id", columnUserID, userConfig.userID )
        val columnUserName = ColumnString( "SYSTEM_users", "full_name", "Владелец", STRING_COLUMN_WIDTH )
        if( userConfig.isAdmin ) {
            columnUserName.selectorAlias = "system_user_people"
            columnUserName.addSelectorColumn( columnUser!!, columnUserID )
            columnUserName.addSelectorColumn( columnUserName )
        }

        val columnDeviceType = ColumnComboBox(tableName, "type", "Тип прибора", STRING_COLUMN_WIDTH)
            tmDeviceType.forEach { columnDeviceType.addChoice( it.key, it.value ) }
        val columnDeviceSerialNo = ColumnString( tableName, "serial_no", "Серийный номер", STRING_COLUMN_WIDTH )
            columnDeviceSerialNo.isRequired = true
            //columnDeviceSerialNo.setUnique( true, null ) - серийные номера приборов разного типа могут пересекаться
        val columnDeviceVersion = ColumnString( tableName, "version", "Версия", STRING_COLUMN_WIDTH )
        val columnDeviceSpec = ColumnString( tableName, "spec", "Спецификация", STRING_COLUMN_WIDTH )

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add( columnID!! )
        alTableHiddenColumn.add( columnUser!! )

        alTableGroupColumn.add( columnDeviceType )

        addTableColumn( columnDeviceSerialNo )
        addTableColumn( columnDeviceVersion )
        addTableColumn( columnDeviceSpec )

        alFormHiddenColumn.add( columnID!! )
        alFormHiddenColumn.add( columnUser!! )

        alFormColumn.add( columnUserName )
        alFormColumn.add( columnDeviceType )
        alFormColumn.add( columnDeviceSerialNo )
        alFormColumn.add( columnDeviceVersion )
        alFormColumn.add( columnDeviceSpec )

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add( columnDeviceSerialNo )
        alTableSortDirect.add( "ASC" )

        //----------------------------------------------------------------------------------------

        hmParentColumn[ "system_user" ] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add( ChildData( "fs_measure", columnID!!, true ) )

        //----------------------------------------------------------------------------------------

        alDependData.add( DependData( "FS_measure", "device_id" ) )
    }

}
