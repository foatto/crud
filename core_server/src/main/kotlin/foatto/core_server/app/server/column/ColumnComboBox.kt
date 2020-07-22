package foatto.core_server.app.server.column

import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.iData

class ColumnComboBox( aTableName: String, aFieldName: String, aCaption: String, aDefaultValue: Int? = null ) :
    ColumnAbstractSelector( aTableName, aFieldName, aCaption, aDefaultValue ) {

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun saveDefault(stm: CoreAdvancedStatement, userConfig: UserConfig, hmColumnData: Map<iColumn, iData> ) {
        userConfig.saveUserProperty( stm, savedDefaultPropertyName, ( hmColumnData[ this ] as DataComboBox ).value.toString() )
    }

    override fun getData() = DataComboBox( this )
}
