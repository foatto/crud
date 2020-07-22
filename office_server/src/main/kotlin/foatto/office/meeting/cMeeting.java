//package foatto.office.meeting;
//
//import foatto.core_server.app.server.cStandart;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.sql.CoreAdvancedStatement;
//import foatto.core_server.app.server.data.DataAbstractValue;
//import foatto.core_server.app.server.data.DataBoolean;
//import foatto.core_server.app.server.data.iData;
//import foatto.office.mTask;
//import foatto.office.mTaskThread;
//import foatto.core_client.util.StringFunction;
//
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.HashSet;
//
//public class cMeeting extends cStandart {
//
//    private GregorianCalendar toDay = new GregorianCalendar();
//
//    protected StringBuilder addSQLWhere( HashSet<String> hsTableRenameList ) {
//        StringBuilder sb = super.addSQLWhere( hsTableRenameList );
//
//        boolean isActualMeeting = aliasConfig.getAlias().equals( "office_meeting" );
//
//        mMeeting m = (mMeeting) model;
//        sb.append( " AND " ).append( renameTableName( hsTableRenameList, model.columnID.getTableName() ) ).append( '.' )
//          .append( m.getColumnFixResult().getFieldName( 0 ) ).append( " = " ).append( isActualMeeting ? 0 : 1 );
//
//        return sb;
//    }
//
//    protected String postEdit( int id, HashMap<iColumn,iData> hmColumnData, HashMap<String,Object> hmOut ) throws Throwable {
//        String postURL = super.postEdit( id, hmColumnData, hmOut );
//
//        mMeeting m = (mMeeting) model;
//        //--- зафиксировать протокол - тожет быть только один раз, т.к. после фиксации протокола
//        //--- совещание становится архивным и нередактируемым (т.е. флаг фиксации протокола уже не может измениться
//        //--- туда-сюда и т.д.)
//        if( ( (DataBoolean) hmColumnData.get( m.getColumnFixResult() ) ).getValue() ) {
//            CoreAdvancedStatement stmRS = dataWorker.alConn.get( 0 ).createStatement();
//
//            int outUserID = ( (DataAbstractValue) hmColumnData.get( model.columnUser ) ).getValue();
//
//            //--- берём список пунктов протокола
//            StringBuilder sbSQL = new StringBuilder( " SELECT type , task_id , subj , new_msg , new_ye , new_mo , new_da " );
//            for( int i = 0; i < mMeetingResult.SPEAKER_COUNT; i++ ) sbSQL.append( " , " ).append( "speaker_id_" ).append( i );
//            sbSQL.append( " FROM OFFICE_meeting_result " ).append( " WHERE meeting_id = " ).append( id );
//
//            CoreAdvancedResultSet rs = stmRS.executeQuery( sbSQL.toString() );
//            while( rs.next() ) {
//                int type = rs.getInt( 1 );
//                int taskID = rs.getInt( 2 );
//                String subj = rs.getString( 3 );
//                String newMsg = rs.getString( 4 ).trim();
//                int newYe = rs.getInt( 5 );
//                int newMo = rs.getInt( 6 );
//                int newDa = rs.getInt( 7 );
//
//                for( int i = 0; i < mMeetingResult.SPEAKER_COUNT; i++ ) {
//                    int speakerID = rs.getInt( i + 8 );
//                    //--- если ответственный не указан, то нечего делать по этому пункту
//                    if( speakerID == 0 ) continue;
//
//                    //--- создание нового поручения
//                    //--- или создание нового при выборе "изменить поручение" без выбора поручения
//                    //--- или создание нового при выборе "изменить поручение" для всех ответственных, кроме первого
//                    if( type == mMeetingResult.NEW_TASK ||
//                        type == mMeetingResult.CHANGE_TASK && ( taskID == 0 || i != 0 ) ) {
//
//                        taskID = dataWorker.alStm.get( 0 ).getNextID( "OFFICE_task", "id" );
//                        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                            " INSERT INTO OFFICE_task ( " )
//                            .append( " id , out_user_id , in_user_id , in_archive , ye , mo , da , subj , file_id ) VALUES ( " )
//                            .append( taskID ).append( " , " ).append( outUserID ).append( " , " )
//                            .append( speakerID ).append( " , 0 , " )
//                            .append( newYe ).append( " , " ).append( newMo ).append( " , " ).append( newDa ).append( " , '" )
//                            .append( subj ).append( "' , 0 ) " ).toString() );
//                        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " INSERT INTO SYSTEM_alert ( id , alert_time , tag , row_id ) VALUES ( " )
//                            .append( dataWorker.alStm.get( 0 ).getNextID( "SYSTEM_alert", "id" ) )
//                            .append( " , " ).append( System.currentTimeMillis() / 1000 ).append( " , '" )
//                            .append( mTask.ALERT_TAG ).append( "' , " ).append( taskID ).append( " ) " ).toString() );
//                    }
//                    //--- обновление текущего поручения, причем только для самого первого пользователя
//                    else if( type == mMeetingResult.CHANGE_TASK ) {
//                        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                            " UPDATE OFFICE_task SET " )
//                            .append( "   out_user_id = " ).append( outUserID )
//                            .append( " , in_user_id = " ).append( speakerID )
//                            .append( " , in_archive = 0 " ) // поднимаем из архива, если он там был
//                            .append( " , ye = " ).append( newYe )
//                            .append( " , mo = " ).append( newMo )
//                            .append( " , da = " ).append( newDa )
//                            .append( " , subj = '" ).append( subj )
//                            .append( "' WHERE id = " ).append( taskID ).toString() );
//                        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " INSERT INTO SYSTEM_alert ( id , alert_time , tag , row_id ) VALUES ( " )
//                            .append( dataWorker.alStm.get( 0 ).getNextID( "SYSTEM_alert", "id" ) )
//                            .append( " , " ).append( System.currentTimeMillis() / 1000 ).append( " , '" )
//                            .append( mTask.ALERT_TAG ).append( "' , " ).append( taskID ).append( " ) " ).toString() );
//                    }
//                    //--- не создавать поручение
//                    else taskID = 0;
//                    //--- добавляем сообщение, только если указано поручение и сам текст не пустой
//                    if( taskID != 0 && ! newMsg.isEmpty() ) {
//                        int[] arrDT = StringFunction.DateTime_Arr( toDay );
//                        int taskThreadID = dataWorker.alStm.get( 0 ).getNextID( "OFFICE_task_thread", "id" );
//                        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                            " INSERT INTO OFFICE_task_thread ( " )
//                            .append( " id , user_id , task_id , ye , mo , da , ho , mi , message , file_id ) VALUES ( " )
//                            .append( taskThreadID ).append( " , " )
//                            .append( outUserID ).append( " , " ).append( taskID ).append( " , " )
//                            .append( arrDT[ 0 ] ).append( " , " ).append( arrDT[ 1 ] ).append( " , " ).append( arrDT[ 2 ] ).append( " , " )
//                            .append( arrDT[ 3 ] ).append( " , " ).append( arrDT[ 4 ] ).append( " , '" )
//                            .append( newMsg ).append( "' , 0 ) " ).toString() );
//                        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " INSERT INTO SYSTEM_alert ( id , alert_time , tag , row_id ) VALUES ( " )
//                            .append( dataWorker.alStm.get( 0 ).getNextID( "SYSTEM_alert", "id" ) )
//                            .append( " , " ).append( System.currentTimeMillis() / 1000 ).append( " , '" )
//                            .append( mTaskThread.ALERT_TAG ).append( "' , " ).append( taskThreadID ).append( " ) " ).toString() );
//                    }
//                }
//            }
//            rs.close();
//
//            //--- удалить напоминания (именно удалить, а не в архив - т.к. данные напоминания в архиве какой-либо ценности не имеют)
//            dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " DELETE FROM OFFICE_reminder WHERE id IN ( " )
//                .append( " SELECT reminder_id FROM OFFICE_meeting_invite WHERE meeting_id = " ).append( id )
//                .append( " ) " ) );
//            stmRS.close();
//        }
//        //--- обычное сохранение - без фиксации - просто обновим напоминания
//        else {
//            CoreAdvancedStatement stmRS = dataWorker.alConn.get( 0 ).createStatement();
//            CoreAdvancedResultSet rs = stmRS.executeQuery( new StringBuilder( " SELECT reminder_id , invite_id FROM OFFICE_meeting_invite WHERE meeting_id = " ).append( id ).toString() );
//            while( rs.next() ) {
//                int reminderID = rs.getInt( 1 );
//                if( reminderID != 0 ) cMeetingInvite.updateReminder( dataWorker.alStm.get( 0 ), id, rs.getInt( 2 ), reminderID );
//            }
//            rs.close();
//            stmRS.close();
//        }
//
//        return postURL;
//    }
//
//    protected void postDelete( int id, HashMap<iColumn,iData> hmColumnData ) throws Throwable {
//        super.postDelete( id, hmColumnData );
//
//        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//            " DELETE FROM OFFICE_reminder WHERE id IN ( " )
//            .append( " SELECT reminder_id FROM OFFICE_meeting_invite WHERE meeting_id = " ).append( id )
//            .append( " ) " ).toString() );
//    }
//
////    protected TableCellInfo setTableGroupColumnStyle( HashMap<iColumn,iData> hmColumnData, iColumn column, TableCellInfo tci ) throws Exception {
////        super.setTableGroupColumnStyle( hmColumnData, column, tci );
////
////        mReminder mr = (mReminder) model;
////        if( column.equals( mr.getColumnDate() ) ) {
////            GregorianCalendar gc = ( (DataDate) hmColumnData.get( mr.getColumnDate() ) ).getStaticValue();
////            gc.add( GregorianCalendar.DAY_OF_MONTH, 1 );
////            if( gc.before( toDay ) ) {
////                tci.foreColorType = TableCellInfo.FORE_COLOR_TYPE_DEFINED;
////                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL;
////            }
////        }
////        else if( column.equals( mr.getColumnTime() ) ) {
////            DataDate date = (DataDate) hmColumnData.get( mr.getColumnDate() );
////            DataTime time = (DataTime) hmColumnData.get( mr.getColumnTime() );
////            GregorianCalendar gc = new GregorianCalendar( date.getYear(), date.getMonth() - 1, date.getDay(),
////                                                          time.getHour(), time.getMinute(), 0 );
////            if( gc.before( toDay ) ) {
////                tci.foreColorType = TableCellInfo.FORE_COLOR_TYPE_DEFINED;
////                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL;
////            }
////        }
////        return tci;
////    }
//
//}
