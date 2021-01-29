package foatto.core_server.app.server.column

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedStatement

class ColumnRadioButton( aTableName: String, aFieldName: String, aCaption: String, aDefaultValue: Int? = null ) :
      ColumnAbstractSelector( aTableName, aFieldName, aCaption, aDefaultValue ) {

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun saveDefault(stm: CoreAdvancedStatement, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>) {
        userConfig.saveUserProperty( stm, savedDefaultPropertyName, ( hmColumnData[ this ] as DataRadioButton ).intValue.toString() )
    }

    override fun getData() = DataRadioButton( this )
}
