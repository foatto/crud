package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import java.time.LocalDate

class DataDate3Int(aColumn: iColumn) : DataAbstractDate(aColumn) {

    override val fieldSQLCount: Int
        get() = 3

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS

        localDate = LocalDate.of(rs.getInt(posRS++), rs.getInt(posRS++), rs.getInt(posRS++))
        arrErrorValue = null
        errorText = null

        return posRS
    }

    override fun loadFromDefault() {
        val cd = column as ColumnDate3Int
        localDate = LocalDate.from(cd.default)
        arrErrorValue = null
        errorText = null
    }

    override fun getFieldSQLValue(index: Int): String {
        return when(index) {
            0 -> "${localDate.year}"
            1 -> "${localDate.monthValue}"
            2 -> "${localDate.dayOfMonth}"
            else -> throw Throwable("DataDate3Int.getFieldSQLValue: wrong index = $index")
        }
    }
}
