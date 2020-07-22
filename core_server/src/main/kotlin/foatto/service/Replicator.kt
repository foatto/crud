//package foatto.service
//
//import foatto.core_server.service.CoreReplicator
//import foatto.core_server.service.CoreServiceWorker
//import foatto.sql.AdvancedConnection
//
//class Replicator( aConfigFileName: String ) : CoreReplicator( aConfigFileName ) {
//
//    companion object {
//
//        @JvmStatic
//        fun main( args: Array<String> ) {
//            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
//            try {
//                CoreServiceWorker.serviceWorkerName = "Replicator"
//                if( args.size == 1 ) {
//                    Replicator( args[ 0 ] ).run()
//                    exitCode = 1
//                }
//                else println( "Usage: ${CoreServiceWorker.serviceWorkerName} <ini-file-name>" )
//            }
//            catch( t: Throwable ) {
//                t.printStackTrace()
//            }
//
//            System.exit( exitCode )
//        }
//    }
//
//    override fun initDB() {
//        for( i in alDBConfig.indices ) {
//            alConn.add( AdvancedConnection( alDBConfig[ i ] ) )
//            alStm.add( alConn[ i ].createStatement() )
//        }
//    }
//
//}
