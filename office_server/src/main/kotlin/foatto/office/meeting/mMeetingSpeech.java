//package foatto.office.meeting;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mMeetingSpeech extends mAbstract {
//
//    public boolean isUseParentUserID() { return true; }
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
////----------------------------------------------------------------------------------------
//
//        tableName = "OFFICE_meeting_speech";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//
//        ColumnInt columnMeeting = new ColumnInt( tableName, "meeting_id", hmParentData.get( "office_meeting" ) );
//
//        ColumnInt columnOrderNo = new ColumnInt( tableName, "order_no", "Порядковый номер", 10 );
//
//        //--- временные поля для копирования user_id & user_name
//        ColumnInt columnUserID = new ColumnInt( "SYSTEM_users", "id" );
//        ColumnString columnUserName = new ColumnString( "SYSTEM_users", "full_name", "", STRING_COLUMN_WIDTH );
//
//        ColumnInt columnSpeaker = new ColumnInt( tableName, "speaker_id" );
//        ColumnString columnSpeakerName = new ColumnString( tableName, "speaker_name", "Выступавший", STRING_COLUMN_WIDTH );
//            columnSpeakerName.setSelectorAlias( "system_user_people" );
//            columnSpeakerName.addSelectorColumn( columnSpeaker, columnUserID );
//            columnSpeakerName.addSelectorColumn( columnSpeakerName, columnUserName );
//
//        ColumnString columnSubj = new ColumnString( tableName, "subj", "Текст выступления", 12,
//                                STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//
////------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnMeeting );
//        alTableHiddenColumn.add( columnSpeaker );
//
//        addTableColumn( columnOrderNo );
//        addTableColumn( columnSpeakerName );
//        addTableColumn( columnSubj );
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnMeeting );
//        alFormHiddenColumn.add( columnSpeaker );
//
//        alFormColumn.add( columnOrderNo );
//        alFormColumn.add( columnSpeakerName );
//        alFormColumn.add( columnSubj );
//
////---------------------------------------------------------------------
//
//        //--- поля для сортировки
//        alTableSortColumn.add( columnOrderNo );
//            alTableSortDirect.add( "ASC" );
//
////----------------------------------------------------------------------------------------
//
//        //hmParentColumn.put( "system_user", columnSpeaker ); - нет смысла без meeting-parent'a
//        hmParentColumn.put( "office_meeting", columnMeeting );
//        //hmParentColumn.put( "office_task_out", columnTask ); - нет смысла без meeting-parent'a
//
////----------------------------------------------------------------------------------------
//
////        alChildData.add( new ChildData( "office_task_thread" , columnID, true, true ) );
//
////----------------------------------------------------------------------------------------
//
////        alDependData.add( new DependData( "OFFICE_task_thread", "task_id", DependData.DELETE ) );
//
//    }
//
//}
