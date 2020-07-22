package foatto.sql

import java.sql.Statement

class AdvancedStatement( aConn: AdvancedConnection, val stm: Statement ) : CoreAdvancedStatement( aConn ) {

    override fun close() {
        stm.close()
        //stm = null
    }

    override fun executeQuery(sql: String): CoreAdvancedResultSet {
        return AdvancedResultSet( dialect, stm.executeQuery( sql.toString() ) )
    }

    override fun executeUpdate( sql: String ): Int {
        return stm.executeUpdate( sql )
    }
}
