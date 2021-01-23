//package foatto.mms;
//
//import foatto.mms.del.CoreVideoCopier;
//
//public class VideoCopier extends CoreVideoCopier {
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "VideoCopier";
//            if( args.length == 1 ) {
//                new VideoCopier( args[ 0 ] ).run();
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
//    public VideoCopier( String aConfigFileName ) throws Throwable {
//        super( aConfigFileName );
//    }
//
//    protected void initDB() throws Throwable {}
//}
