package foatto.server.ds

import foatto.server.sql.AdvancedConnection
import foatto.server.sql.CoreAdvancedConnection

class DataWorker(aDataServer: CoreNioServer) : CoreNioWorker(aDataServer) {

    override fun openConnection(): CoreAdvancedConnection {
        return AdvancedConnection(dataServer.dbConfig)
    }
}
