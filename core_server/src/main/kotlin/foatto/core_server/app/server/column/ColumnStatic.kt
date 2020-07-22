package foatto.core_server.app.server.column

import foatto.core.link.TableCellAlign
import foatto.core.util.getRandomInt
import foatto.core_server.app.server.data.DataStatic

class ColumnStatic( aStaticValue: String, aTableAlign: TableCellAlign = TableCellAlign.LEFT ) : ColumnAbstract() {

    var staticValue = ""

    //--- без этого одинаковые/пустые статические столбцы начинают глючить
    private val salt = getRandomInt()

//    constructor( otherColumn: iColumn ) : this( otherColumn.caption )

    init {
        staticValue = aStaticValue
        tableAlign = aTableAlign

        caption = ""
        isVirtual = true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun isSortable() = false
    override fun getSortFieldName( index: Int ) = ""

    override fun getData() = DataStatic( this )

//--- перекрыто для нормальной работы HashMap.get при передаче описаний полей/столбцов между модулями ---------------------------------------------------------------------------------

    //--- без этого одинаковые/пустые статические столбцы начинают глючить
    override fun hashCode(): Int = staticValue.hashCode() + salt

    override fun equals( other: Any? ): Boolean {
        if( super.equals( other ) ) return true  // if( this == other ) return true;
        if( other == null ) return false
        if( other !is ColumnStatic ) return false

        if( staticValue != other.staticValue ) return false
        if( salt != other.salt ) return false

        return true
    }

}

