package foatto.core_server.app.server.data

import foatto.sql.CoreAdvancedResultSet
import foatto.core_server.app.server.column.iColumn

//--- специальный промежуточный/общий класс для DataInt и DataComboBox
//--- для общего способа получения целочисленного значения value
abstract class DataAbstractValue( aColumn: iColumn ) : DataAbstract( aColumn ) {

    var value = 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int ): Int {
        var posRS = aPosRS
        value = rs.getInt( posRS++ )
        return posRS
    }

    override fun getFieldSQLValue(index: Int ): String = "$value"

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun validate( obj: Number? ): Int = obj?.toInt() ?: 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    //--- перекрыто для нормальной работы HashMap.get при передаче описаний полей/столбцов между модулями
//    override fun hashCode(): Int = value
}
