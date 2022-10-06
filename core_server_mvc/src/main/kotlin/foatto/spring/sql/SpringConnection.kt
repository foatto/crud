package foatto.spring.sql

import foatto.sql.CoreAdvancedResultSet
import foatto.sql.DBConfig
import foatto.sql.JdbcAdvancedConnection
import org.springframework.jdbc.core.JdbcTemplate

class SpringConnection(private val jdbcTemplate: JdbcTemplate, dbConfig: DBConfig) : JdbcAdvancedConnection(dbConfig) {

    override fun executeUpdate(sql: String): Int = jdbcTemplate.update(sql)

    override fun executeQuery(sql: String): CoreAdvancedResultSet = SpringResultSet(dialect, jdbcTemplate.queryForRowSet(sql))

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
}
