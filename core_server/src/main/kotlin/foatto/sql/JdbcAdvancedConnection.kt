package foatto.sql

abstract class JdbcAdvancedConnection(dbConfig: DBConfig) : CoreAdvancedConnection(dbConfig) {

    init {
        if (dbConfig.url.startsWith("jdbc:h2")) dialect = SQLDialect.H2
        else if (dbConfig.url.startsWith("jdbc:sqlserver")) dialect = SQLDialect.MSSQL
        else if (dbConfig.url.startsWith("jdbc:oracle")) dialect = SQLDialect.ORACLE
        else if (dbConfig.url.startsWith("jdbc:postgresql")) dialect = SQLDialect.POSTGRESQL
        else if (dbConfig.url.startsWith("jdbc:sqlite")) dialect = SQLDialect.SQLITE
    }
}
