package foatto.sql

abstract class JdbcAdvancedConnection(dbConfig: DBConfig) : CoreAdvancedConnection(dbConfig) {

    init {
        dialect = if (dbConfig.url.startsWith("jdbc:h2")) {
            SQLDialect.H2
        } else if (dbConfig.url.startsWith("jdbc:sqlserver")) {
            SQLDialect.MSSQL
        } else if (dbConfig.url.startsWith("jdbc:oracle")) {
            SQLDialect.ORACLE
        } else if (dbConfig.url.startsWith("jdbc:postgresql")) {
            SQLDialect.POSTGRESQL
        } else if (dbConfig.url.startsWith("jdbc:sqlite")) {
            SQLDialect.SQLITE
        } else {
            SQLDialect.POSTGRESQL
        }
    }
}
