//package foatto.office.meeting.report;
//
//import foatto.core_server.app.server.data.DataInt;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.office.meeting.mMeetingResult;
//import foatto.office.report.cOfficeReport;
//import foatto.core_client.util.AdvancedByteBuffer;
//import foatto.core_client.util.StringFunction;
//import jxl.CellView;
//import jxl.format.*;
//import jxl.write.Label;
//import jxl.write.WritableSheet;
//
//import java.util.*;
//
//public class cMeetingResultReport extends cOfficeReport {
//
////---------------------------------------------------------------------------------------------------
//
//    public String doSave( AdvancedByteBuffer bbIn, HashMap<String,Object> hmOut ) throws Throwable {
//
//        String returnURL = super.doSave( bbIn, hmOut );
//        if( returnURL != null ) return returnURL;
//
//        mMeetingResultReport mrr = (mMeetingResultReport) model;
//
//        //--- выборка данных параметров для отчета
//        hmReportParam.put( "report_meeting", ( (DataInt) hmColumnData.get( mrr.getColumnMeeting() ) ).getValue() );
//
//        return getReport();
//    }
//
//    protected void setPrintOptions() {
//        printPaperSize = PaperSize.A4;
//        printPageOrientation = PageOrientation.PORTRAIT;
//
//        printMarginLeft = 20;
//        printMarginRight = 10;
//        printMarginTop = 10;
//        printMarginBottom = 10;
//
////        printKeyX = 1;
////        printKeyY = 0;
//        //--- без водяного знака
//        printKeyW = 0;
//        printKeyH = 0;
//    }
//
//    protected void postReport( WritableSheet sheet ) throws Throwable {
//
//        //--- загрузка стартовых параметров
//        int meetingID = (Integer) hmReportParam.get( "report_meeting" );
//
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT claimer_name , result_no , subj , ye , mo , da , place , preparer_name " )
//            .append( " FROM OFFICE_meeting WHERE id = " ).append( meetingID ).toString() );
//        rs.next();
//        String meetingClaimerName = rs.getString( 1 );
//        int meetingResultNo = rs.getInt( 2 );
//        String meetingSubj = rs.getString( 3 );
//        String meetingDate = StringFunction.DateTime_DMY(
//                                new int[] { rs.getInt( 4 ), rs.getInt( 5 ), rs.getInt( 6 ), 0, 0, 0 } );
//        String meetingPlace = rs.getString( 7 );
//        String meetingPreparerName = rs.getString( 8 );
//        rs.close();
//
//        ArrayList<String> alPresent = new ArrayList<>();
//        ArrayList<String> alNotPresent = new ArrayList<>();
//        rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT is_present , invite_name " )
//            .append( " FROM OFFICE_meeting_invite WHERE meeting_id = " ).append( meetingID )
//            .append( " ORDER BY order_no " ).toString() );
//        while( rs.next() ) ( rs.getInt( 1 ) != 0 ? alPresent : alNotPresent ).add( rs.getString( 2 ) );
//        rs.close();
//
//        ArrayList<String> alPlanSubj = new ArrayList<>();
//        ArrayList<String> alPlanSpeaker = new ArrayList<>();
//        rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT subj , speaker_name " )
//            .append( " FROM OFFICE_meeting_plan WHERE meeting_id = " ).append( meetingID )
//            .append( " ORDER BY order_no " ).toString() );
//        while( rs.next() ) {
//            alPlanSubj.add( rs.getString( 1 ) );
//            alPlanSpeaker.add( rs.getString( 2 ) );
//        }
//        rs.close();
//
//        ArrayList<String> alSpeechSubj = new ArrayList<>();
//        ArrayList<String> alSpeechSpeaker = new ArrayList<>();
//        rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT subj , speaker_name " )
//            .append( " FROM OFFICE_meeting_speech WHERE meeting_id = " ).append( meetingID )
//            .append( " ORDER BY order_no " ).toString() );
//        while( rs.next() ) {
//            alSpeechSubj.add( rs.getString( 1 ) );
//            alSpeechSpeaker.add( rs.getString( 2 ) );
//        }
//        rs.close();
//
////  subj              VARCHAR( 8000 ),-- новый пункт
//        ArrayList<String> alResultText = new ArrayList<>();
//        ArrayList<String> alResultUser = new ArrayList<>();
//        ArrayList<String> alResultDate = new ArrayList<>();
//
//        StringBuilder sbSQL = new StringBuilder( " SELECT new_msg , new_ye , new_mo , new_da " );
//        for( int i = 0; i < mMeetingResult.SPEAKER_COUNT; i++ )
//            sbSQL.append( " , " ).append( "speaker_name_" ).append( i );
//        sbSQL.append( " FROM OFFICE_meeting_result WHERE meeting_id = " ).append( meetingID )
//             .append( " ORDER BY order_no " );
//        rs = dataWorker.alStm.get( 0 ).executeQuery( sbSQL.toString() );
//        while( rs.next() ) {
//            alResultText.add( rs.getString( 1 ) );
//            alResultDate.add( StringFunction.DateTime_DMY(
//                                new int[] { rs.getInt( 2 ), rs.getInt( 3 ), rs.getInt( 4 ), 0, 0, 0 } );
//            StringBuilder sbResultUser = new StringBuilder();
//            for( int i = 0; i < mMeetingResult.SPEAKER_COUNT; i++ ) {
//                String un = rs.getString( 5 + i ).trim();
//                if( ! un.isEmpty() ) sbResultUser.append( sbResultUser.length() == 0 ? "" : '\n' ).append( un );
//            }
//            alResultUser.add( sbResultUser.toString() );
//        }
//        rs.close();
//
//        defineFormats( 8, 2, 0 );
//
//        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
//        ArrayList<Integer> alDim = new ArrayList<>();
//        alDim.add(  3 );    // № п/п
//        alDim.add( 57 );    // центральный текст
//        alDim.add( 30 );    // текст с правой стороны (шапка)
//
//        for( int i = 0; i < alDim.size(); i++ ) {
//            CellView cvNN = new CellView();
//            cvNN.setSize( alDim.get( i ) * 256 );
//            sheet.setColumnView( i, cvNN );
//        }
//
//        int offsY = 0;
//
//        //--- блок УТВЕРЖДАЮ
//        sheet.addCell( new Label( 2, offsY++, "УТВЕРЖДАЮ", wcfTextLB ) );
//        //!!! переделать обратно на StringTokenizer
//        String[] arrCap = meetingClaimerName.split( "\n" );
//        for( String capRow : arrCap )
//            sheet.addCell( new Label( 2, offsY++, capRow, wcfTextL ) );
//        sheet.addCell( new Label( 2, offsY++, "'____'__________ 201__ г.", wcfTextL ) );
//
//        offsY++;
//
//        //--- заголовок протокола
//        sheet.addCell( new Label( 1, offsY, new StringBuilder( "Протокол № " ).append( meetingResultNo ).toString(), wcfTextC ) );
//        sheet.mergeCells( 1, offsY, 2, offsY );
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY, "совещания на тему:", wcfTextC ) );
//        sheet.mergeCells( 1, offsY, 2, offsY );
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY, meetingSubj, wcfTextCB ) );
//        sheet.mergeCells( 1, offsY, 2, offsY );
//        offsY++;
//
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY, meetingDate, wcfTextL ) );
//        sheet.addCell( new Label( 2, offsY, meetingPlace, wcfTextR ) );
//        offsY++;
//
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY++, "Присутствовали:", wcfTextLB ) );
//        for( int i = 0; i < alPresent.size(); i++ ) {
//            sheet.addCell( new Label( 0, offsY, new StringBuilder().append( i + 1 ).append( '.' ).toString(), wcfTextR ) );
//            sheet.addCell( new Label( 1, offsY, alPresent.get( i ), wcfTextL ) );
//            sheet.mergeCells( 1, offsY, 2, offsY );
//            offsY++;
//        }
//
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY++, "Отсутствовали:", wcfTextLB ) );
//        for( int i = 0; i < alNotPresent.size(); i++ ) {
//            sheet.addCell( new Label( 0, offsY, new StringBuilder().append( i + 1 ).append( '.' ).toString(), wcfTextR ) );
//            sheet.addCell( new Label( 1, offsY, alNotPresent.get( i ), wcfTextL ) );
//            sheet.mergeCells( 1, offsY, 2, offsY );
//            offsY++;
//        }
//
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY++, "Повестка дня:", wcfTextLB ) );
//        for( int i = 0; i < alPlanSubj.size(); i++ ) {
//            sheet.addCell( new Label( 0, offsY, new StringBuilder().append( i + 1 ).append( '.' ).toString(), wcfTextR ) );
//            sheet.addCell( new Label( 1, offsY, alPlanSubj.get( i ), wcfTextL ) );
//            sheet.mergeCells( 1, offsY, 2, offsY );
//            offsY++;
//            sheet.addCell( new Label( 1, offsY, alPlanSpeaker.get( i ), wcfTextL ) );
//            sheet.mergeCells( 1, offsY, 2, offsY );
//            offsY++;
//        }
//
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY++, "Выступили:", wcfTextLB ) );
//        for( int i = 0; i < alSpeechSubj.size(); i++ ) {
//            sheet.addCell( new Label( 0, offsY, new StringBuilder().append( i + 1 ).append( '.' ).toString(), wcfTextR ) );
//            sheet.addCell( new Label( 1, offsY, alSpeechSpeaker.get( i ), wcfTextL ) );
//            sheet.mergeCells( 1, offsY, 2, offsY );
//            offsY++;
//            sheet.addCell( new Label( 1, offsY, alSpeechSubj.get( i ), wcfTextL ) );
//            sheet.mergeCells( 1, offsY, 2, offsY );
//            offsY++;
//        }
//
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY++, "Принятые решения:", wcfTextLB ) );
//        for( int i = 0; i < alResultText.size(); i++ ) {
//            sheet.addCell( new Label( 0, offsY, new StringBuilder().append( i + 1 ).append( '.' ).toString(), wcfTextR ) );
//            sheet.addCell( new Label( 1, offsY, alResultText.get( i ), wcfCellL ) );
//            //--- если объединять ячейки, то начинает глючить определение высоты строки для многострочного текста
//            //sheet.mergeCells( 1, offsY, 2, offsY );
//            offsY++;
//            sheet.addCell( new Label( 1, offsY, "Ответственные:", wcfTextR ) );
//            sheet.addCell( new Label( 2, offsY, alResultUser.get( i ), wcfTextL ) );
//            offsY++;
//            sheet.addCell( new Label( 1, offsY, "Срок исполнения:", wcfTextR ) );
//            sheet.addCell( new Label( 2, offsY, alResultDate.get( i ), wcfTextL ) );
//            offsY++;
//        }
//
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY, "Подготовил: _________________", wcfTextR ) );
//        sheet.addCell( new Label( 2, offsY, meetingPreparerName, wcfTextL ) );
//
//        offsY += 2;
//
//        sheet.addCell( new Label( 1, offsY, new StringBuilder( "Подготовлено: " )
//                                     .append( StringFunction.DateTime_DMYHMS( new GregorianCalendar() ) ).toString(),
//                                     wcfTextL ) );
//        //sheet.mergeCells( 4, offsY, 5, offsY );
//    }
//
//}
