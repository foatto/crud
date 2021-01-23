//package foatto.mms;
//
//import foatto.mms.del.CoreImageDel;
//import foatto.sql.AdvancedConnection;
//
//public class ImageDel extends CoreImageDel {
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "ImageDel";
//            if( args.length == 1 ) {
//                new ImageDel( args[ 0 ] ).run();
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
//    public ImageDel( String aConfigFileName ) {
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