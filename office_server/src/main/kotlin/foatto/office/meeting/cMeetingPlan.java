//package foatto.office.meeting;
//
//import foatto.core_server.app.server.cStandart;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_server.app.server.data.DataInt;
//import foatto.core_server.app.server.data.DataString;
//import foatto.core_server.app.server.data.iData;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//public class cMeetingPlan extends cStandart {
//
//    protected void getHeader( String selectorID, boolean withAnchors,
//                              ArrayList<String> alURL, ArrayList<String> alText, HashMap<String,Object> hmOut ) {
//        Integer meetingID = hmParentData.get( "office_meeting" );
//
//        alURL.add( "" );
//        if( meetingID != null ) {
//            CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                " SELECT subj FROM OFFICE_meeting WHERE id = " ).append( meetingID ).toString() );
//            alText.add( rs.next() ? new StringBuilder( "Тема совещания: " ).append( rs.getString( 1 ) ).toString()
//                                  : aliasConfig.getDescr() );
//            rs.close();
//        }
//        else alText.add( aliasConfig.getDescr() );
//    }
//
//    protected String postAdd( int id, HashMap<iColumn,iData> hmColumnData, HashMap<String,Object> hmOut ) throws Throwable {
//        String postURL = super.postAdd( id, hmColumnData, hmOut );
//
//        //--- во избежание будущих нестыковок при фиксации пунктов протокола совещания:
//        //--- если ранее выбранный user_name по speaker_id по справочнику пользователей
//        //--- не совпадает с более поздним вручную введенным speaker_name, то обнуляем speaker_id
//        //--- (т.к. speaker_name более приоритетно)
//        mMeetingPlan m = (mMeetingPlan) model;
//        DataInt dataSpeaker = ( (DataInt) hmColumnData.get( m.getColumnSpeaker() ) );
//        String speakerName = ( ( DataString) hmColumnData.get( m.getColumnSpeakerName() ) ).getText().trim();
//        if( dataSpeaker.getValue() != 0 && ! speakerName.equals( userConfig.getUserFullNames().get( dataSpeaker.getValue() ).trim() ) )
//            dataSpeaker.setValue( 0 );
//
//        Integer meetingID = hmParentData.get( "office_meeting" );
//        if( meetingID != null ) cMeetingInvite.refreshPlannedInvites( dataWorker.alStm.get( 0 ), meetingID );
//
//        return postURL;
//    }
//
//    protected String postEdit( int id, HashMap<iColumn,iData> hmColumnData, HashMap<String,Object> hmOut ) throws Throwable {
//        String postURL = super.postEdit( id, hmColumnData, hmOut );
//
//        Integer meetingID = hmParentData.get( "office_meeting" );
//        if( meetingID != null ) cMeetingInvite.refreshPlannedInvites( dataWorker.alStm.get( 0 ), meetingID );
//
//        return postURL;
//    }
//
//    protected void postDelete( int id, HashMap<iColumn,iData> hmColumnData ) throws Throwable {
//        super.postDelete( id, hmColumnData );
//
//        Integer meetingID = hmParentData.get( "office_meeting" );
//        if( meetingID != null ) cMeetingInvite.refreshPlannedInvites( dataWorker.alStm.get( 0 ), meetingID );
//    }
//}
