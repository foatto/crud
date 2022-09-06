package foatto.core_server.app.server.column

import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection

class ColumnBoolean(aTableName: String, aFieldName: String, aCaption: String = "", aDefaultValue: Boolean? = null) : ColumnSimple() {

    init {
        columnTableName = aTableName
        addFieldName(aFieldName)
        caption = aCaption

        tableAlign = TableCellAlign.CENTER
    }

    var defaultValue: Boolean? = aDefaultValue
    var arrSwitchText: Array<String> = emptyArray()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setSavedDefault(userConfig: UserConfig) {
        isSavedDefault = true
        defaultValue = userConfig.getUserProperty(savedDefaultPropertyName)?.toBoolean()
    }

    override fun saveDefault(application: iApplication, conn: CoreAdvancedConnection, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>) {
        application.saveUserProperty(conn, userConfig, savedDefaultPropertyName, (hmColumnData[this] as DataBoolean).value.toString())
    }

    override fun getData() = DataBoolean(this)
}
