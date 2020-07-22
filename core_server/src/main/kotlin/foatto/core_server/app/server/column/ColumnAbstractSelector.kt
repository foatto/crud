package foatto.core_server.app.server.column

import foatto.core_server.app.server.UserConfig

abstract class ColumnAbstractSelector( aTableName: String, aFieldName: String, aCaption: String, aDefaultValue: Int? = null ) : ColumnSimple() {

    val alSelectorData = mutableListOf<SelectorData>()

    var defaultValue: Int? = null

    //--- требуется выбрать любое значение, кроме указанного (комбинация isRequired & empty/exceptValue)
    var requiredExcept: Int? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init  {
        tableName = aTableName
        addFieldName( aFieldName )
        caption = aCaption
        defaultValue = aDefaultValue
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setSavedDefault( userConfig: UserConfig ) {
        //--- берём из сохранённых значений из базы, если есть, иначе оставляем прежнее значение
        defaultValue = userConfig.getUserProperty( savedDefaultPropertyName )?.toIntOrNull() ?: defaultValue
        isSavedDefault = true
    }

//--- свои методы ---------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun addChoice( value: Int, tableDescr: String, formDescr: String = tableDescr, icon: String = ""/*, image: String = ""*/ ) {
        alSelectorData.add( SelectorData( value, tableDescr, formDescr, icon/*, image*/ ) )
    }

    fun findChoiceIndex( value: Int ) = alSelectorData.indexOfFirst { it.value == value }
    fun findChoiceTableDescr( value: Int ): String {
        val index = findChoiceIndex( value )
        return if( index == -1 ) "[ Неизвестное значение = $value ]"
               else alSelectorData[ index ].tableDescr
    }
    fun findChoiceFormDescr( value: Int ): String {
        val index = findChoiceIndex( value )
        return if( index == -1 ) "[ Неизвестное значение = $value ]"
               else alSelectorData[index].formDescr
    }
    fun findChoiceIcon( value: Int ): String {
        val index = findChoiceIndex( value )
        return if( index == -1 ) "" else alSelectorData[ index ].icon
    }
//    fun findChoiceImage( value: Int ): String {
//        val index = findChoiceIndex( value )
//        return if( index == -1 ) "" else alSelectorData[ index ].image
//    }
}

data class SelectorData( val value: Int, val tableDescr: String, val formDescr: String, val icon: String/*, val image: String*/ )
