package foatto.core_server.app.server.column

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection

class ColumnRadioButton( aTableName: String, aFieldName: String, aCaption: String, aDefaultValue: Int? = null ) :
      ColumnAbstractSelector( aTableName, aFieldName, aCaption, aDefaultValue ) {

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun saveDefault(conn: CoreAdvancedConnection, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>) {
        userConfig.saveUserProperty(conn, savedDefaultPropertyName, ( hmColumnData[ this ] as DataRadioButton ).intValue.toString() )
    }

    override fun getData() = DataRadioButton( this )
}
