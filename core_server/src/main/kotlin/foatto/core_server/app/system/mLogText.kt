package foatto.core_server.app.system

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mLogText : mAbstract() {

    lateinit var columnLogFileName: ColumnString
        private set
    lateinit var columnLogRow: ColumnString
        private set

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //        //--- может быть null при вызове из "Модули системы"
        //        Integer objectId = hmParentData.get( "mms_object" );
        //        if( objectId == null ) objectId = 0;

        //----------------------------------------------------------------------------------------------------------------------

        tableName = FAKE_TABLE_NAME

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id").apply {
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnLogFileName = ColumnString(tableName, "_log_file_name", "-", STRING_COLUMN_WIDTH)
        columnLogFileName.isVirtual = true
        columnLogRow = ColumnString(tableName, "_log_row", "-", STRING_COLUMN_WIDTH)
        columnLogRow.isVirtual = true

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)

        alTableGroupColumn.add(columnLogFileName)

        addTableColumn(columnLogRow)

        //--- у этой таблицы нет ID-поля, поэтому её записи невозможно показать в виде формы
    }
}

