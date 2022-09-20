//package foatto.mms.core_mms.vc;
//
//import foatto.core.link.AppAction;
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.ChildData;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.ds.nio.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mVideoFileList extends mAbstract {
//
//    private ColumnString columnVFLDir = null;
//    private ColumnString columnVFLFile = null;
//
//    public void init( CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id ) throws Throwable {
//
//        super.init( dataWorker, aliasConfig, userConfig, hmParentData, id );
//
////        //--- может быть null при вызове из "Модули системы"
////        Integer objectId = hmParentData.get( "mms_object" );
////        if( objectId == null ) objectId = 0;
//
////----------------------------------------------------------------------------------------------------------------------
//
//        tableName = FAKE_TABLE_NAME;
//
////----------------------------------------------------------------------------------------------------------------------
//
////        columnID = new ColumnInt( tableName, "-" );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnVFLDir = new ColumnString( tableName, "_video_file_list_dir", "-", STRING_COLUMN_WIDTH );
//            columnVFLDir.setVirtual( true );
//        columnVFLFile = new ColumnString( tableName, "_video_file_list_file", "-", STRING_COLUMN_WIDTH );
//            columnVFLFile.setVirtual( true );
//
////----------------------------------------------------------------------------------------------------------------------
//
////        alTableHiddenColumn.add( columnID );
//
//        alTableGroupColumn.add( columnVFLDir );
//
//        addTableColumn( columnVFLFile );
//
//        //--- у этой таблицы нет ID-поля, поэтому её записи невозможно показать в виде формы
//
//        alChildData.add( new ChildData( "mms_video_archive", columnID, AppAction.FORM, true, true ) );
//    }
//
//    public ColumnString getColumnVFLDir() { return columnVFLDir; }
//    public ColumnString getColumnVFLFile() { return columnVFLFile; }
//}
//
//
