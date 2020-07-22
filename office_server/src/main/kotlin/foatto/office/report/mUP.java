//package foatto.office.report;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnDate;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.app.server.mAbstractReport;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mUP extends mAbstractReport {
//
//    private ColumnInt columnReportUser = null;
//    private ColumnDate columnReportBegDate = null;
//    private ColumnDate columnReportEndDate = null;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
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
//            ColumnInt columnUserID = new ColumnInt( "SYSTEM_users", "id" );
//        columnReportUser = new ColumnInt( tableName, "user_id", columnUserID, 0 /*userConfig.getUserID()*/ );
//        ColumnString columnUserName = new ColumnString( "SYSTEM_users", "full_name", "По пользователю", STRING_COLUMN_WIDTH );
//            columnUserName.setSelectorAlias( "system_user_people" );
//            columnUserName.addSelectorColumn( columnReportUser, columnUserID );
//            columnUserName.addSelectorColumn( columnUserName );
//
//        columnReportBegDate = new ColumnDate( tableName, "beg_ye", "beg_mo", "beg_da", "Начало периода", 2010, 2100, timeZone );
//        columnReportEndDate = new ColumnDate( tableName, "end_ye", "end_mo", "end_da", "Конец периода", 2010, 2100, timeZone );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnReportUser );
//
//        alFormColumn.add( columnUserName );
//        alFormColumn.add( columnReportBegDate );
//        alFormColumn.add( columnReportEndDate );
//    }
//
//    public ColumnInt getColumnReportUser() { return columnReportUser; }
//    public ColumnDate getColumnReportBegDate() { return columnReportBegDate; }
//    public ColumnDate getColumnReportEndDate() { return columnReportEndDate; }
//}
