package foatto.sql

import foatto.core.util.AdvancedByteBuffer
import java.nio.ByteOrder
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AdvancedResultSet(aDialect: SQLDialect, val rs: ResultSet) : CoreAdvancedResultSet(aDialect) {

    override fun close() {
        rs.close()
        //rs = null
    }

    override operator fun next(): Boolean {
        isNext = rs.next()
        return isNext
    }

    override fun getInt(index: Int): Int {
        return rs.getInt(index)
    }

    override fun getLong(index: Int): Long {
        return rs.getLong(index)
    }

    override fun getDouble(index: Int): Double {
        return rs.getDouble(index)
    }

    override fun getString(index: Int): String {
        return rs.getString(index) ?: ""
    }

    override fun getByteBuffer(index: Int, byteOrder: ByteOrder): AdvancedByteBuffer {
        return if(dialect.isBinaryDataSupported) {
            AdvancedByteBuffer(rs.getBytes(index), byteOrder)
        } else {
            AdvancedByteBuffer(rs.getString(index))
        }
    }

    override fun getTimeStampTime(index: Int) = (rs.getTimestamp(index).time / 1000).toInt()

    override fun getDate(index: Int): LocalDate? = rs.getObject(index, LocalDate::class.java)

    override fun getTime(index: Int): LocalTime? = rs.getObject(index, LocalTime::class.java)

    override fun getDateTime(index: Int): LocalDateTime? = rs.getObject(index, LocalDateTime::class.java)

}
