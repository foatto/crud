package foatto.core_server.app.server.column

import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData

class ColumnString( aTableName: String, aFieldName: String, aCaption: String, aRows: Int, aCols: Int, aMaxSize: Int ) : ColumnSimple() {

    var cols = 0
    var rows = 0       // одновременно как флаг отображения: > 0 - отображать как многострочный текст

    var maxSize = 250              // max = 32000 - ограничение UTF-кодировщика текста

    var defaultValue: String? = null
    var emptyValueString = "-"

    var isRequired: Boolean = false
    var isPassword = false
    var isUseTrim = true

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    constructor( aTableName: String, aFieldName: String, aCaption: String, aCols: Int, aMaxSize: Int ) : this( aTableName, aFieldName, aCaption, 0, aCols, aMaxSize )
    constructor( aTableName: String, aFieldName: String, aCaption: String, aCols: Int ) : this( aTableName, aFieldName, aCaption, 0, aCols, 250 )

    init {
        tableName = aTableName
        addFieldName( aFieldName )
        caption = aCaption

        rows = aRows
        cols = aCols
        maxSize = aMaxSize
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setSavedDefault( userConfig: UserConfig ) {
        isSavedDefault = true
        defaultValue = userConfig.getUserProperty( savedDefaultPropertyName )
    }

    override fun saveDefault(stm: CoreAdvancedStatement, userConfig: UserConfig, hmColumnData: Map<iColumn, iData> ) {
        userConfig.saveUserProperty( stm, savedDefaultPropertyName, ( hmColumnData[ this ] as DataString ).text )
    }

    override fun getData() = DataString( this )

}
