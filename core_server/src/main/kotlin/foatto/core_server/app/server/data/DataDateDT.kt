package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.ColumnDateDT
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import java.time.LocalDate

class DataDateDT(aColumn: iColumn) : DataAbstractDate(aColumn) {

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS

        localDate = rs.getDate(posRS++) ?: NULL_DATE
        arrErrorValue = null
        errorText = null

        return posRS
    }

    override fun loadFromDefault() {
        val cd = column as ColumnDateDT
        localDate = LocalDate.from(cd.default)
        arrErrorValue = null
        errorText = null
    }

    override fun getFieldCellName(index: Int): String = "${getFieldName(0)}_$index".replace(".", "___")

    override fun getFieldSQLValue(index: Int): String {
        return "'${localDate.year}-${localDate.monthValue}-${localDate.dayOfMonth}'"
    }
}
