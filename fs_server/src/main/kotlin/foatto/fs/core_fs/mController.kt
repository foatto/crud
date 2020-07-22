package foatto.fs.core_fs

import foatto.core_server.app.server.*
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.sql.CoreAdvancedStatement

class mController : mAbstract() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {
        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "FS_controller"

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

        val columnControllerSerialNo = ColumnString( tableName, "serial_no", "Серийный номер", STRING_COLUMN_WIDTH )
            columnControllerSerialNo.isRequired = true
            columnControllerSerialNo.setUnique( true, null )
        val columnControllerVersion = ColumnString( tableName, "version", "Версия", STRING_COLUMN_WIDTH )
        val columnControllerStatus = ColumnString( tableName, "status", "Статус", STRING_COLUMN_WIDTH )
            columnControllerStatus.isEditable = false

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add( columnID!! )
        alTableHiddenColumn.add( columnUser!! )

        addTableColumn( columnControllerSerialNo )
        addTableColumn( columnControllerVersion )
        addTableColumn( columnControllerStatus )

        alFormHiddenColumn.add( columnID!! )
        alFormHiddenColumn.add( columnUser!! )

        alFormColumn.add( columnUserName )
        alFormColumn.add( columnControllerSerialNo )
        alFormColumn.add( columnControllerVersion )
        alFormColumn.add( columnControllerStatus )

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add( columnControllerSerialNo )
        alTableSortDirect.add( "ASC" )

        //----------------------------------------------------------------------------------------

        hmParentColumn[ "system_user" ] = columnUser!!
    }

}
