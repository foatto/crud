//package foatto.office;
//
//import foatto.core_server.app.server.cStandart;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.util.StringFunction;
//import foatto.core_server.app.server.data.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//public class cClientWork extends cStandart {
//
//    //--- добавление разрешено только если клиент в режиме "в работе"
//    protected boolean isAddEnabled() {
//        return super.isAddEnabled() && hmParentData.get( "office_client_in_work" ) != null;
//    }
//
//    protected void getHeader( String selectorID, boolean withAnchors,
//                              ArrayList<String> alURL, ArrayList<String> alText, HashMap<String,Object> hmOut ) {
//
//        Integer companyID = hmParentData.get( "office_company" );
//
//        Integer clientID = hmParentData.get( "office_client_not_need" );
//        if( clientID == null ) clientID = hmParentData.get( "office_client_in_work" );
//        if( clientID == null ) clientID = hmParentData.get( "office_client_out_work" );
//
//        String clientCaption = "Клиент: ";
//        if( clientID == null ) {
//            clientID = hmParentData.get( "office_people" );
//            clientCaption = "Контакт: ";
//        }
//
//        alURL.add( "" );
//        if( companyID != null ) {
//            CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                " SELECT name FROM OFFICE_company WHERE id = " ).append( companyID ).toString() );
//            alText.add( rs.next() ? new StringBuilder( "Предприятие: " ).append( rs.getString( 1 ) ).toString() : aliasConfig.getDescr() );
//            rs.close();
//        }
//        else if( clientID != null ) {
//            CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                         " SELECT OFFICE_people.name , OFFICE_people.post , OFFICE_company.name " )
//                .append( " FROM OFFICE_people , OFFICE_company " )
//                .append( " WHERE OFFICE_people.company_id = OFFICE_company.id " )
//                .append( " AND OFFICE_people.id = " ).append( clientID ).toString() );
//            alText.add( rs.next() ? new StringBuilder( clientCaption ).append( rs.getString( 1 ) ).append( ", " )
//                    .append( rs.getString( 2 ) ).append( ", " ).append( rs.getString( 3 ) ).toString() : aliasConfig.getDescr() );
//            rs.close();
//        }
//        else alText.add( aliasConfig.getDescr() );
//    }
//
//    protected String postAdd( int id, HashMap<iColumn,iData> hmColumnData, HashMap<String,Object> hmOut ) throws Throwable {
//        String postURL = super.postAdd( id, hmColumnData, hmOut );
//
//        mClientWork mcw = (mClientWork) model;
//
//        DataInt dataUser = ( (DataInt) hmColumnData.get( model.columnUser ) );
//        DataInt dataClient = ( (DataInt) hmColumnData.get( mcw.getColumnClient() ) );
//        DataDate dataAD = ( (DataDate) hmColumnData.get( mcw.getColumnActionDate() ) );
//        DataTime dataAT = ( (DataTime) hmColumnData.get( mcw.getColumnActionTime() ) );
//        DataString dataActionDescr = ( (DataString) hmColumnData.get( mcw.getColumnActionDescr() ) );
//        DataString dataActionResult = ( (DataString) hmColumnData.get( mcw.getColumnActionResult() ) );
//        DataDate dataPD = ( (DataDate) hmColumnData.get( mcw.getColumnPlanDate() ) );
//        DataTime dataPT = ( (DataTime) hmColumnData.get( mcw.getColumnPlanTime() ) );
//        DataString dataPlanAction = ( (DataString) hmColumnData.get( mcw.getColumnPlanAction() ) );
//        DataComboBox dataReminderType = ( (DataComboBox) hmColumnData.get( mcw.getColumnReminderType() ) );
//
//        //--- обновить дату/время последнего/планируемого действия
//        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " UPDATE OFFICE_people SET " )
//                .append( "   action_ye = " ).append( dataAD.getYear() )
//                .append( " , action_mo = " ).append( dataAD.getMonth() )
//                .append( " , action_da = " ).append( dataAD.getDay() )
//                .append( " , action_ho = " ).append( dataAT.getHour() )
//                .append( " , action_mi = " ).append( dataAT.getMinute() )
//                .append( " , plan_ye = " ).append( dataPD.getYear() )
//                .append( " , plan_mo = " ).append( dataPD.getMonth() )
//                .append( " , plan_da = " ).append( dataPD.getDay() )
//                .append( " , plan_ho = " ).append( dataPT.getHour() )
//                .append( " , plan_mi = " ).append( dataPT.getMinute() )
//                .append( " WHERE id = " ).append( dataClient.getValue() ).toString() );
//
//        if( dataReminderType.getValue() != mReminder.REMINDER_TYPE_NO_REMINDER ) {
//            dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " INSERT INTO OFFICE_reminder ( id , user_id , in_archive , type , ye , mo , da , ho , mi , people_id , subj , descr ) VALUES ( " )
//                .append( dataWorker.alStm.get( 0 ).getNextID( "OFFICE_reminder", "id" ) ).append( " , " )
//                .append( dataUser.getValue() ).append( " , 0 , " ).append( dataReminderType.getValue() ).append( " , " )
//                .append( dataPD.getYear() ).append( " , " ).append( dataPD.getMonth() ).append( " , " ).append( dataPD.getDay() ).append( " , " )
//                .append( dataPT.getHour() ).append( " , " ).append( dataPT.getMinute() ).append( " , " )
//                .append( dataClient.getValue() ).append( " , '" )
//                .append( StringFunction.prepareForSQL( dataPlanAction.getText() ) ).append( "' , '" )
//                .append( "---Последнее действие:\n" ).append( StringFunction.prepareForSQL( dataActionDescr.getText() ) )
//                .append( "\n---Результат последнего действия:\n" ).append( StringFunction.prepareForSQL( dataActionResult.getText() ) )
//                .append( "' ) " ).toString() );
//        }
//
//        return postURL;
//    }
//}