package foatto.sql

import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class AdvancedConnection(dbConfig: DBConfig) : JdbcAdvancedConnection(dbConfig) {

    private val conn: Connection

    init {
        val dbProperty = Properties()
        dbProperty["user"] = dbConfig.login
        dbProperty["password"] = dbConfig.password

        if (dialect == SQLDialect.H2) {
            // пока вроде нечего
        } else if (dialect == SQLDialect.MSSQL) {
            // пока вроде нечего
        } else if (dialect == SQLDialect.ORACLE) {
            // пока вроде нечего
        } else if (dialect == SQLDialect.POSTGRESQL) {
            // пока вроде нечего
        } else if (dialect == SQLDialect.SQLITE) {
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

        if (dialect == SQLDialect.H2) {
            // пока вроде нечего
        } else if (dialect == SQLDialect.MSSQL) {
            stm.executeUpdate("SET DATEFORMAT ymd")
        } else if (dialect == SQLDialect.ORACLE) {
            stm.executeUpdate("ALTER SESSION SET nls_date_format='yyyy.mm.dd hh24:mi:ss'")
        } else if (dialect == SQLDialect.POSTGRESQL) {
            // пока вроде нечего
        } else if (dialect == SQLDialect.SQLITE) {
            stm.executeUpdate("PRAGMA journal_mode = WAL")
            stm.executeUpdate("PRAGMA journal_size_limit = -1")
        }
        stm.close()
    }

    override fun createStatement() = AdvancedStatement(this, conn.createStatement())

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
        //conn = null
    }
}
