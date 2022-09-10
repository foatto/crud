package foatto.core_server.service

import foatto.core.util.AdvancedLogger
import foatto.core.util.loadTextFile
import java.nio.charset.Charset

abstract class CoreDataTransfer(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {
        private val CONFIG_SQL_FILE = "sql_file"

        private val TYPE_UNKNOWN = 0
        private val TYPE_INT = 1
        private val TYPE_DOUBLE = 2
        private val TYPE_STRING = 3
    }

//----------------------------------------------------------------------------------------------------------------------

    override val isRunOnce: Boolean = true

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private lateinit var sqlFileName: String

//----------------------------------------------------------------------------------------------------------------------

    override fun loadConfig() {
        super.loadConfig()

        sqlFileName = hmConfig[CONFIG_SQL_FILE]!!
    }

    override fun cycle() {
        val alSQL = loadTextFile(
            sqlFileName,
            Charset.defaultCharset(),
            null,
            true
        )
        var tableName: String? = null
        var sSQL = ""
        val alFieldName = mutableListOf<String>()
        val alFieldType = mutableListOf<Int>()

        for (rawSQL in alSQL) {
            AdvancedLogger.debug(rawSQL)

            //--- убираем возможный комментарий до конца строки
            val sql = rawSQL.substringBefore("--")
            if (sql.isBlank()) {
                continue
            }

            //--- распознаём SQL-директиву
            val st = sql.split(" ", "(", ")", ",").filter { it.isNotBlank() }
            val word1 = st[0]
            val word2 = st.getOrNull(1)

            //--- создание таблицы или индекса
            if (word1.equals("CREATE", ignoreCase = true)) {
                //--- ещё не закончилась обработка предыдущей таблицы
                if (tableName != null) {
                    throw Throwable("Previous CREATE TABLE not finished: '$sql'")
                }

                if (word2.equals("TABLE", ignoreCase = true)) {
                    tableName = st[2]
                    sSQL += sql
                } else if (word2.equals("INDEX", ignoreCase = true) || word2.equals("CLUSTERED", ignoreCase = true)) {

//                    //--- множество таблиц с нумерацией в своём имени
//                    if (sql.contains("#")) {
//                        val alID = getTableID(rawSQL, commentPos)
//                        for (id in alID) {
//                            alStm[1].executeUpdate(sql.replace("#", id.toString()))
//                            alConn[1].commit()
//                        }
//                    } else {
                    alConn[1].executeUpdate(sql)
                    alConn[1].commit()
//                    }
                } else {
                    throw Throwable("Unknown CREATE command: '$sql'")
                }
            } else if (word1 == ";") {
                if (tableName == null) {
                    throw Throwable("Undefined table finishing: '$sql'")
                }
                sSQL += sql
                val sFieldName = alFieldName.joinToString(" , ")

//                //--- множество таблиц с нумерацией в своём имени
//                if (tableName.contains("#")) {
//                    val alID = getTableID(rawSQL, commentPos)
//                    for (id in alID) transferOneTable(
//                        sbSQL.toString().replace("#", id.toString()),
//                        tableName.replace("#", id.toString()),
//                        sbFieldName, alFieldType
//                    )
//                } else {
                transferOneTable(sSQL, tableName, sFieldName, alFieldType)
//                }
                AdvancedLogger.info("--- Data transfer for table $tableName completed. ---")
                tableName = null
                sSQL = ""
                alFieldName.clear()
                alFieldType.clear()
            } else if (word1.equals("PRIMARY", ignoreCase = true)) {
                if (tableName == null) {
                    throw Throwable("Primary key for undefined table: '$sql'")
                }
                sSQL += sql
            } else {
                if (tableName == null) {
                    throw Throwable("Primary key for undefined table: '$sql'")
                }
                val fieldType = if (word2.equals("INT", ignoreCase = true) ||
                    word2.equals("INTEGER", ignoreCase = true) ||
                    word2.equals("BIGINT", ignoreCase = true)
                ) {
                    TYPE_INT
                } else if (word2.equals("FLOAT", ignoreCase = true) ||
                    word2.equals("FLOAT8", ignoreCase = true) ||
                    word2.equals("REAL", ignoreCase = true)
                ) {
                    TYPE_DOUBLE
                } else if (word2.equals("VARCHAR", ignoreCase = true) ||
                    word2.equals("TEXT", ignoreCase = true)
                ) {
                    TYPE_STRING
                } else {
                    TYPE_UNKNOWN
                }
                if (fieldType == TYPE_UNKNOWN) {
                    throw Throwable("Unknown field type: 'sql'")
                }
                sSQL += sql
                alFieldName.add(word1)
                alFieldType.add(fieldType)
            }
        }
    }

//    private open fun getTableID(sql: String, commentPos: Int): ArrayList<Int> {
//        if (commentPos == -1) throw Throwable(
//            StringBuilder(
//                "ID-table not defined: "
//            ).append(sql).toString()
//        )
//        val stTableID = StringTokenizer(sql.substring(commentPos + 2), " ")
//        //--- критическая ошибка - должно быть как минимум два слова, разделённых пробелами/запятыми
//        if (stTableID.countTokens() < 2) throw Throwable(
//            StringBuilder(
//                "Too few tokens for ID-Table: "
//            ).append(sql).toString()
//        )
//        val idTableName = stTableID.nextToken()
//        val idFieldName = stTableID.nextToken()
//        val alID = ArrayList<Int>()
//        val rs = alStm[0].executeQuery(
//            StringBuilder(
//                " SELECT "
//            ).append(idFieldName).append(" FROM ").append(idTableName)
//                .append(" WHERE ").append(idFieldName).append(" <> 0 ")
//        )
//        while (rs.next()) alID.add(rs.getInt(1))
//        rs.close()
//        return alID
//    }

    private fun transferOneTable(
        sql: String,
        tableName: String,
        fieldNames: String,
        alFieldType: List<Int>
    ) {

        //--- создаём таблицу
        alConn[1].executeUpdate(sql)
        alConn[1].commit()

        //--- копируем данные
        val rs = alConn[0].executeQuery(" SELECT $fieldNames FROM $tableName ")
        var interCommitCounter = 0
        while (rs.next()) {
            var sFieldValue = ""
            var pos = 1
            for (fieldType in alFieldType) {
                sFieldValue += if (sFieldValue.isEmpty()) {
                    ""
                } else {
                    " , "
                } +
                    if (fieldType == TYPE_STRING) {
                        "'"
                    } else {
                        ""
                    } +
                    if (fieldType == TYPE_INT) {
                        rs.getLong(pos++).toString()
                    } else if (fieldType == TYPE_DOUBLE) {
                        rs.getDouble(pos++).toString()
                    } else {
                        rs.getString(pos++)
                    } +
                        if (fieldType == TYPE_STRING) {
                            "'"
                        } else {
                            ""
                        }
            }
            alConn[1].executeUpdate(" INSERT INTO $tableName ( $fieldNames ) VALUES ( $sFieldValue ); ")
            if (++interCommitCounter / 1000 == 0) {
                alConn[1].commit()
            }
        }
        rs.close()
        alConn[1].commit()
    }
}
