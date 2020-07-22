//package foatto.office.report;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnDate;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.mAbstractReport;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mP extends mAbstractReport {
//
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
//        columnReportBegDate = new ColumnDate( tableName, "beg_ye", "beg_mo", "beg_da", "Начало периода", 2005, 2100, timeZone );
//        columnReportEndDate = new ColumnDate( tableName, "end_ye", "end_mo", "end_da", "Конец периода", 2005, 2100, timeZone );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        alFormHiddenColumn.add( columnID );
//
//        alFormColumn.add( columnReportBegDate );
//        alFormColumn.add( columnReportEndDate );
//    }
//
//    public ColumnDate getColumnReportBegDate() { return columnReportBegDate; }
//    public ColumnDate getColumnReportEndDate() { return columnReportEndDate; }
//}
