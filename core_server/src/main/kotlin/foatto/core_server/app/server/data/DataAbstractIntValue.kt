package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet

//--- специальный промежуточный/общий класс для DataInt и DataComboBox
//--- для общего способа получения целочисленного значения value
abstract class DataAbstractIntValue(aColumn: iColumn) : DataAbstract(aColumn) {

    var intValue: Int = 0

    protected var errorValue: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS

        intValue = rs.getInt(posRS++)
        clearError()

        return posRS
    }

    override fun getFieldSQLValue(index: Int): String = "$intValue"

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getUniqueCheckValue(index: Int): Any = intValue

    override fun setUniqueCheckingError(message: String) {
        setError(intValue.toString(), message)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setError(aErrorValue: String?, aErrorText: String?) {
        errorValue = aErrorValue
        errorText = aErrorText
    }

    fun clearError() {
        setError(null, null)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun validate(obj: Number?): Int = obj?.toInt() ?: 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    //--- перекрыто для нормальной работы HashMap.get при передаче описаний полей/столбцов между модулями
//    override fun hashCode(): Int = value
}
