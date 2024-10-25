package foatto.server.sql

import foatto.server.util.AdvancedByteBuffer
import java.nio.ByteOrder
import java.sql.ResultSet
import java.sql.Statement
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AdvancedResultSet(
    val stm: Statement,
    val rs: ResultSet,
) : CoreAdvancedResultSet() {

    override fun getResultSet(): ResultSet = rs

    override fun close() {
        rs.close()
        stm.close()
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

    override fun getByteBuffer(index: Int, byteOrder: ByteOrder): AdvancedByteBuffer = AdvancedByteBuffer(rs.getBytes(index), byteOrder)

    override fun getTimeStampTime(index: Int) = (rs.getTimestamp(index).time / 1000).toInt()

    override fun getDate(index: Int): LocalDate? = rs.getObject(index, LocalDate::class.java)

    override fun getTime(index: Int): LocalTime? = rs.getObject(index, LocalTime::class.java)

    override fun getDateTime(index: Int): LocalDateTime? = rs.getObject(index, LocalDateTime::class.java)

}
