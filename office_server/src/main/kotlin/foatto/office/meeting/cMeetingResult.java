//package foatto.office.meeting;
//
//import foatto.core_server.app.server.cStandart;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.sql.CoreAdvancedStatement;
//import foatto.core_client.util.AdvancedByteBuffer;
//import foatto.core_client.util.StringFunction;
//
//import java.util.ArrayList;
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//
//public class cMeetingResult extends cStandart {
//
//    public void getTable( AdvancedByteBuffer bbOut, HashMap<String,Object> hmOut ) {
//        Integer meetingID = hmParentData.get( "office_meeting" );
//        int[] arrDT = StringFunction.DateTime_Arr( new GregorianCalendar() );
//
//        CoreAdvancedStatement stmRs = dataWorker.alConn.get( 0 ).createStatement();
//
//        int rowCount = 0;
//        CoreAdvancedResultSet rs = stmRs.executeQuery( new StringBuilder(
//            " SELECT COUNT(*) FROM OFFICE_meeting_result WHERE meeting_id = " ).append( meetingID ).toString() );
//        if( rs.next() ) rowCount = rs.getInt( 1 );
//        rs.close();
//
//        //--- если список пунктов протокола пуст, то заполним его
//        if( rowCount == 0 ) {
//            rs = stmRs.executeQuery( new StringBuilder(
//                " SELECT order_no , type , subj , speaker_name , speaker_id , task_id " )
//                .append( " FROM OFFICE_meeting_plan " )
//                .append( " WHERE meeting_id = " ).append( meetingID ).toString() );
//
//            while( rs.next() ) {
//                int orderNo = rs.getInt( 1 );
//                int type = rs.getInt( 2 );
//                String subj = rs.getString( 3 );
//                String speakerName = rs.getString( 4 );
//                int speakerID = rs.getInt( 5 );
//                int taskID = rs.getInt( 6 );
//
//                int resultID = dataWorker.alStm.get( 0 ).getNextID( "OFFICE_meeting_result", "id" );
//
//                StringBuilder sbSQL = new StringBuilder( " INSERT INTO OFFICE_meeting_result ( " )
//                                                .append( " id , meeting_id , order_no , type , task_id , " )
//                                                .append( " subj , new_msg , new_ye , new_mo , new_da " );
//                for( int i = 0; i < mMeetingResult.SPEAKER_COUNT; i++ )
//                    sbSQL.append( " , " ).append( "speaker_name_" ).append( i ).append( " , " ).append( "speaker_id_" ).append( i );
//                sbSQL.append( " ) VALUES ( " ).append( resultID ).append( " , " ).append( meetingID ).append( " , " )
//                     .append( orderNo ).append( " , " ).append( type ).append( " , " ).append( taskID ).append( " , '" )
//                     .append( subj ).append( "' , '' , " ).append( arrDT[ 0 ] ).append( " , " ).append( arrDT[ 1 ] )
//                     .append( " , " ).append( arrDT[ 2 ] );
//                for( int i = 0; i < mMeetingResult.SPEAKER_COUNT; i++ )
//                    sbSQL.append( " , '" ).append( i == 0 ? speakerName : "" ).append( "' , " ).append( i == 0 ? speakerID : 0 );
//                sbSQL.append( " ) " );
//                dataWorker.alStm.get( 0 ).executeUpdate( sbSQL.toString() );
//            }
//            rs.close();
//        }
//
//        stmRs.close();
//
//        super.getTable( bbOut, hmOut );
//    }
//
//    protected void getHeader( String selectorID, boolean withAnchors,
//                              ArrayList<String> alURL, ArrayList<String> alText, HashMap<String,Object> hmOut ) {
//        Integer meetingID = hmParentData.get( "office_meeting" );
//
//        alURL.add( "" );
//        if( meetingID != null ) {
//            CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder( " SELECT subj FROM OFFICE_meeting WHERE id = " ).append( meetingID ).toString() );
//            alText.add( rs.next() ? new StringBuilder( "Тема совещания: " ).append( rs.getString( 1 ) ).toString() : aliasConfig.getDescr() );
//            rs.close();
//        }
//        else alText.add( aliasConfig.getDescr() );
//    }
//
//}
