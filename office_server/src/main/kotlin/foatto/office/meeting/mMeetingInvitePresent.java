//package foatto.office.meeting;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnBoolean;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mMeetingInvitePresent extends mAbstract {
//
//    private ColumnInt columnMeeting = null;
//    private ColumnInt columnInvite = null;
//    private ColumnInt columnReminder = null;
//    private ColumnBoolean columnPlan = null;
//
//    public boolean isUseParentUserID() { return true; }
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
//        boolean isInvite = aliasConfig.getAlias().equals( "office_meeting_invite" );
//
////----------------------------------------------------------------------------------------
//
//        tableName = "OFFICE_meeting_invite";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//
//        columnMeeting = new ColumnInt( tableName, "meeting_id", hmParentData.get( "office_meeting" ) );
//
//        columnPlan = new ColumnBoolean( tableName, "is_plan", "В повестке дня", false );
//            columnPlan.setEditable( false );
//
//        ColumnInt columnOrderNo = new ColumnInt( tableName, "order_no", "Порядковый номер", 10 );
//            columnOrderNo.setEditable( isInvite );
//
//        //--- временные поля для копирования user_id & user_name
//        ColumnInt columnUserID = new ColumnInt( "SYSTEM_users", "id" );
//        ColumnString columnUserName = new ColumnString( "SYSTEM_users", "full_name", "", STRING_COLUMN_WIDTH );
//
//        columnInvite = new ColumnInt( tableName, "invite_id" );
//        ColumnString columnInviteName = new ColumnString( tableName, "invite_name", "Приглашённый", STRING_COLUMN_WIDTH );
//            columnInviteName.setEditable( isInvite );
//        if( isInvite ) {
//            columnInviteName.setSelectorAlias( "system_user_people" );
//            columnInviteName.addSelectorColumn( columnInvite, columnUserID );
//            columnInviteName.addSelectorColumn( columnInviteName, columnUserName );
//        }
//
//        ColumnBoolean columnPresent = new ColumnBoolean( tableName, "is_present", "Присутствовал(а)", true );
//
//        columnReminder = new ColumnInt( tableName, "reminder_id", 0 );
//
////------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnMeeting );
//        alTableHiddenColumn.add( columnInvite );
//        alTableHiddenColumn.add( columnReminder );
//
//        if( isInvite ) addTableColumn( columnPlan );
//        else alTableHiddenColumn.add( columnPlan );
//        addTableColumn( columnOrderNo );
//        addTableColumn( columnInviteName );
//        if( isInvite ) alTableHiddenColumn.add( columnPresent );
//        else addTableColumn( columnPresent );
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnMeeting );
//        alFormHiddenColumn.add( columnInvite );
//        alFormHiddenColumn.add( columnReminder );
//
//        ( isInvite ? alFormColumn : alFormHiddenColumn ).add( columnPlan );
//        alFormColumn.add( columnOrderNo );
//        alFormColumn.add( columnInviteName );
//        ( isInvite ? alFormHiddenColumn : alFormColumn ).add( columnPresent );
//
////---------------------------------------------------------------------
//
//        //--- поля для сортировки
//        alTableSortColumn.add( columnOrderNo );
//            alTableSortDirect.add( "ASC" );
//
////----------------------------------------------------------------------------------------
//
//        //hmParentColumn.put( "system_user", columnInvite ); - нет смысла без meeting-parent'a
//        hmParentColumn.put( "office_meeting", columnMeeting );
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
//    public ColumnInt getColumnMeeting() { return columnMeeting; }
//    public ColumnInt getColumnInvite() { return columnInvite; }
//    public ColumnInt getColumnReminder() { return columnReminder; }
//    public ColumnBoolean getColumnPlan() { return columnPlan; }
//}
