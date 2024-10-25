package foatto.server.sql

import foatto.server.util.AdvancedByteBuffer
import java.nio.ByteOrder
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

abstract class CoreAdvancedResultSet() {

    abstract fun getResultSet(): ResultSet

    //--- судя по документации, реализация функции isAfterLast в различных jdbc-драйверах не гарантирована,
    //--- поэтому эмулируем её через запоминание последнего состояния функции next
    protected var isNext = false
    val isAfterLast: Boolean
        get() = !isNext

    abstract fun close()

    abstract fun next(): Boolean

    //--- нумерация столбцов подразумевается с 1, как в обычном java-sql
    abstract fun getInt(index: Int): Int
    abstract fun getLong(index: Int): Long
    abstract fun getDouble(index: Int): Double
    abstract fun getString(index: Int): String
    abstract fun getByteBuffer(index: Int, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): AdvancedByteBuffer

    abstract fun getTimeStampTime(index: Int): Int

    abstract fun getDate(index: Int): LocalDate?
    abstract fun getTime(index: Int): LocalTime?
    abstract fun getDateTime(index: Int): LocalDateTime?
}
