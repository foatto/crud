package foatto.core_server.app.server.column

import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataDouble
import foatto.core_server.app.server.data.iData

class ColumnDouble( aTableName: String, aFieldName: String, aCaption: String = "", aCols: Int = 0, aPrecision: Int = -1, aDefaultValue: Double? = null ) : ColumnSimple() {

    var cols = 0
    var defaultValue: Double? = null
    //--- == -1 : кол-во цифр после запятой определяется автоматически (последние нули убираются)
    var precision = -1

    var emptyValue: Double? = null
        private set
    var emptyText: String? = null
        private set

    var isRequired: Boolean = false

    var minValue: Double? = null
    var maxValue: Double? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    constructor( aTableName: String, aFieldName: String, aDefaultValue: Double? ) : this( aTableName, aFieldName, "", 0, -1, aDefaultValue )

    init {
        tableName = aTableName
        addFieldName( aFieldName )
        caption = aCaption

        cols = aCols
        precision = aPrecision
        defaultValue = aDefaultValue
        //setTableCellAlign( 2 ); сливается со строками справа

        isWordWrap = false
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setSavedDefault( userConfig: UserConfig ) {
        isSavedDefault = true
        defaultValue = userConfig.getUserProperty( savedDefaultPropertyName )?.toDoubleOrNull()
    }

    override fun saveDefault(stm: CoreAdvancedStatement, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>) {
        userConfig.saveUserProperty( stm, savedDefaultPropertyName, ( hmColumnData[ this ] as DataDouble ).value.toString() )
    }

    override fun getData() = DataDouble( this )

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setEmptyData( aEmptyValue: Double, aEmptyText: String ) {
        emptyValue = aEmptyValue
        emptyText = aEmptyText
    }
}
