package foatto.core_server.app.server.data

import foatto.core.util.getDateTime
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet

class DataDateTimeInt(aColumn: iColumn) : DataAbstractDateTime(aColumn) {

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS

        zonedDateTime = getDateTime(zoneId, rs.getInt(posRS++))
        arrErrorValue = null
        errorText = null

        return posRS
    }

    override fun getFieldSQLValue(index: Int): String = zonedDateTime.toEpochSecond().toString()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setDateTime(second: Int) {
        zonedDateTime = getDateTime(zoneId, second)
        arrErrorValue = null
        errorText = null
    }
}
