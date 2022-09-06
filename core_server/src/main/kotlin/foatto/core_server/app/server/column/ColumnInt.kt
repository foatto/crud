package foatto.core_server.app.server.column

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection

class ColumnInt(
    aTableName: String,
    aFieldName: String,
    aCaption: String = "",
    aCols: Int = 0,
    aDefaultValue: Int? = null
) : ColumnSimple(), iUniqableColumn {

    companion object {
        val BIN = 2
        val DEC = 10
        val HEX = 16
    }

    var defaultValue: Int? = null
    var cols = 0
    val maxSize = 250
    var radix = DEC

    var emptyValue: Int? = null
        private set
    var emptyText: String? = null
        private set

    var isRequired: Boolean = false

    var minValue: Int? = null
    var maxValue: Int? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- hidden-поле
    constructor(aTableName: String, aFieldName: String, aDefaultValue: Int?) : this(aTableName, aFieldName, "", 0, aDefaultValue)

    //--- link-поле
    constructor(aTableName: String, aFieldName: String, aLinkColumn: ColumnInt, aDefaultValue: Int? = null) : this(aTableName, aFieldName, "", 0, aDefaultValue) {
        linkColumn = aLinkColumn
    }

    init {
        columnTableName = aTableName
        addFieldName(aFieldName)
        caption = aCaption

        cols = aCols
        defaultValue = aDefaultValue
        //setTableCellAlign( 2 ); сливается со строками справа

        isWordWrap = false
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setSavedDefault(userConfig: UserConfig) {
        isSavedDefault = true
        defaultValue = userConfig.getUserProperty(savedDefaultPropertyName)?.toIntOrNull(radix) ?: defaultValue
    }

    override fun saveDefault(application: iApplication, conn: CoreAdvancedConnection, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>) {
        application.saveUserProperty(conn, userConfig, savedDefaultPropertyName, (hmColumnData[this] as DataInt).intValue.toString(radix))
    }

    override fun getData() = DataInt(this)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setEmptyData(aEmptyValue: Int, aEmptyText: String) {
        emptyValue = aEmptyValue
        emptyText = aEmptyText
    }

}
