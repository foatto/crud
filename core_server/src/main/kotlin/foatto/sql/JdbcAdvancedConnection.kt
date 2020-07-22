package foatto.sql

abstract class JdbcAdvancedConnection( dbConfig: DBConfig ) : CoreAdvancedConnection( dbConfig ) {

//    companion object {
//        protected val hmDriverName = HashMap<SQLDialect, String>()
//        init {
//            hmDriverName.put( SQLDialect.H2, "org.h2.Driver" )
//            hmDriverName.put( SQLDialect.MSSQL, "com.microsoft.sqlserver.jdbc.SQLServerDriver" )
//            hmDriverName.put( SQLDialect.ORACLE, "oracle.jdbc.OracleDriver" )
//            hmDriverName.put( SQLDialect.POSTGRESQL, "org.postgresql.Driver" )
//            hmDriverName.put( SQLDialect.SQLITE, "org.sqlite.JDBC" )
//        }
//    }

    init {
        //--- распознаём диалект по урлу соединения
        if( dbConfig.url.startsWith( "jdbc:h2") ) dialect = SQLDialect.H2
        else if( dbConfig.url.startsWith( "jdbc:sqlserver") ) dialect = SQLDialect.MSSQL
        else if( dbConfig.url.startsWith( "jdbc:oracle") ) dialect = SQLDialect.ORACLE
        else if( dbConfig.url.startsWith( "jdbc:postgresql") ) dialect = SQLDialect.POSTGRESQL
        else if( dbConfig.url.startsWith( "jdbc:sqlite") ) dialect = SQLDialect.SQLITE
    }
}
