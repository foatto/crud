package foatto.core_server.app.server.column

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection

class ColumnComboBox(aTableName: String, aFieldName: String, aCaption: String, aDefaultValue: Int? = null) :
    ColumnAbstractSelector(aTableName, aFieldName, aCaption, aDefaultValue) {

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun saveDefault(application: iApplication, conn: CoreAdvancedConnection, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>) {
        application.saveUserProperty(
            conn = conn,
            userId = null,
            userConfig = userConfig,
            upName = savedDefaultPropertyName,
            upValue = (hmColumnData[this] as DataComboBox).intValue.toString()
        )
    }

    override fun getData() = DataComboBox(this)
}
