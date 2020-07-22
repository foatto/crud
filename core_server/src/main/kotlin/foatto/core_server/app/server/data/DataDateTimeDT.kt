package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet

class DataDateTimeDT(aColumn: iColumn) : DataAbstractDateTime(aColumn) {

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        zonedDateTime = rs.getDateTime(posRS++)?.atZone(zoneId) ?: NULL_DATE_TIME
        return posRS
    }

    override fun getFieldSQLValue(index: Int): String {
        return "'${zonedDateTime.year}-${zonedDateTime.monthValue}-${zonedDateTime.dayOfMonth} ${zonedDateTime.hour}:${zonedDateTime.minute}:${zonedDateTime.second}'"
    }
}