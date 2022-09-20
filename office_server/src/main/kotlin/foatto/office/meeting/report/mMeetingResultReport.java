//package foatto.office.meeting.report;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnDate;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.ds.nio.CoreDataServer;
//import foatto.core_server.ds.nio.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mMeetingResultReport extends mAbstract {
//
//    private ColumnInt columnMeeting = null;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public String getSaveButonCaption( AliasConfig aAliasConfig ) { return "Показать"; }
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        boolean isActualParentMeeting = hmParentData.containsKey( "office_meeting" );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        tableName = "OFFICE_report";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//
////----------------------------------------------------------------------------------------------------------------------
//
//            ColumnInt columnMeetingID = new ColumnInt( "OFFICE_meeting", "id" );
//        columnMeeting = new ColumnInt( tableName, "meeting_id", columnMeetingID );
//
//        ColumnString columnSubj = new ColumnString( "OFFICE_meeting", "subj", "Тема", STRING_COLUMN_WIDTH );
//        ColumnDate columnDate = new ColumnDate( "OFFICE_meeting", "ye", "mo", "da", "Дата", 2005, 2030, timeZone );
//
//        ColumnString columnClaimerName = new ColumnString( "OFFICE_meeting", "claimer_name", "Утверждаю", 12,
//                                                           STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//        ColumnInt columnResultNo = new ColumnInt( "OFFICE_meeting", "result_no", "Номер протокола", 10 );
//        ColumnString columnPreparerName = new ColumnString( "OFFICE_meeting", "preparer_name", "Подготовил(а)", STRING_COLUMN_WIDTH );
//
//        columnSubj.setSelectorAlias( isActualParentMeeting ? "office_meeting" : "office_meeting_archive" );
//        columnSubj.addSelectorColumn( columnMeeting, columnMeetingID );
//        columnSubj.addSelectorColumn( columnSubj );
//        columnSubj.addSelectorColumn( columnDate );
//        columnSubj.addSelectorColumn( columnClaimerName );
//        columnSubj.addSelectorColumn( columnResultNo );
//        columnSubj.addSelectorColumn( columnPreparerName );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnMeeting );
//
//        alFormColumn.add( columnSubj );
//        alFormColumn.add( columnDate );
//        alFormColumn.add( columnClaimerName );
//        alFormColumn.add( columnResultNo );
//        alFormColumn.add( columnPreparerName );
//
//        hmParentColumn.put( "office_meeting", columnMeeting );
//        hmParentColumn.put( "office_meeting_archive", columnMeeting );
//    }
//
//    public ColumnInt getColumnMeeting() { return columnMeeting; }
//}
