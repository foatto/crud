package foatto.ds

import foatto.core_server.ds.nio.CoreNioServer
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection

class DataWorker(aDataServer: CoreNioServer) : CoreNioWorker(aDataServer) {

    override fun openConnection(): CoreAdvancedConnection {
        return AdvancedConnection(dataServer.dbConfig)
    }
}
