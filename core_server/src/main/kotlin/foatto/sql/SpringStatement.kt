package foatto.sql

import org.springframework.jdbc.core.JdbcTemplate

class SpringStatement( aConn: SpringConnection, val jdbcTemplate: JdbcTemplate ) : CoreAdvancedStatement( aConn ) {

    override fun close() {}

    override fun executeQuery(sql: String) = SpringResultSet( dialect, jdbcTemplate.queryForRowSet( sql.toString() ) )

    override fun executeUpdate( sql: String ) = jdbcTemplate.update( sql )
}
