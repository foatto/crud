//package foatto.mms.del;
//
//import foatto.core.util.CommonFunction;
//import foatto.core.util.StringFunction;
//import foatto.core_server.service.CoreServiceWorker;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.TimeZone;
//
//public abstract class CoreFileIndexer extends CoreServiceWorker {
//
//    public static final String CONFIG_INDEX_ROOT = "index_root";
//    private static final String CONFIG_INDEX_FILE = "index_file";
//
////---------------------------------------------------------------------------------------------------------------
//
//    private String indexRoot = null;
//    private String indexFileName = null;
//
////---------------------------------------------------------------------------------------------------------------
//
//    private TimeZone timeZone = TimeZone.getDefault();
//
////---------------------------------------------------------------------------------------------------------------
//
//    protected CoreFileIndexer( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        indexRoot = hmConfig.get( CONFIG_INDEX_ROOT );
//        indexFileName = hmConfig.get( CONFIG_INDEX_FILE );
//    }
//
//    protected boolean isRunOnce() { return true; }
//
//    protected void cycle() throws Throwable {
//
//        ArrayList<File> alDir = new ArrayList<>();
//
//        alDir.add( new File( indexRoot ) );
//        for( int i = 0; i < alDir.size(); i++ ) {
//            File dir = alDir.get( i );
//            File indexFile = new File( dir, indexFileName );
//            BufferedWriter bwIndexFile = CommonFunction.getFileWriter( indexFile, "Cp1251", false );
//
//            File[] arrFile = dir.listFiles();
//            for( File file : arrFile ) {
//                String fileName = file.getName();
//                boolean isDirectory = file.isDirectory();
//
//                //--- сам индексный файл не отображаем
//                if( fileName.equals( indexFileName ) ) continue;
//                //--- пропускаем файлы нулевого размера
//                if( file.isFile() && file.length() == 0 ) continue;
//
//                bwIndexFile.write( fileName );
//                bwIndexFile.write( "\t" );
//                bwIndexFile.write( isDirectory ? "-" : Long.toString( file.length() ) );
//                bwIndexFile.write( "\t" );
//                bwIndexFile.write( StringFunction.DateTime_DMYHMS( timeZone, file.lastModified() ).toString() );
//                bwIndexFile.newLine();
//
//                if( isDirectory ) alDir.add( file );
//            }
//            bwIndexFile.close();
//            //--- если индексный файл получился пустым, то удаляем его, чтобы не мешался удалению пустых папок
//            if( indexFile.length() == 0 ) indexFile.delete();
//        }
//    }
//
//}
