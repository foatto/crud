//package foatto.service;
//
//import foatto.core_server.service.CoreCommander;
//
//public class Commander extends CoreCommander {
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "Commander";
//            if( args.length == 1 ) {
//                new Commander( args[ 0 ] ).run();
//                exitCode = 1;
//            }
//            else System.out.println( new StringBuilder( "Usage: " ).append( serviceWorkerName ).append( " <ini-file-name>" ).toString() );
//        }
//        catch( Throwable t ) {
//            t.printStackTrace();
//        }
//        exitProcess( exitCode );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public Commander( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    protected void initDB() {
////        for( int i = 0; i < alDBURL.size(); i++ ) {
////            alConn.add( new AdvancedConnection(
////                                        alDBURL.get( i ), alDBLogin.get( i ), alDBPassword.get( i ),
////                                        alDBReplicationName.get( i ), alDBReplicationFilter.get( i ), alDBReplicationPath.get( i ) ) );
////            alStm.add( alConn.get( i ).createStatement() );
////        }
//    }
//}
