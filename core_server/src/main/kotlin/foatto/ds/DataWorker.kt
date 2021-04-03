package foatto.ds

import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection

class DataWorker constructor(aDataServer: CoreDataServer) : CoreDataWorker(aDataServer) {

    override fun openConnection(dbIndex: Int): CoreAdvancedConnection {
        return AdvancedConnection(dataServer.alDBConfig[dbIndex])
    }
}
