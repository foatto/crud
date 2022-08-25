package foatto.sql

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getRandomInt
import foatto.core.util.getRandomLong

abstract class CoreAdvancedStatement(val conn: CoreAdvancedConnection) {

    //--- для тех случаев, когда лень только из-за этого передавать Connection в параметрах ---
    val dialect: CoreSQLDialectEnum = conn.dialect

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    abstract fun close()

    abstract fun executeQuery(sql: String): CoreAdvancedResultSet

    fun executeUpdate(sql: CharSequence, withReplication: Boolean = true): Int {
        val s = sql.toString()
        val result = executeUpdate(s)
        if (withReplication) {
            conn.addReplicationSQL(s)
        }
        return result
    }

    protected abstract fun executeUpdate(sql: String): Int

    fun getPreLimit(limit: Int): StringBuilder = conn.getPreLimit(limit)
    fun getMidLimit(limit: Int): StringBuilder = conn.getMidLimit(limit)
    fun getPostLimit(limit: Int): StringBuilder = conn.getPostLimit(limit)
    //    public boolean isSlowCount() { return conn.isSlowCount(); }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

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
                if (checkExisting(arrTableName[i], arrFieldIds[i], nextId)) {
                    continue@OUT
                }
            }
            return nextId
        }
    }

    fun checkExisting(aTableName: String, aFieldCheck: String, aValue: Any, aFieldID: String? = null, id: Number = 0): Boolean {
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

        val rs = executeQuery(
            """
                SELECT $aFieldCheck 
                FROM $aTableName 
                WHERE $aFieldCheck = $stringBound$aValue$stringBound 
                $andFieldIDCheck 
            """
        )
        val isExist = rs.next()
        rs.close()
        return isExist
    }

    fun checkExisting(aTableName: String, alFieldCheck: List<Pair<String, Any>>, aFieldID: String? = null, id: Int = 0): Boolean {
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
        val rs = executeQuery(
            """
                SELECT ${alFieldCheck[0].first} 
                FROM $aTableName 
                WHERE $checks 
                $andFieldIDCheck 
            """
        )
        val isExist = rs.next()
        rs.close()
        return isExist
    }

    // '${bbData.getHex( null, false )}'
    fun getHexValue(bbData: AdvancedByteBuffer): String {
        val hex = bbData.getHex(null, false)
        return when (dialect) {
            CoreSQLDialectEnum.H2 -> "X'$hex'"
            CoreSQLDialectEnum.MSSQL -> "0x$hex"
            CoreSQLDialectEnum.POSTGRESQL -> "'\\x$hex'"
            else -> "'$hex'"
        }
    }
}
