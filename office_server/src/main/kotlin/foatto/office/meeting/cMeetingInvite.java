//package foatto.office.meeting;
//
//import foatto.core_server.app.server.cStandart;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_server.app.server.data.DataBoolean;
//import foatto.core_server.app.server.data.DataInt;
//import foatto.core_server.app.server.data.iData;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.sql.CoreAdvancedStatement;
//import foatto.office.mReminder;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//public class cMeetingInvite extends cStandart {
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
//    //--- записи приглашений, созданные из повестки дня, нельзя редактировать
//    protected boolean isEditEnabled( HashMap<iColumn,iData> hmColumnData, int id ) {
//        mMeetingInvitePresent mvi = (mMeetingInvitePresent) model;
//        boolean isPlan = ( (DataBoolean) hmColumnData.get( mvi.getColumnPlan() ) ).getValue();
//        return ! isPlan && super.isEditEnabled( hmColumnData, id );
//    }
//
//    //--- записи приглашений, созданные из повестки дня, нельзя удалять
//    protected boolean isDeleteEnabled( HashMap<iColumn,iData> hmColumnData, int id ) {
//        mMeetingInvitePresent mvi = (mMeetingInvitePresent) model;
//        boolean isPlan = ( (DataBoolean) hmColumnData.get( mvi.getColumnPlan() ) ).getValue();
//        return ! isPlan && super.isDeleteEnabled( hmColumnData, id );
//    }
//
//    protected void preSave( int id, HashMap<iColumn,iData> hmColumnData ) {
//        super.preSave( id, hmColumnData );
//
//        mMeetingInvitePresent mvi = (mMeetingInvitePresent) model;
//        int meetingID = ( (DataInt) hmColumnData.get( mvi.getColumnMeeting() ) ).getValue();
//        int inviteID = ( (DataInt) hmColumnData.get( mvi.getColumnInvite() ) ).getValue();
//        DataInt dataReminder = (DataInt) hmColumnData.get( mvi.getColumnReminder() );
//        //--- перед добавлением записи создадим напоминание и сохраним его ID
//        if( id == 0 )
//            dataReminder.setValue( createReminder( dataWorker.alStm.get( 0 ), meetingID, inviteID ) );
//        //--- при изменении записи поменяем данные в напоминании (при смене пользователя просто сменим userID в самом напоминании)
//        else
//            updateReminder( dataWorker.alStm.get( 0 ), meetingID, inviteID, dataReminder.getValue() );
//    }
//
//    protected void postDelete( int id, HashMap<iColumn,iData> hmColumnData ) throws Throwable {
//        super.postDelete( id, hmColumnData );
//
//        mMeetingInvitePresent mvi = (mMeetingInvitePresent) model;
//        int reminderID = ( (DataInt) hmColumnData.get( mvi.getColumnReminder() ) ).getValue();
//        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//            " DELETE OFFICE_reminder WHERE id = " ).append( reminderID ) );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public static void refreshPlannedInvites( CoreAdvancedStatement stm, int aMeetingID ) {
//        //--- удалить предыдущие приглашения вместе с напоминаниями
//        conn.executeUpdate( new StringBuilder(
//            " DELETE FROM OFFICE_reminder WHERE id IN ( " )
//            .append( " SELECT reminder_id FROM OFFICE_meeting_invite WHERE is_plan = 1 AND meeting_id = " ).append( aMeetingID )
//            .append( " ) " ) );
//        conn.executeUpdate( new StringBuilder(
//            " DELETE FROM OFFICE_meeting_invite WHERE is_plan = 1 AND meeting_id = " ).append( aMeetingID ).toString() );
//
//        //--- собрать список докладчиков и ответственных
//        ArrayList<Integer> alOrderNo = new ArrayList<>();
//        ArrayList<Integer> alUserID = new ArrayList<>();
//        ArrayList<String> alUserName = new ArrayList<>();
//        CoreAdvancedResultSet rs = conn.executeQuery( new StringBuilder(
//            " SELECT order_no , speaker_id , speaker_name FROM OFFICE_meeting_plan " )
//            .append( " WHERE meeting_id = " ).append( aMeetingID ).toString() );
//        while( rs.next() ) {
//            alOrderNo.add( rs.getInt( 1 ) );
//            alUserID.add( rs.getInt( 2 ) );
//            alUserName.add( rs.getString( 3 ) );
//        }
//        rs.close();
//
//        //--- создаём заново (т.е. полностью обновляем) список приглашённых,
//        //--- в части, зависимой от повестки дня, вместе с соответствующими напоминаниями
//        for( int i = 0; i < alUserID.size(); i++ ) {
//            int userID = alUserID.get( i );
//            //--- для внешних (т.е. не входящих в систему) пользователей напоминания не пишем - некуда же
//            int reminderID = userID == 0 ? 0 : createReminder( stm, aMeetingID, userID );
//            int id = stm.getNextID( "OFFICE_meeting_invite", "id" );
//
//            conn.executeUpdate( new StringBuilder(
//                " INSERT INTO OFFICE_meeting_invite ( " )
//                .append( " id , meeting_id , order_no , invite_id , invite_name , reminder_id , is_plan , is_present ) VALUES ( " )
//                .append( id ).append( " , " ).append( aMeetingID ).append( " , " ).append( alOrderNo.get( i ) ).append( " , " )
//                .append( userID ).append( " , '" ).append( alUserName.get( i ) ).append( "' , " )
//                .append( reminderID ).append( " , 1 , 1 ) " ).toString() );
//        }
//    }
//
//    public static int createReminder( CoreAdvancedStatement stm, int aMeetingID, int aUserID ) {
//        int reminderID = 0;
//
//        Object[] arrMeetingInfo = collectInfoForReminder( stm, aMeetingID );
//
//        if( arrMeetingInfo[ 0 ] != null ) {
//            reminderID = stm.getNextID( "OFFICE_reminder", "id" );
//
//            conn.executeUpdate( new StringBuilder(
//                " INSERT INTO OFFICE_reminder ( id , user_id , in_archive , type , ye , mo , da , ho , mi , people_id , subj , descr ) VALUES ( " )
//                .append( reminderID ).append( " , " ).append( aUserID ).append( " , 0 , " ).append( mReminder.REMINDER_TYPE_MEETING ).append( " , " )
//                .append( arrMeetingInfo[ 2 ] ).append( " , " ).append( arrMeetingInfo[ 3 ] ).append( " , " ).append( arrMeetingInfo[ 4 ] ).append( " , " )
//                .append( arrMeetingInfo[ 5 ] ).append( " , " ).append( arrMeetingInfo[ 6 ] ).append( " , 0 , '" )
//                .append( arrMeetingInfo[ 0 ].toString() ).append( "' , '" ).append( arrMeetingInfo[ 1 ] ).append( "' ) " ) );
//        }
//
//        return reminderID;
//    }
//
//    public static void updateReminder( CoreAdvancedStatement stm, int aMeetingID, int aUserID, int aReminderID ) {
//        Object[] arrMeetingInfo = collectInfoForReminder( stm, aMeetingID );
//
//        if( arrMeetingInfo[ 0 ] != null ) {
//            conn.executeUpdate( new StringBuilder(
//                " UPDATE OFFICE_reminder SET " )
//                .append( "   user_id = " ).append( aUserID )
//                .append( " , ye = " ).append( arrMeetingInfo[ 2 ] )
//                .append( " , mo = " ).append( arrMeetingInfo[ 3 ] )
//                .append( " , da = " ).append( arrMeetingInfo[ 4 ] )
//                .append( " , ho = " ).append( arrMeetingInfo[ 5 ] )
//                .append( " , mi = " ).append( arrMeetingInfo[ 6 ] )
//                .append( " , subj = '" ).append( arrMeetingInfo[ 0 ] )
//                .append( "', descr = '" ).append( arrMeetingInfo[ 1 ] )
//                .append( "' WHERE id = " ).append( aReminderID ) );
//        }
//    }
//
//    private static Object[] collectInfoForReminder( CoreAdvancedStatement stm, int aMeetingID ) {
//        String subj = null;
//        StringBuilder sbDescr = new StringBuilder();
//        int ye = 0, mo = 0, da = 0, ho = 0, mi = 0;
//
//        CoreAdvancedResultSet rs = conn.executeQuery( new StringBuilder(
//            " SELECT subj , ye , mo , da , ho , mi , place FROM OFFICE_meeting WHERE id = " ).append( aMeetingID ) );
//        if( rs.next() ) {
//            subj = rs.getString( 1 );
//            ye = rs.getInt( 2 );
//            mo = rs.getInt( 3 );
//            da = rs.getInt( 4 );
//            ho = rs.getInt( 5 );
//            mi = rs.getInt( 6 );
//            sbDescr.append( "Место проведения: " ).append( rs.getString( 7 ) );
//        }
//        rs.close();
//
//        //--- соберём инфу по повестке дня
//        if( subj != null ) {
//            sbDescr.append( "\nПовестка дня: " );
//            rs = conn.executeQuery( new StringBuilder(
//                " SELECT order_no , subj , speaker_name FROM OFFICE_meeting_plan WHERE meeting_id = " )
//                .append( aMeetingID ).append( " ORDER BY order_no " ).toString() );
//            while( rs.next() ) {
//                sbDescr.append( '\n' ).append( rs.getInt( 1 ) ).append( ". " )
//                       .append( rs.getString( 2 ) )
//                       .append( '\n' ).append( rs.getString( 3 ) );
//            }
//            rs.close();
//        }
//        return new Object[] { subj, sbDescr.toString(), ye, mo, da, ho , mi };
//    }
//}
