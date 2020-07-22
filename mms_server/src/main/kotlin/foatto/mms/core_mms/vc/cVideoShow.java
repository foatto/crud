//package foatto.mms.core_mms.vc;
//
//import foatto.core.app.iCoreAppContainer;
//import foatto.core.app.video.VideoParameter;
//import foatto.core.link.AppAction;
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.CommonFunction;
//import foatto.core.util.StringFunction;
//import foatto.core_server.app.server.cAbstractForm;
//import foatto.core_server.app.server.data.DataDate;
//import foatto.core_server.app.server.data.DataInt;
//import foatto.core_server.app.server.data.DataRadioButton;
//import foatto.core_server.app.server.data.DataTime;
//import foatto.core_server.app.video.server.VideoStartData;
//import foatto.mms.core_mms.ObjectConfig;
//
//import java.util.Calendar;
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//
//public class cVideoShow extends cAbstractForm {
//
//    protected String getOkButtonIconName() { return iCoreAppContainer.ICON_NAME_VIDEO; }
//
//    protected boolean isFormAutoClick() {
////        if( aliasConfig.getAlias().equals( "mms_video_show" ) ||
////            hmParentData.get( "mms_day_work" ) != null ||
////            hmParentData.get( "mms_work_shift" ) != null ||
////            hmParentData.get( "mms_waybill" ) != null ||
////            hmParentData.get( "mms_shift_work" ) != null )
//
//            return true;
//
////        return super.isFormAutoClick();
//    }
//
//    public String doSave( AdvancedByteBuffer bbIn, HashMap<String,Object> hmOut ) throws Throwable {
//
//        String returnURL = super.doSave( bbIn, hmOut );
//        if( returnURL != null ) return returnURL;
//
//        boolean isOnlineMode = aliasConfig.getAlias().equals( "mms_video_show" );
//
//        mVideoShow msfd = (mVideoShow) model;
//
//        //--- выборка данных параметров для отчета
//        int selectObject = ( (DataInt) hmColumnData.get( msfd.getColumnObject() ) ).getValue();
//
//        VideoStartData vsd = new VideoStartData( selectObject );
//
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT descr , name , login , pwd , url_0 , url_1 , url_image " )
//            .append( " FROM VC_camera " )
//            .append( " WHERE object_id = " ).append( selectObject )
//            .append( " ORDER BY descr " ) );
//        while( rs.next() ) {
//            vsd.alCameraDescr.add( rs.getString( 1 ) );
//            vsd.alCameraName.add( rs.getString( 2 ) );
//            vsd.alCameraLogin.add( rs.getString( 3 ) );
//            vsd.alCameraPassword.add( rs.getString( 4 ) );
//            vsd.alCameraURL0.add( rs.getString( 5 ) );
//            vsd.alCameraURL1.add( rs.getString( 6 ) );
//            vsd.alCameraURLImage.add( rs.getString( 7 ) );
//        }
//        rs.close();
//
//        //--- флаг онлайн-показа
//        if( isOnlineMode ) vsd.rangeType = -1;
//        else {
//            vsd.rangeType = ( (DataRadioButton) hmColumnData.get( msfd.getColumnShowRangeType() ) ).getValue();
//            if( vsd.rangeType == 0 ) {
//                vsd.arrBegDT = new int[] { ( (DataDate) hmColumnData.get( msfd.getColumnShowBegDate() ) ).getYear(),
//                                           ( (DataDate) hmColumnData.get( msfd.getColumnShowBegDate() ) ).getMonth(),
//                                           ( (DataDate) hmColumnData.get( msfd.getColumnShowBegDate() ) ).getDay(),
//                                           ( (DataTime) hmColumnData.get( msfd.getColumnShowBegTime() ) ).getHour(),
//                                           ( (DataTime) hmColumnData.get( msfd.getColumnShowBegTime() ) ).getMinute(),
//                                           ( (DataTime) hmColumnData.get( msfd.getColumnShowBegTime() ) ).getSecond() };
//                vsd.arrEndDT = new int[] { ( (DataDate) hmColumnData.get( msfd.getColumnShowEndDate() ) ).getYear(),
//                                           ( (DataDate) hmColumnData.get( msfd.getColumnShowEndDate() ) ).getMonth(),
//                                           ( (DataDate) hmColumnData.get( msfd.getColumnShowEndDate() ) ).getDay(),
//                                           ( (DataTime) hmColumnData.get( msfd.getColumnShowEndTime() ) ).getHour(),
//                                           ( (DataTime) hmColumnData.get( msfd.getColumnShowEndTime() ) ).getMinute(),
//                                           ( (DataTime) hmColumnData.get( msfd.getColumnShowEndTime() ) ).getSecond() };
//            }
//            //--- обработка динамических диапазонов
//            else {
//                GregorianCalendar begDT = new GregorianCalendar( timeZone );
//                begDT.add( Calendar.SECOND, - vsd.rangeType );
//                GregorianCalendar endDT = new GregorianCalendar( timeZone );
//                vsd.arrBegDT = StringFunction.DateTime_Arr( begDT );
//                vsd.arrEndDT = StringFunction.DateTime_Arr( endDT );
//            }
//        }
//
//        //--- заполнение текста заголовка информацией по объекту
//        ObjectConfig oc = ObjectConfig.getObjectConfig( dataWorker.alStm.get( 0 ), userConfig, selectObject );
//        vsd.sbTitle = new StringBuilder( aliasConfig.getDescr() ).append( ": " );
//        vsd.sbTitle.append( oc.name );
//        if( ! oc.model.isEmpty() ) vsd.sbTitle.append( ", " ).append( oc.model );
//
//        //--- заполнение текста заголовка информацией по периоду времени
//        if( vsd.rangeType > 0 ) {
//            vsd.sbTitle.append( " за последние " );
//            if( vsd.rangeType % 3600 == 0 )
//                vsd.sbTitle.append( vsd.rangeType / 3600 ).append( " час(а,ов) " );
//            else if( vsd.rangeType % 60 == 0 )
//                vsd.sbTitle.append( vsd.rangeType / 60 ).append( " минут " );
//            else
//                vsd.sbTitle.append( vsd.rangeType ).append( " секунд " );
//        }
//
//        int paramID = CommonFunction.getRandomInt();
//        hmOut.put( VideoParameter.VIDEO_START_DATA + paramID, vsd );
//
//        return getParamURL( aliasConfig.getAlias(), AppAction.VIDEO, null, null, null, null,
//                new StringBuilder().append( '&' ).append( VideoParameter.VIDEO_START_DATA ).append( '=' ).append( paramID ).toString() );
//    }
//}
//
