//package foatto.mms;
//
//import foatto.mms.del.CoreFileIndexer;
//
//public class FileIndexer extends CoreFileIndexer {
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "FileIndexer";
//            if( args.length == 1 ) {
//                new FileIndexer( args[ 0 ] ).run();
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
//    public FileIndexer( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    protected void initDB() {}
//}