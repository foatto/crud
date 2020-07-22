//package foatto.office.report;
//
//import foatto.core_server.app.server.data.DataDate;
//import foatto.core_server.app.server.data.DataInt;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.util.AdvancedByteBuffer;
//import foatto.core_client.util.StringFunction;
//import jxl.CellView;
//import jxl.format.PageOrientation;
//import jxl.format.PaperSize;
//import jxl.write.Label;
//import jxl.write.WritableSheet;
//
//import java.util.*;
//
//public class cTaskDayState extends cOfficeReport {
//
////---------------------------------------------------------------------------------------------------
//
//    public String doSave( AdvancedByteBuffer bbIn, HashMap<String,Object> hmOut ) throws Throwable {
//
//        String returnURL = super.doSave( bbIn,  hmOut );
//        if( returnURL != null ) return returnURL;
//
//        mUP m = (mUP) model;
//
//        //--- выборка данных параметров для отчета
//        hmReportParam.put( "report_user", ( (DataInt) hmColumnData.get( m.getColumnReportUser() ) ).getValue() );
//        hmReportParam.put( "report_beg_year", ( (DataDate) hmColumnData.get( m.getColumnReportBegDate() ) ).getYear() );
//        hmReportParam.put( "report_beg_month", ( (DataDate) hmColumnData.get( m.getColumnReportBegDate() ) ).getMonth() );
//        hmReportParam.put( "report_beg_day", ( (DataDate) hmColumnData.get( m.getColumnReportBegDate() ) ).getDay() );
//        hmReportParam.put( "report_end_year", ( (DataDate) hmColumnData.get( m.getColumnReportEndDate() ) ).getYear() );
//        hmReportParam.put( "report_end_month", ( (DataDate) hmColumnData.get( m.getColumnReportEndDate() ) ).getMonth() );
//        hmReportParam.put( "report_end_day", ( (DataDate) hmColumnData.get( m.getColumnReportEndDate() ) ).getDay() );
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
//        printKeyX = 1;
//        printKeyY = 0;
//        printKeyW = 1;
//        printKeyH = 2;
//    }
//
//    protected void postReport( WritableSheet sheet ) throws Throwable {
//
//        //--- загрузка стартовых параметров
//        int reportUser = (Integer) hmReportParam.get( "report_user" );
//        int reportBegYear = (Integer) hmReportParam.get( "report_beg_year" );
//        int reportBegMonth = (Integer) hmReportParam.get( "report_beg_month" );
//        int reportBegDay = (Integer) hmReportParam.get( "report_beg_day" );
//        int reportEndYear = (Integer) hmReportParam.get( "report_end_year" );
//        int reportEndMonth = (Integer) hmReportParam.get( "report_end_month" );
//        int reportEndDay = (Integer) hmReportParam.get( "report_end_day" );
//
//        GregorianCalendar gcBeg = new GregorianCalendar( reportBegYear, reportBegMonth - 1, reportBegDay );
//        GregorianCalendar gcEnd = new GregorianCalendar( reportEndYear, reportEndMonth - 1, reportEndDay );
//
//        try {defineFormats( 8, 2, 0 );}
//        catch( Throwable aThrowable ) {
//            aThrowable.printStackTrace();
//        }
//        int offsY = fillReportTitle( aliasConfig.getDescr(), reportBegYear, reportBegMonth, reportBegDay,
//                                     reportEndYear, reportEndMonth, reportEndDay, sheet, wcfTitleL, 0, 0 );
//        offsY++;
//
//        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
//        ArrayList<Integer> alDim = new ArrayList<>();
//        alDim.add( 80 );    // Дата
//        alDim.add( 5 );     // Просроченных
//        alDim.add( 5 );     // Всего
//
//        for( int i = 0; i < alDim.size(); i++ ) {
//            CellView cvNN = new CellView();
//            cvNN.setSize( alDim.get( i ) * 256 );
//            sheet.setColumnView( i, cvNN );
//        }
//
//        //--- вывод заголовка
//        sheet.addCell( new Label( 0, offsY, "Дата", wcfCaptionHC ) );
//        sheet.addCell( new Label( 1, offsY, "Просроченных", wcfCaptionHC ) );
//        sheet.addCell( new Label( 2, offsY, "Всего", wcfCaptionHC ) );
//
//        offsY++;
//
//        //--- между заголовком и первой строкой вставим пустую строку для красоты
//        offsY++;
//
//        //--- прописывать ограничение по ye/mo/da полям в SQL-запросе очень громоздко,
//        //--- будем брать все, а потом программно отсекать ненужные
//        StringBuilder sb = new StringBuilder(
//             " SELECT SYSTEM_users.full_name , OFFICE_task_day_state.ye , OFFICE_task_day_state.mo , OFFICE_task_day_state.da , " )
//            .append( " OFFICE_task_day_state.count_red , OFFICE_task_day_state.count_all " )
//            .append( " FROM OFFICE_task_day_state , SYSTEM_users " )
//            .append( " WHERE OFFICE_task_day_state.in_user_id = SYSTEM_users.id " )
//            .append( " AND OFFICE_task_day_state.out_user_id = " ).append( userConfig.getUserID() );
//        if( reportUser != 0 ) sb.append( " AND OFFICE_task_day_state.in_user_id = " ).append( reportUser );
//            sb.append( " ORDER BY SYSTEM_users.full_name , OFFICE_task_day_state.ye , OFFICE_task_day_state.mo , OFFICE_task_day_state.da " );
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( sb.toString() );
//        String lastUserName = "";
//        while( rs.next() ) {
//            String userName = rs.getString( 1 );
//            GregorianCalendar gc = new GregorianCalendar( rs.getInt( 2 ), rs.getInt( 3 ) - 1, rs.getInt( 4 ) );
//            int countRed = rs.getInt( 5 );
//            int countAll = rs.getInt( 6 );
//
//            //--- программный пропуск неподходящих дат
//            if( gc.before( gcBeg ) || gc.after( gcEnd ) ) continue;
//            //--- пошли данные по другому пользователю, выводим его имя
//            if( ! userName.equals( lastUserName ) ) {
//                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
//                offsY++;
//                sheet.addCell( new Label( 0, offsY++, userName, wcfCellCBStdYellow ) );
//                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
//                offsY ++;
//                lastUserName = userName;
//            }
//            sheet.addCell( new Label( 0, offsY, StringFunction.DateTime_DMY( gc ), wcfCellC ) );
//            sheet.addCell( new Label( 1, offsY, Integer.toString( countRed ), countRed == 0 ? wcfCellC : wcfCellCRedStd ) );
//            sheet.addCell( new Label( 2, offsY, Integer.toString( countAll ), wcfCellC ) );
//
//            offsY++;
//        }
//        rs.close();
//
//        offsY += 2;
//
//        sheet.addCell( new Label( 0, offsY, new StringBuilder( "Подготовлено: " )
//                                     .append( StringFunction.DateTime_DMYHMS( new GregorianCalendar() ) ).toString(),
//                                     wcfCellL ) );
//        //sheet.mergeCells( 4, offsY, 5, offsY );
//    }
//}
