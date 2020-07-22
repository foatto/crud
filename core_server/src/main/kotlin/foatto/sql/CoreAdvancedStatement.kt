package foatto.sql

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getRandomInt

abstract class CoreAdvancedStatement( val conn: CoreAdvancedConnection) {

    //--- для тех случаев, когда лень только из-за этого передавать Connection в параметрах ---
    val dialect: SQLDialect = conn.dialect

//    init {
//        //        String dialect = conn.getDialect();
//        //        if( dialect.equals( SQLDialect.ORACLE ) ) {
//        //            executeUpdate( " ALTER SESSION SET nls_date_format='yyyy.mm.dd hh24:mi:ss' " );
//        //        }
//        //        else if( dialect.equals( SQLDialect.MS_SQL ) ) {
//        //            executeUpdate( " SET DATEFORMAT ymd " );
//        //        }
//        //        else if( dialect.equals( SQLDialect.SQLITE ) ) {
//        //            // пока вроде нечего
//        //        }
//    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    abstract fun close()

    abstract fun executeQuery(sql: String): CoreAdvancedResultSet

    fun executeUpdate( sql: CharSequence, withReplication: Boolean = true ): Int {
        val s = sql.toString()
        val result = executeUpdate( s )
        if( withReplication )
            conn.addReplicationSQL( s )
        return result
    }
    protected abstract fun executeUpdate( sql: String ): Int

    fun getPreLimit( limit: Int ): StringBuilder = conn.getPreLimit( limit )
    fun getMidLimit( limit: Int ): StringBuilder = conn.getMidLimit( limit )
    fun getPostLimit( limit: Int ): StringBuilder = conn.getPostLimit( limit )
    //    public boolean isSlowCount() { return conn.isSlowCount(); }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- вернуть следующее уникальное значение id в таблице
    fun getNextID( aTableName: String, aFieldID: String ): Int {
        return getNextID( arrayOf( aTableName ), arrayOf( aFieldID ) )
    }

    //--- вернуть следующее уникальное значение поля среди нескольких таблиц
    fun getNextID( arrTableName: Array<String>, arrFieldID: Array<String> ): Int {
        var nextID: Int
        OUT@
        while( true ) {
            nextID = getRandomInt()
            if( nextID == 0 ) continue
            for( i in arrTableName.indices ) if( checkExist( arrTableName[ i ], arrFieldID[ i ], nextID, null, 0 ) ) continue@OUT
            return nextID
        }
    }

    fun checkExist( aTableName: String, aFieldCheck: String, aValue: Any, aFieldID: String? = null, id: Int = 0 ): Boolean {
        val stringBound = if( aValue is String ) "'" else ""
        val andFieldIDCheck = if( aFieldID != null ) " AND $aFieldID <> $id " else ""

        val rs = executeQuery( " SELECT $aFieldCheck FROM $aTableName WHERE $aFieldCheck = $stringBound$aValue$stringBound $andFieldIDCheck " )
        val isExist = rs.next()
        rs.close()
        return isExist
    }

    fun checkExist( aTableName: String, arrFieldCheck: Array<Pair<String,Any>>, aFieldID: String? = null, id: Int = 0 ): Boolean {
        var checks = ""
        arrFieldCheck.forEach {
            if(checks.isNotEmpty()) checks += " AND "
            val stringBound = if( it.second is String ) "'" else ""
            checks += "${it.first} = $stringBound${it.second}$stringBound"
        }
        val andFieldIDCheck = if( aFieldID != null ) " AND $aFieldID <> $id " else ""

        val rs = executeQuery( " SELECT ${arrFieldCheck[0].first} FROM $aTableName WHERE $checks $andFieldIDCheck " )
        val isExist = rs.next()
        rs.close()
        return isExist
    }

    // '${bbData.getHex( null, false )}'
    fun getHexValue( bbData: AdvancedByteBuffer ): String {
        val hex = bbData.getHex( null, false )
        return when( dialect ) {
            SQLDialect.H2 -> "X'$hex'"
            SQLDialect.MSSQL -> "0x$hex"
            SQLDialect.POSTGRESQL -> "'\\x$hex'"
            else -> "'$hex'"
        }
    }
}
