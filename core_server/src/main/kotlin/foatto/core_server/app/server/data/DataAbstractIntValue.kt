package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet

//--- специальный промежуточный/общий класс для DataInt и DataComboBox
//--- для общего способа получения целочисленного значения value
abstract class DataAbstractIntValue(aColumn: iColumn) : DataAbstract(aColumn) {

    var intValue = 0

    var errorValue: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS

        intValue = rs.getInt(posRS++)
        errorText = null
        errorValue = null

        return posRS
    }

    override fun getFieldSQLValue(index: Int): String = "$intValue"

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun validate(obj: Number?): Int = obj?.toInt() ?: 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    //--- перекрыто для нормальной работы HashMap.get при передаче описаний полей/столбцов между модулями
//    override fun hashCode(): Int = value
}
