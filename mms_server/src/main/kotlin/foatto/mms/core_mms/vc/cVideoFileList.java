//package foatto.mms.core_mms.vc;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.AscendingFileNameComparator;
//import foatto.core_server.app.server.cStandart;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_server.app.server.data.DataString;
//import foatto.core_server.app.server.data.iData;
//
//import java.io.File;
//import java.util.HashMap;
//import java.util.PriorityQueue;
//
//public class cVideoFileList extends cStandart {
//
//    //--- список посуточных папок
//    private PriorityQueue<File> pqDateDir = new PriorityQueue<>( 30, new AscendingFileNameComparator() );
//    //--- список файлов в текущей посуточной папке
//    private PriorityQueue<File> pqTimeDir = new PriorityQueue<>( 100, new AscendingFileNameComparator() );
//
//    private String fileVideoDate = null;
//    private String fileVideoTime = null;
//
//    //--- понятно что будет регулироваться правами доступа, но лишняя предосторожность не помешает
//    protected boolean isAddEnabled() { return false; }
//    protected boolean isEditEnabled( HashMap<iColumn,iData> hmColumnData, int id ) throws Throwable { return false; }
//    protected boolean isDeleteEnabled( HashMap<iColumn,iData> hmColumnData, int id ) throws Throwable { return false; }
//
//    public void getTable( AdvancedByteBuffer bbOut, HashMap<String,Object> hmOut ) throws Throwable {
//
//        File dirDate = new File( dataServer.hmConfig.get( aliasConfig.getAlias() ),
//                                 hmParentData.get( "mms_object" ).toString() );
//        if( dirDate.exists() ) {
//            File[] arrDateDir = dirDate.listFiles();
//            for( File file : arrDateDir )
//                if( file.isDirectory() ) pqDateDir.offer( file );
//        }
//
//        super.getTable( bbOut, hmOut );
//    }
//
//    protected boolean isNextDataInTable( CoreAdvancedResultSet rs ) throws Throwable {
//        //--- возможно что нам попадётся пустая суточная папка
//        while( pqTimeDir.isEmpty() ) {
//            File dayDir = pqDateDir.poll();
//            if( dayDir == null ) return false;
//
//            String dateDirName = dayDir.getName();
//            fileVideoDate = dateDirName.replace( '-', '.' );
//
//            File[] arrTimeDir = dayDir.listFiles();
//            for( File file : arrTimeDir )
//                if( file.isFile() ) pqTimeDir.offer( file );
//        }
//        File timeFile = pqTimeDir.poll();
//        String timeFileName = timeFile.getName();
//        //--- заменяем '-' на ':' для показаний времени и убираем расширение файла
//        fileVideoTime = timeFileName.replace( '-', ':' ).substring( 0, timeFileName.length() - 4 );
//
//        return true;
//    }
//
//
//    //--- перекрывается наследниками для генерации данных в момент загрузки записей ДО фильтров поиска и страничной разбивки
//    protected void generateColumnDataBeforeFilter( HashMap<iColumn,iData> hmColumnData ) throws Throwable {
//        mVideoFileList mvfl = (mVideoFileList) model;
//
//        ( (DataString) hmColumnData.get( mvfl.getColumnVFLDir() ) ).setText( fileVideoDate );
//        ( (DataString) hmColumnData.get( mvfl.getColumnVFLFile() ) ).setText( fileVideoTime );
//    }
//}
