//package foatto.mms;
//
//import foatto.mms.del.CoreCameraStatus;
//import foatto.sql.AdvancedConnection;
//
//public class CameraStatus extends CoreCameraStatus {
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "CameraStatus";
//            if( args.length == 1 ) {
//                new CameraStatus( args[ 0 ] ).run();
//                exitCode = 1;
//            }
//            else
//                System.out.println( new StringBuilder( "Usage: " ).append( serviceWorkerName ).append( " <ini-file-name>" ).toString() );
//        }
//        catch( Throwable t ) {
//            t.printStackTrace();
//        }
//        exitProcess( exitCode );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public CameraStatus( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    protected void initDB() {
//        for( int i = 0; i < alDBConfig.size(); i++ ) {
//            alConn.add( new AdvancedConnection( alDBConfig.get( i ) ) );
//            alStm.add( alConn.get( i ).createStatement() );
//        }
//    }
//}