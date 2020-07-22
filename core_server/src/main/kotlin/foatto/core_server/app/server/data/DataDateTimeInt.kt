package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.DateTime_DMYHM
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getDateTime
import foatto.core_server.app.server.column.ColumnAbstract
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement
import java.time.ZonedDateTime

class DataDateTimeInt(aColumn: iColumn) : DataAbstractDateTime(aColumn) {

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        zonedDateTime = getDateTime(zoneId, rs.getInt(posRS++))
        return posRS
    }

    override fun getFieldSQLValue(index: Int): String = zonedDateTime.toEpochSecond().toString()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setDateTime(second: Int) {
        zonedDateTime = getDateTime(zoneId, second)
    }
}
