package foatto.core_server.app.server.column

import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData

class ColumnInt( aTableName: String, aFieldName: String, aCaption: String = "", aCols: Int = 0, aDefaultValue: Int? = null ) : ColumnSimple() {

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
    constructor( aTableName: String, aFieldName: String, aDefaultValue: Int? ) : this( aTableName, aFieldName, "", 0, aDefaultValue )

    //--- link-поле
    constructor( aTableName: String, aFieldName: String, aLinkColumn: ColumnInt, aDefaultValue: Int? = null ) : this( aTableName, aFieldName, "", 0, aDefaultValue ) {
        linkColumn = aLinkColumn
    }

    init {
        tableName = aTableName
        addFieldName( aFieldName )
        caption = aCaption

        cols = aCols
        defaultValue = aDefaultValue
        //setTableCellAlign( 2 ); сливается со строками справа

        isWordWrap = false
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setSavedDefault( userConfig: UserConfig ) {
        isSavedDefault = true
        defaultValue = userConfig.getUserProperty( savedDefaultPropertyName )?.toIntOrNull( radix )
    }

    override fun saveDefault(stm: CoreAdvancedStatement, userConfig: UserConfig, hmColumnData: Map<iColumn, iData> ) {
        userConfig.saveUserProperty( stm, savedDefaultPropertyName, ( hmColumnData[ this ] as DataInt ).value.toString( radix ) )
    }

    override fun getData() = DataInt( this )

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setEmptyData( aEmptyValue: Int, aEmptyText: String ) {
        emptyValue = aEmptyValue
        emptyText = aEmptyText
    }

}
