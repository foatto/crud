package foatto.server.sql

import foatto.core.util.getRandomInt
import foatto.core.util.getRandomLong
import foatto.server.util.AdvancedByteBuffer
import java.sql.Connection

abstract class CoreAdvancedConnection {
    protected lateinit var conn: Connection

    fun executeUpdate(sql: String): Int {
        val stm = conn.createStatement()
        val result = stm.executeUpdate(sql)
        stm.close()

        return result
    }

    fun executeQuery(sql: String): CoreAdvancedResultSet {
        val stm = conn.createStatement()
        return AdvancedResultSet(stm, stm.executeQuery(sql))
        //--- will be close by AdvancedResultSet.close()
    }

    fun commit() {
        conn.commit()
    }

    fun rollback() {
        conn.rollback()
    }

    fun close() {
        conn.close()
    }

    fun getNextIntId(aTableName: String, aFieldId: String): Int {
        return getNextIntId(arrayOf(aTableName), arrayOf(aFieldId))
    }

    //--- вернуть следующее уникальное значение поля среди нескольких таблиц
    fun getNextIntId(arrTableName: Array<String>, arrFieldIds: Array<String>): Int {
        var nextId: Int
        OUT@
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            for (i in arrTableName.indices) {
                if (checkExisting(arrTableName[i], arrFieldIds[i], nextId, null, 0)) {
                    continue@OUT
                }
            }
            return nextId
        }
    }

    fun getNextLongId(aTableName: String, aFieldId: String): Long {
        return getNextLongId(arrayOf(aTableName), arrayOf(aFieldId))
    }

    //--- вернуть следующее уникальное значение поля среди нескольких таблиц
    fun getNextLongId(arrTableName: Array<String>, arrFieldIds: Array<String>): Long {
        var nextId: Long
        OUT@
        while (true) {
            nextId = getRandomLong()
            if (nextId == 0L) {
                continue
            }
            for (i in arrTableName.indices) {
                if (checkExisting(arrTableName[i], arrFieldIds[i], nextId, null, 0)) {
                    continue@OUT
                }
            }
            return nextId
        }
    }

    fun checkExisting(aTableName: String, aFieldCheck: String, aValue: Any, aFieldID: String?, id: Number): Boolean {
        val stringBound = if (aValue is String) {
            "'"
        } else {
            ""
        }
        val andFieldIDCheck = if (aFieldID != null) {
            " AND $aFieldID <> $id "
        } else {
            ""
        }

        val stm = conn.createStatement()
        val rs = stm.executeQuery(
            """
                SELECT $aFieldCheck 
                FROM $aTableName 
                WHERE $aFieldCheck = $stringBound$aValue$stringBound 
                $andFieldIDCheck 
            """.trimIndent()
        )
        val isExist = rs.next()
        rs.close()
        stm.close()

        return isExist
    }

    fun checkExisting(aTableName: String, alFieldCheck: List<Pair<String, Any>>, aFieldID: String?, id: Number): Boolean {
        var checks = ""
        alFieldCheck.forEach { fieldCheckData ->
            if (checks.isNotEmpty()) {
                checks += " AND "
            }
            val stringBound = if (fieldCheckData.second is String) {
                "'"
            } else {
                ""
            }
            checks += "${fieldCheckData.first} = $stringBound${fieldCheckData.second}$stringBound"
        }
        val andFieldIDCheck = if (aFieldID != null) {
            " AND $aFieldID <> $id "
        } else {
            ""
        }
        val stm = conn.createStatement()
        val rs = stm.executeQuery(
            """
                SELECT ${alFieldCheck[0].first} 
                FROM $aTableName 
                WHERE $checks 
                $andFieldIDCheck 
            """.trimIndent()
        )
        val isExist = rs.next()
        rs.close()
        stm.close()

        return isExist
    }

    fun getHexValue(bbData: AdvancedByteBuffer): String {
        val hex = bbData.getHex(null, false)
        return "'\\x$hex'"
    }

}
