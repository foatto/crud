package foatto.server.sql

import java.sql.Connection

class SpringConnection(aConn: Connection) : CoreAdvancedConnection() {

    init {
        conn = aConn
        //conn.autoCommit = false
    }
}