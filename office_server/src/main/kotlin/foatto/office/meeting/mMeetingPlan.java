//package foatto.office.meeting;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.app.server.column.ColumnComboBox;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.ds.nio.CoreDataServer;
//import foatto.core_server.ds.nio.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mMeetingPlan extends mAbstract {
//
//    public static final int NEW_POINT = 0;
//    public static final int FROM_TASK = 1;
//
//    private ColumnInt columnSpeaker = null;
//    private ColumnString columnSpeakerName = null;
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
//        tableName = "OFFICE_meeting_plan";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//
//        ColumnInt columnMeeting = new ColumnInt( tableName, "meeting_id", hmParentData.get( "office_meeting" ) );
//
//        ColumnInt columnOrderNo = new ColumnInt( tableName, "order_no", "Порядковый номер", 10 );
//
//        ColumnComboBox columnType = new ColumnComboBox( tableName, "type", "Тип пункта", NEW_POINT );
//            columnType.addChoice( NEW_POINT, "Новый пункт" );
//            columnType.addChoice( FROM_TASK, "По поручению" );
//
//        //--- временные поля для копирования user_id & user_name
//        ColumnInt columnUserID = new ColumnInt( "SYSTEM_users", "id" );
//        ColumnString columnUserName = new ColumnString( "SYSTEM_users", "full_name", "", STRING_COLUMN_WIDTH );
//
//        columnSpeaker = new ColumnInt( tableName, "speaker_id" );
//        columnSpeakerName = new ColumnString( tableName, "speaker_name", "Докладчик", STRING_COLUMN_WIDTH );
//            columnSpeakerName.setSelectorAlias( "system_user_people" );
//            columnSpeakerName.addSelectorColumn( columnSpeaker, columnUserID );
//            columnSpeakerName.addSelectorColumn( columnSpeakerName, columnUserName );
//
//        //--- временные поля для копирования user_id, user_name, task_id
//        ColumnInt columnTaskID = new ColumnInt( "OFFICE_task", "id" );
//        ColumnString columnTaskSubj = new ColumnString( "OFFICE_task", "subj", "", 12, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//        ColumnInt columnTaskUser = new ColumnInt( "OFFICE_task", "in_user_id" );
//
//        ColumnInt columnTask = new ColumnInt( tableName, "task_id" );
//        ColumnString columnSubj = new ColumnString( tableName, "subj", "Тема", 12, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnSubj.setSelectorAlias( "office_task_out" );
//            columnSubj.addSelectorColumn( columnTask, columnTaskID );
//            columnSubj.addSelectorColumn( columnSubj, columnTaskSubj );
//            columnSubj.addSelectorColumn( columnSpeaker, columnTaskUser );
//            columnSubj.addSelectorColumn( columnSpeakerName, columnUserName );
//
////------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnMeeting );
//        alTableHiddenColumn.add( columnTask );
//        alTableHiddenColumn.add( columnSpeaker );
//
//        addTableColumn( columnOrderNo );
//        addTableColumn( columnType );
//        addTableColumn( columnSubj );
//        addTableColumn( columnSpeakerName );
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnMeeting );
//        alFormHiddenColumn.add( columnTask );
//        alFormHiddenColumn.add( columnSpeaker );
//
//        alFormColumn.add( columnOrderNo );
//        alFormColumn.add( columnType );
//        alFormColumn.add( columnSubj );
//        alFormColumn.add( columnSpeakerName );
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
////----------------------------------------------------------------------------------------------------------------------
//
//    public ColumnInt getColumnSpeaker() { return columnSpeaker; }
//    public ColumnString getColumnSpeakerName() { return columnSpeakerName; }
//}
