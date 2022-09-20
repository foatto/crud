//package foatto.office.meeting;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.*;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.ds.nio.CoreDataServer;
//import foatto.core_server.ds.nio.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mMeetingResult extends mAbstract {
//
//    public static final int SPEAKER_COUNT = 10;
//
//    public static final int NEW_TASK = 0;
//    public static final int CHANGE_TASK = 1;
//    public static final int NO_TASK = 2;
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
//        tableName = "OFFICE_meeting_result";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//
//        ColumnInt columnMeeting = new ColumnInt( tableName, "meeting_id", hmParentData.get( "office_meeting" ) );
//
//        ColumnInt columnOrderNo = new ColumnInt( tableName, "order_no", "Порядковый номер", 10 );
//
//        ColumnRadioButton columnType = new ColumnRadioButton( tableName, "type", "Действие", NEW_TASK );
//            columnType.addChoice( NEW_TASK, "Создать новое поручение" );
//            columnType.addChoice( CHANGE_TASK, "Изменить существующее поручение" );
//            columnType.addChoice( NO_TASK, "Не создавать поручение" );
//
//        //--- временные поля для копирования user_id & user_name
//        ColumnInt columnUserID = new ColumnInt( "SYSTEM_users", "id" );
//        ColumnString columnUserName = new ColumnString( "SYSTEM_users", "full_name", "", STRING_COLUMN_WIDTH );
//
//        ColumnInt[] arrColumnSpeaker = new ColumnInt[ SPEAKER_COUNT ];
//        ColumnString[] arrColumnSpeakerName = new ColumnString[ SPEAKER_COUNT ];
//        for( int i = 0; i < SPEAKER_COUNT; i++ ) {
//            arrColumnSpeaker[ i ] = new ColumnInt( tableName, new StringBuilder( "speaker_id_" ).append( i ).toString() );
//            arrColumnSpeakerName[ i ] = new ColumnString( tableName, new StringBuilder( "speaker_name_" ).append( i ).toString(),
//                                            new StringBuilder( "Ответственный " ).append( i + 1 ).toString(), STRING_COLUMN_WIDTH );
//                arrColumnSpeakerName[ i ].setSelectorAlias( "system_user_people" );
//                arrColumnSpeakerName[ i ].addSelectorColumn( arrColumnSpeaker[ i ], columnUserID );
//                arrColumnSpeakerName[ i ].addSelectorColumn( arrColumnSpeakerName[ i ], columnUserName );
//        }
//
//        //--- временные поля для копирования user_id, user_name, task_id
//        ColumnInt columnTaskID = new ColumnInt( "OFFICE_task", "id" );
//        ColumnString columnTaskSubj = new ColumnString( "OFFICE_task", "subj", "", 12, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//        ColumnInt columnTaskUser = new ColumnInt( "OFFICE_task", "in_user_id" );
//        //--- в большинстве случаев даже для существующих поручений сроки будут установлены заново, копировать текущие нет смысла
//        //ColumnDate columnTaskDate = new ColumnDate( "OFFICE_task", "ye", "mo", "da", "", 2010, 2100 );
//
//        ColumnInt columnTask = new ColumnInt( tableName, "task_id" );
//        ColumnString columnSubj = new ColumnString( tableName, "subj", "Тема поручения", 12, STRING_COLUMN_WIDTH,
//                                                    false, textFieldMaxSize );
//            columnSubj.setSelectorAlias( "office_task_out" );
//            columnSubj.addSelectorColumn( columnTask, columnTaskID );
//            columnSubj.addSelectorColumn( columnSubj, columnTaskSubj );
//            columnSubj.addSelectorColumn( arrColumnSpeaker[ 0 ], columnTaskUser );
//            columnSubj.addSelectorColumn( arrColumnSpeakerName[ 0 ], columnUserName );
//            //--- в большинстве случаев даже для существующих поручений сроки будут установлены заново, копировать текущие нет смысла
//            //columnNewSubj.addSelectorColumn( columnNewDate, columnTaskDate );
//
//        ColumnString columnNewMessage = new ColumnString( tableName, "new_msg", "Текст пункта протокола",
//                                                          12, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//        ColumnDate columnNewDate = new ColumnDate( tableName, "new_ye", "new_mo", "new_da", "Новый срок поручения", 2010, 2100, timeZone );
//
////------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnMeeting );
//        for( int i = 0; i < SPEAKER_COUNT; i++ ) alTableHiddenColumn.add( arrColumnSpeaker[ i ] );
//        alTableHiddenColumn.add( columnTask );
//
//        addTableColumn( columnOrderNo );
//        addTableColumn( columnType );
//        addTableColumn( columnSubj );
//        addTableColumn( columnNewMessage );
//        addTableColumn( columnNewDate );
//        for( int i = 0; i < SPEAKER_COUNT; i++ ) addTableColumn( arrColumnSpeakerName[ i ] );
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnMeeting );
//        for( int i = 0; i < SPEAKER_COUNT; i++ ) alFormHiddenColumn.add( arrColumnSpeaker[ i ] );
//        alFormHiddenColumn.add( columnTask );
//
//        alFormColumn.add( columnOrderNo );
//        alFormColumn.add( columnType );
//        alFormColumn.add( columnSubj );
//        alFormColumn.add( columnNewMessage );
//        alFormColumn.add( columnNewDate );
//        for( int i = 0; i < SPEAKER_COUNT; i++ ) alFormColumn.add( arrColumnSpeakerName[ i ] );
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
