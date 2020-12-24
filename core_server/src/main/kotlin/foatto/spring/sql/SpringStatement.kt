//package foatto.spring.sql
//
//import org.springframework.jdbc.core.JdbcTemplate
//
//class SpringStatement(aConn: SpringConnection, private val jdbcTemplate: JdbcTemplate) : CoreAdvancedStatement(aConn) {
//
//    override fun close() {}
//
//    override fun executeQuery(sql: String) = SpringResultSet(dialect, jdbcTemplate.queryForRowSet(sql))
//
//    override fun executeUpdate(sql: String) = jdbcTemplate.update(sql)
//}
