//package foatto.mms;
//
//import foatto.mms.del.CoreVideoRack;
//
//public class VideoRack extends CoreVideoRack {
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "VideoRack";
//            if( args.length == 1 ) {
//                new VideoRack( args[ 0 ] ).run();
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
//    public VideoRack( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    protected void initDB() {}
//}
