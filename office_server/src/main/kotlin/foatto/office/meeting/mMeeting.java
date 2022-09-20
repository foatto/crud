//package foatto.office.meeting;
//
//import foatto.core_client.link.AppAction;
//import foatto.core_server.app.server.*;
//import foatto.core_server.app.server.column.*;
//import foatto.core_server.ds.nio.CoreDataServer;
//import foatto.core_server.ds.nio.CoreDataWorker;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//
//import java.util.HashMap;
//
//public class mMeeting extends mAbstract {
//
//    private ColumnBoolean columnFixResult = null;
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
//        boolean isActualMeeting = aliasConfig.getAlias().equals( "office_meeting" );
//        boolean isFixableResult = false;
//
//        String place = "";
//        String claimerName = "";
//        int resultNo = 1;
//
//        //--- если это не архивное совещание, то для новой записи определим номер протокола,
//        //--- а для режима редактирования записи определим возможноть фиксации результатов совещания
//        if( isActualMeeting ) {
//            //--- определить самого употребимые варианты места проведения совещания, утверждателя
//            //--- и номер протокола этого совещания
//            if( id == 0 ) {
//                CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery(
//                    " SELECT place , COUNT(*) as aaa FROM OFFICE_meeting GROUP BY place ORDER BY aaa DESC " );
//                if( rs.next() ) place = rs.getString( 1 );
//                rs.close();
//
//                rs = dataWorker.alStm.get( 0 ).executeQuery(
//                    " SELECT claimer_name , COUNT(*) as aaa FROM OFFICE_meeting GROUP BY claimer_name ORDER BY aaa DESC " );
//                if( rs.next() ) claimerName = rs.getString( 1 );
//                rs.close();
//
//                rs = dataWorker.alStm.get( 0 ).executeQuery( " SELECT MAX( result_no ) FROM OFFICE_meeting " );
//                if( rs.next() ) resultNo = rs.getInt( 1 ) + 1;
//                rs.close();
//            }
//            //--- если это не создание записи (т.е. id != 0) и протокол совещания уже заполнен,
//            //--- то разрешаем фиксацию результатов совещания
//            else  {
//                CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                    " SELECT COUNT(*) FROM OFFICE_meeting_result WHERE meeting_id = " ).append( id ).toString() );
//                if( rs.next() ) isFixableResult = rs.getInt( 1 ) > 0;
//                rs.close();
//            }
//        }
//
////----------------------------------------------------------------------------------------
//
//        tableName = "OFFICE_meeting";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//        columnUser = new ColumnInt( tableName, "user_id", userConfig.getUserID() );
//
//        ColumnString columnSubj = new ColumnString( tableName, "subj", "Тема", STRING_COLUMN_WIDTH );
//        ColumnDate columnDate = new ColumnDate( tableName, "ye", "mo", "da", "Дата", 2005, 2030, timeZone );
//        ColumnTime columnTime = new ColumnTime( tableName, "ho", "mi", "Время", timeZone );
//
//        ColumnString columnPlace = new ColumnString( tableName, "place", "Место", STRING_COLUMN_WIDTH );
//            columnPlace.setDefaultValue( place );
//
//        ColumnString columnClaimerName = new ColumnString( tableName, "claimer_name", "Утверждаю", 12,
//                                                           STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnClaimerName.setDefaultValue( claimerName );
//        ColumnInt columnResultNo = new ColumnInt( tableName, "result_no", "Номер протокола", 10, resultNo );
//        ColumnString columnPreparerName = new ColumnString( tableName, "preparer_name", "Подготовил", STRING_COLUMN_WIDTH );
//
//        columnFixResult = new ColumnBoolean( tableName, "fix_result", "Зафиксировать протокол", false );
//            columnFixResult.setEditable( isFixableResult );
//
//        ColumnFile columnFile = new ColumnFile( tableName, "file_id", "Файлы" );
//
////------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnUser );
//
//        alTableGroupColumn.add( columnDate );
//
//        addTableColumn( columnTime );
//        addTableColumn( columnSubj );
//        addTableColumn( columnPlace );
//        addTableColumn( columnResultNo );
//        addTableColumn( columnFile );
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnUser );
//
//        alFormColumn.add( columnSubj );
//        alFormColumn.add( columnDate );
//        alFormColumn.add( columnTime );
//        alFormColumn.add( columnPlace );
//        alFormColumn.add( columnResultNo );
//        alFormColumn.add( columnClaimerName );
//        alFormColumn.add( columnPreparerName );
//        alFormColumn.add( columnFixResult );
//        alFormColumn.add( columnFile );
//
////---------------------------------------------------------------------
//
//        //--- поля для сортировки
//        alTableSortColumn.add( columnDate );
//            alTableSortDirect.add( "ASC" );
//        alTableSortColumn.add( columnTime );
//            alTableSortDirect.add( "ASC" );
//
////----------------------------------------------------------------------------------------
//
//        hmParentColumn.put( "system_user", columnUser );
//
////----------------------------------------------------------------------------------------
//
//        if( isActualMeeting ) {
//            alChildData.add( new ChildData( "office_meeting_plan" , columnID, true ) );
//            alChildData.add( new ChildData( "office_meeting_invite" , columnID ) );
//            alChildData.add( new ChildData( "office_meeting_present" , columnID, true ) );
//            alChildData.add( new ChildData( "office_meeting_speech" , columnID ) );
//            alChildData.add( new ChildData( "office_meeting_result" , columnID ) );
//        }
//        alChildData.add( new ChildData( "office_report_meeting_result" , columnID, AppAction.FORM, true ) );
//
////----------------------------------------------------------------------------------------
//
//        alDependData.add( new DependData( "OFFICE_meeting_plan", "meeting_id", DependData.DELETE ) );
//        alDependData.add( new DependData( "OFFICE_meeting_invite", "meeting_id", DependData.DELETE ) );
//        alDependData.add( new DependData( "OFFICE_meeting_speech", "meeting_id", DependData.DELETE ) );
//        alDependData.add( new DependData( "OFFICE_meeting_result", "meeting_id", DependData.DELETE ) );
//
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public ColumnBoolean getColumnFixResult() { return columnFixResult; }
//}
