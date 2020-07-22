//package foatto.office.report;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnBoolean;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.app.server.mAbstractReport;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mUS extends mAbstractReport {
//
//    private ColumnInt columnReportUser = null;
//    private ColumnBoolean columnSumOnly = null;
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
//        columnSumOnly = new ColumnBoolean( tableName, "sum_only", "Выводить только суммы", false );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnReportUser );
//
//        alFormColumn.add( columnUserName );
//        alFormColumn.add( columnSumOnly );
//    }
//
//    public ColumnInt getColumnReportUser() { return columnReportUser; }
//    public ColumnBoolean getColumnSumOnly() { return columnSumOnly; }
//}
