package foatto.fs.core_fs

import foatto.core_server.app.server.*
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.sql.CoreAdvancedStatement

class mObject : mAbstract() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {
        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "FS_object"

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

        val columnObjectName = ColumnString( tableName, "name", "Наименование", STRING_COLUMN_WIDTH )
            columnObjectName.isRequired = true
            //columnObjectName.setUnique(  true, null  ); - у разных клиентов/пользователей могут быть объекты с одинаковыми названиями

        val columnObjectInfo = ColumnString( tableName, "info", "Дополнительная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize )

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add( columnID!! )
        alTableHiddenColumn.add( columnUser!! )

        addTableColumn( columnObjectName )
        addTableColumn( columnObjectInfo )

        alFormHiddenColumn.add( columnID!! )
        alFormHiddenColumn.add( columnUser!! )

        alFormColumn.add( columnUserName )
        alFormColumn.add( columnObjectName )
        alFormColumn.add( columnObjectInfo )

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add( columnObjectName )
        alTableSortDirect.add( "ASC" )

        //----------------------------------------------------------------------------------------

        hmParentColumn[ "system_user" ] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add( ChildData( "fs_measure", columnID!!, true ) )

        //----------------------------------------------------------------------------------------------------------------------

        alDependData.add( DependData( "FS_measure", "object_id" ) )
    }

}
