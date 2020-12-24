//package foatto.spring.sql//package foatto.sql
//
//import foatto.core.util.AdvancedByteBuffer
//import org.springframework.jdbc.support.rowset.SqlRowSet
//import java.nio.ByteOrder
//import java.time.LocalDate
//import java.time.LocalDateTime
//import java.time.LocalTime
//
//class SpringResultSet(aDialect: SQLDialect, val rs: SqlRowSet) : CoreAdvancedResultSet(aDialect) {
//
//    override fun close() {}
//
//    override operator fun next(): Boolean {
//        isNext = rs.next()
//        return isNext
//    }
//
//    override fun getInt(index: Int): Int {
//        return rs.getInt(index)
//    }
//
//    override fun getLong(index: Int): Long {
//        return rs.getLong(index)
//    }
//
//    override fun getDouble(index: Int): Double {
//        return rs.getDouble(index)
//    }
//
//    override fun getString(index: Int): String {
//        return rs.getString(index) ?: ""
//    }
//
//    override fun getByteBuffer(index: Int, byteOrder: ByteOrder): AdvancedByteBuffer {
//        return if(dialect.isBinaryDataSupported) {
//            AdvancedByteBuffer(rs.getObject(index) as ByteArray, byteOrder)
//        } else {
//            AdvancedByteBuffer(rs.getString(index) ?: "")
//        }
//    }
//
//    override fun getTimeStampTime(index: Int) = ((rs.getTimestamp(index)?.time ?: 0) / 1000).toInt()
//
//    //!!! java.sql.SQLFeatureNotSupportedException: Not supported yet.
//    //override fun getDate(index: Int): LocalDate? = rs.getObject(index, LocalDate::class.java)
//    override fun getDate(index: Int): LocalDate? = rs.getDate(index)?.toLocalDate()
//
//    //!!! java.sql.SQLFeatureNotSupportedException: Not supported yet.
//    //override fun getTime(index: Int): LocalTime? = rs.getObject(index, LocalTime::class.java)
//    override fun getTime(index: Int): LocalTime? = rs.getTime(index)?.toLocalTime()
//
//    //!!! java.sql.SQLFeatureNotSupportedException: Not supported yet.
//    //override fun getDateTime(index: Int): LocalDateTime? = rs.getObject(index, LocalDateTime::class.java)
//    override fun getDateTime(index: Int): LocalDateTime? = rs.getTimestamp(index)?.toLocalDateTime()
//}
