package foatto.sql

import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class AdvancedConnection(dbConfig: DBConfig) : JdbcAdvancedConnection(dbConfig) {

    val conn: Connection

    init {
        val dbProperty = Properties()
        dbProperty["user"] = dbConfig.login
        dbProperty["password"] = dbConfig.password

        if (dialect == CoreSQLDialectEnum.H2) {
            // пока вроде нечего
        } else if (dialect == CoreSQLDialectEnum.MSSQL) {
            // пока вроде нечего
        } else if (dialect == CoreSQLDialectEnum.ORACLE) {
            // пока вроде нечего
        } else if (dialect == CoreSQLDialectEnum.POSTGRESQL) {
            // пока вроде нечего
        } else if (dialect == CoreSQLDialectEnum.SQLITE) {
            dbProperty["busy_timeout"] = "5000"
            //--- загоняет открытие соединения в бесконечный цикл с ошибкой
            //--- java.sql.BatchUpdateException: batch entry 0: query returns results.
            //--- решение: поскольку journal_mode задается единожды и далее сохраняется в настройках самой базы,
            //--- то задавать каждый раз эту опцию вовсе не обязательно
            //dbProperty.put( "journal_mode", "WAL" );
            dbProperty["synchronous"] = "FULL"
            dbProperty["transaction_mode"] = "IMMEDIATE"
        }

        conn = DriverManager.getConnection(dbConfig.url, dbProperty)
        conn.autoCommit = false

        val stm = conn.createStatement()

        if (dialect == CoreSQLDialectEnum.H2) {
            // пока вроде нечего
        } else if (dialect == CoreSQLDialectEnum.MSSQL) {
            stm.executeUpdate("SET DATEFORMAT ymd")
        } else if (dialect == CoreSQLDialectEnum.ORACLE) {
            stm.executeUpdate("ALTER SESSION SET nls_date_format='yyyy.mm.dd hh24:mi:ss'")
        } else if (dialect == CoreSQLDialectEnum.POSTGRESQL) {
            // пока вроде нечего
        } else if (dialect == CoreSQLDialectEnum.SQLITE) {
            stm.executeUpdate("PRAGMA journal_mode = WAL")
            stm.executeUpdate("PRAGMA journal_size_limit = -1")
        }
        stm.close()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun executeUpdate(sql: String): Int {
        val stm = conn.createStatement()
        val result = stm.executeUpdate(sql)
        stm.close()

        return result
    }

    override fun executeQuery(sql: String): CoreAdvancedResultSet {
        val stm = conn.createStatement()
        return AdvancedResultSet(dialect, stm, stm.executeQuery(sql))
        //--- will be close by AdvancedResultSet.close()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun commit() {
        super.commit()

        conn.commit()
    }

    override fun rollback() {
        super.rollback()

        conn.rollback()
    }

    override fun close() {
        super.close()

        conn.close()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun checkExisting(aTableName: String, aFieldCheck: String, aValue: Any, aFieldID: String?, id: Number): Boolean {
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
            """
        )
        val isExist = rs.next()
        rs.close()
        stm.close()

        return isExist
    }

    override fun checkExisting(aTableName: String, alFieldCheck: List<Pair<String, Any>>, aFieldID: String?, id: Number): Boolean {
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
            """
        )
        val isExist = rs.next()
        rs.close()
        stm.close()

        return isExist
    }

}
