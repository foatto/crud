package foatto.server.sql

import java.sql.DriverManager
import java.util.*

class AdvancedConnection(dbConfig: DBConfig) : CoreAdvancedConnection() {

    init {
        val dbProperty = Properties()
        dbProperty["user"] = dbConfig.login
        dbProperty["password"] = dbConfig.password

        conn = DriverManager.getConnection(dbConfig.url, dbProperty)
        conn.autoCommit = false
    }

}
