@file:JvmName("DataWorker")
package foatto.ds

import foatto.sql.CoreAdvancedConnection
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.sql.AdvancedConnection

class DataWorker constructor( aDataServer: CoreDataServer ) : CoreDataWorker( aDataServer ) {

    override fun openConnection( dbIndex: Int ): CoreAdvancedConnection {
        return AdvancedConnection( dataServer.alDBConfig[ dbIndex ] )
    }
}
