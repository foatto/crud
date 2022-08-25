package foatto.sql

abstract class JdbcAdvancedConnection(dbConfig: DBConfig) : CoreAdvancedConnection(dbConfig) {

    init {
        dialect = if (dbConfig.url.startsWith("jdbc:h2")) {
            CoreSQLDialectEnum.H2
        } else if (dbConfig.url.startsWith("jdbc:sqlserver")) {
            CoreSQLDialectEnum.MSSQL
        } else if (dbConfig.url.startsWith("jdbc:oracle")) {
            CoreSQLDialectEnum.ORACLE
        } else if (dbConfig.url.startsWith("jdbc:postgresql")) {
            CoreSQLDialectEnum.POSTGRESQL
        } else if (dbConfig.url.startsWith("jdbc:sqlite")) {
            CoreSQLDialectEnum.SQLITE
        } else {
            CoreSQLDialectEnum.POSTGRESQL
        }
    }
}
