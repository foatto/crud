//package foatto.office;
//
//import foatto.core_server.app.server.*;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mBusiness extends mAbstract {
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        tableName = "OFFICE_business";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//        ColumnString columnBusinessName = new ColumnString( tableName, "name", "Наименование", STRING_COLUMN_WIDTH );
//            columnBusinessName.setRequired( true );
//            columnBusinessName.setUnique( true, null );
//
//        alTableHiddenColumn.add( columnID );
//
//        addTableColumn( columnBusinessName );
//
//        alFormHiddenColumn.add( columnID );
//
//        alFormColumn.add( columnBusinessName );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        //--- поля для сортировки
//        alTableSortColumn.add( columnBusinessName );
//            alTableSortDirect.add( "ASC" );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        //--- если клиент ещё не в разработке - у него нет "направления деятельности"
//        //alChildData.add( new ChildData( "office_client_not_need" , columnID ) );
//        alChildData.add( new ChildData( "office_client_in_work" , columnID, true, true ) );
//        alChildData.add( new ChildData( "office_client_out_work" , columnID ) );
//
////----------------------------------------------------------------------------------------
//
//        alDependData.add( new DependData( "OFFICE_people", "business_id" ) );
//    }
//}
