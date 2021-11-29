package foatto.core_server.app.server.column

import foatto.core.link.TableCellAlign
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection

class ColumnBoolean( aTableName: String, aFieldName: String, aCaption: String = "", aDefaultValue: Boolean? = null ) : ColumnSimple() {

    var defaultValue: Boolean? = aDefaultValue

    init {
        columnTableName = aTableName
        addFieldName( aFieldName )
        caption = aCaption

        tableAlign = TableCellAlign.CENTER
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setSavedDefault( userConfig: UserConfig ) {
        isSavedDefault = true
        defaultValue = userConfig.getUserProperty( savedDefaultPropertyName )?.toBoolean()
    }

    override fun saveDefault(conn: CoreAdvancedConnection, userConfig: UserConfig, hmColumnData: Map<iColumn, iData> ) {
        userConfig.saveUserProperty(conn, savedDefaultPropertyName, ( hmColumnData[ this ] as DataBoolean ).value.toString() )
    }

    override fun getData() = DataBoolean( this )
}
