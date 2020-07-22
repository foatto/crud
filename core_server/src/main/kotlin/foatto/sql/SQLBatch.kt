package foatto.sql

class SQLBatch {

    private val alSQL =  mutableListOf<CharSequence>()

    fun add( sql: CharSequence ) { alSQL.add( sql ) }

    //--- executeBatch отсутствует в Android,
    //--- поэтому реализуем в виде обычного последовательного исполнения
    fun execute( stm: CoreAdvancedStatement): Int {
        var result = 0
        for( sql in alSQL ) result += stm.executeUpdate( sql )
        return result
    }
}
