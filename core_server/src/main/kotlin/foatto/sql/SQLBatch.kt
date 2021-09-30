package foatto.sql

class SQLBatch {

    private val alSQL = mutableListOf<String>()

    fun add(sql: String) {
        //--- add ';' char to end of sql-expression, if forget it
        val semicolonSuffix = if(sql.trimEnd().endsWith(';')) {
            ""
        } else {
            ";"
        }
        alSQL.add(sql + semicolonSuffix)
    }

    //--- executeBatch отсутствует в Android,
    //--- поэтому реализуем в виде обычного последовательного исполнения
    fun execute(stm: CoreAdvancedStatement): Int {
        var result = 0
        alSQL.forEach { sql ->
            result += stm.executeUpdate(sql)
        }
        return result
    }
}
