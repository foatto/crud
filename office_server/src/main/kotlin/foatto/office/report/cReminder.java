//package foatto.office.report;
//
//import foatto.core_server.app.server.OtherOwnerData;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.sql.CoreAdvancedStatement;
//import foatto.core_server.app.server.data.DataDate;
//import foatto.office.mReminder;
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
//public class cReminder extends cOfficeReport {
//
////---------------------------------------------------------------------------------------------------
//
//    public String doSave( AdvancedByteBuffer bbIn, HashMap<String,Object> hmOut ) throws Throwable {
//
//        String returnURL = super.doSave( bbIn,  hmOut );
//        if( returnURL != null ) return returnURL;
//
//        mP m = (mP) model;
//
//        //--- выборка данных параметров для отчета
////        hmReportParam.put( "report_object_user", ( (DataComboBox) hmColumnData.get( m.getUODG().getColumnObjectUser() ) ).getStaticValue() );
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
//        printPageOrientation = PageOrientation.LANDSCAPE;
//
//        printMarginLeft = 10;
//        printMarginRight = 10;
//        printMarginTop = 20;
//        printMarginBottom = 10;
//
//        printKeyX = 0;
//        printKeyY = 0;
//        printKeyW = 1;
//        printKeyH = 2;
//    }
//
//    protected void postReport( WritableSheet sheet ) throws Throwable {
//
//        ArrayList<ReminderData> alResult = calcReport();
//
//        //--- загрузка стартовых параметров
//        int reportBegYear = (Integer) hmReportParam.get( "report_beg_year" );
//        int reportBegMonth = (Integer) hmReportParam.get( "report_beg_month" );
//        int reportBegDay = (Integer) hmReportParam.get( "report_beg_day" );
//        int reportEndYear = (Integer) hmReportParam.get( "report_end_year" );
//        int reportEndMonth = (Integer) hmReportParam.get( "report_end_month" );
//        int reportEndDay = (Integer) hmReportParam.get( "report_end_day" );
//
////        //--- если отчет получается слишком длинный, то включаем режим вывода только сумм
////        if( tmResult.size() > Short.MAX_VALUE ) reportSumOnly = true;
//
//        try {defineFormats( 8, 2, 0 );}
//        catch( Throwable aThrowable ) {
//            aThrowable.printStackTrace();
//        }
//        int offsY = fillReportTitle( aliasConfig.getDescr(), reportBegYear, reportBegMonth, reportBegDay,
//                                     reportEndYear, reportEndMonth, reportEndDay, sheet, wcfTitleL, 1, 0 );
//        offsY++;
//
////        offsY = fillReportHeader( conn, reportDepartment, reportGroup, sheet, wcfTitleName, wcfTitleValue, 1, offsY );
////        offsY++;
//
//        //--- установка размеров заголовков (общая ширина = 140 для А4-ландшафт поля по 10 мм)
//        ArrayList<Integer> alDim = new ArrayList<>();
//        alDim.add(  5 );    // "N п/п"
//        alDim.add( 16 );    // "Тип/Действие"
//        alDim.add( 14 );    // "Время (без секунд)"
//        alDim.add( 35 );    // "Тема, Описание"
//        alDim.add( 35 );    // "Контакт, Должность"
//        alDim.add( 35 );    // "Компания, Город"
//
//        for( int i = 0; i < alDim.size(); i++ ) {
//            CellView cvNN = new CellView();
//            cvNN.setSize( alDim.get( i ) * 256 );
//            sheet.setColumnView( i, cvNN );
//        }
//
//        //--- вывод заголовка
//        sheet.addCell( new Label( 0, offsY, "№ п/п", wcfCaptionHC ) );
//        sheet.addCell( new Label( 1, offsY, "Действие", wcfCaptionHC ) );
//        sheet.addCell( new Label( 2, offsY, "Время", wcfCaptionHC ) );
//        sheet.addCell( new Label( 3, offsY, "Описание", wcfCaptionHC ) );
//        sheet.addCell( new Label( 4, offsY, "Контактное лицо", wcfCaptionHC ) );
//        sheet.addCell( new Label( 5, offsY, "Предприятие", wcfCaptionHC ) );
//
//        offsY++;
//
////        HashMap<Integer,String> hmUserName = userConfig.getUserFullNames();
//
//        int countNN = 1;
//        for( ReminderData rd : alResult ) {
////            String userName = hmUserName.get( ldd.oc.userID );
//            sheet.addCell( new Label( 0, offsY, (countNN++).toString(), wcfNN ) );
//            //--- вырезаем секунды
//            StringBuilder sbTime = StringFunction.DateTime_DMYHMS( TimeZone.getDefault(), rd.time );
//            sheet.addCell( new Label( 1, offsY, mReminder.hmReminderName.get( rd.type ), wcfCellC ) );
//            sheet.addCell( new Label( 2, offsY, sbTime.substring( 0, sbTime.length() - 3 ), wcfCellC ) );
//            sheet.addCell( new Label( 3, offsY, new StringBuilder( rd.subj ).append( ",\n " ).append( rd.descr ).toString(), wcfCellL ) );
//            sheet.addCell( new Label( 4, offsY, new StringBuilder( rd.peopleName ).append( ",\n " ).append( rd.peoplePost ).toString(), wcfCellL ) );
//            sheet.addCell( new Label( 5, offsY, new StringBuilder( rd.companyName ).append( ",\n " ).append( rd.cityName ).toString(), wcfCellL ) );
//
//            offsY++;
//        }
//        offsY++;
//
//        sheet.addCell( new Label( 5, offsY, new StringBuilder( "Подготовлено: " )
//                                     .append( StringFunction.DateTime_DMYHMS( new GregorianCalendar() ) ).toString(),
//                                     wcfCellL ) );
//        //sheet.mergeCells( 4, offsY, 5, offsY );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private ArrayList<ReminderData> calcReport() throws Throwable {
//
//        ArrayList<ReminderData> alResult = new ArrayList<>();
//
//        //--- загрузка стартовых параметров
//        int reportBegYear = (Integer) hmReportParam.get( "report_beg_year" );
//        int reportBegMonth = (Integer) hmReportParam.get( "report_beg_month" );
//        int reportBegDay = (Integer) hmReportParam.get( "report_beg_day" );
//        int reportEndYear = (Integer) hmReportParam.get( "report_end_year" );
//        int reportEndMonth = (Integer) hmReportParam.get( "report_end_month" );
//        int reportEndDay = (Integer) hmReportParam.get( "report_end_day" );
//
////        int timeOffset = Integer.parseInt( userConfig.getUserProperty( iAppContainer.UP_TIME_OFFSET ) );
//
//        GregorianCalendar gcBeg = new GregorianCalendar( reportBegYear, reportBegMonth - 1, reportBegDay );
//        GregorianCalendar gcEnd = new GregorianCalendar( reportEndYear, reportEndMonth - 1, reportEndDay );
//        gcEnd.add( GregorianCalendar.DAY_OF_MONTH, 1 ); // т.е. конец периода для dd2.mm.yyyy на самом деле == dd2+1.mm.yyyy 00:00
//        long begTime = gcBeg.getTimeInMillis();
//        long endTime = gcEnd.getTimeInMillis();
//
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( " SELECT id FROM SYSTEM_alias WHERE name = 'office_reminder' " );
//        rs.next();
//        int reminderAliasID = rs.getInt( 1 );
//        rs.close();
//
//        HashSet<String> hsObjectPermission = userConfig.getUserPermission().get( "office_reminder" );
//
//        StringBuilder sb = new StringBuilder(
//                     " SELECT OFFICE_reminder.id , OFFICE_reminder.user_id , " )
//            .append( " OFFICE_reminder.ye , OFFICE_reminder.mo , OFFICE_reminder.da , OFFICE_reminder.ho , OFFICE_reminder.mi , " )
//            .append( " OFFICE_reminder.type , OFFICE_reminder.subj , OFFICE_reminder.descr , " )
//            .append( " OFFICE_people.name , OFFICE_people.post , " )
//            .append( " OFFICE_company.name , OFFICE_city.name " )
//            .append( " FROM OFFICE_reminder , OFFICE_people , OFFICE_company , OFFICE_city " )
//            .append( " WHERE OFFICE_reminder.people_id = OFFICE_people.id " )
//            .append( " AND OFFICE_people.company_id = OFFICE_company.id " )
//            .append( " AND OFFICE_company.city_id = OFFICE_city.id " )
//            .append( " AND OFFICE_reminder.id <> 0 AND OFFICE_reminder.in_archive = 0 " )
//            .append( " ORDER BY OFFICE_reminder.type , OFFICE_reminder.ye , OFFICE_reminder.mo , OFFICE_reminder.da , OFFICE_reminder.ho , OFFICE_reminder.mi " );
//        CoreAdvancedStatement stmRS = dataWorker.alConn.get( 0 ).createStatement();
//        rs = stmRS.executeQuery( sb.toString() );
//        while( rs.next() ) {
//            int rID = rs.getInt( 1 );
//            int uID = rs.getInt( 2 );
//            long time = StringFunction.Arr_DateTime( TimeZone.getDefault(), new int[] { rs.getInt( 3 ), rs.getInt( 4 ), rs.getInt( 5 ), rs.getInt( 6 ), rs.getInt( 7 ), 0 } );
//            //--- применяем именно conn-версию getOtherOwner, т.к. текущий Statement занят
//            if( time >= begTime && time <= endTime &&
//                checkPerm( userConfig, hsObjectPermission, PERM_TABLE,
//                           OtherOwnerData.getOtherOwner( dataWorker.alStm.get( 0 ), reminderAliasID, rID, uID, userConfig.getUserID() ) ) )
//                alResult.add( new ReminderData( time, rs.getInt( 8 ), rs.getString( 9 ), rs.getString( 10 ),
//                                                rs.getString( 11 ), rs.getString( 12 ), rs.getString( 13 ), rs.getString( 14 ) ) );
//        }
//        rs.close();
//        stmRS.close();
//
//        return alResult;
//    }
//
//    private static class ReminderData {
//        public long time = 0;
//        //public int userID = 0;
//        public int type = mReminder.REMINDER_TYPE_OTHER;
//        public String subj = null;
//        public String descr = null;
//        public String peopleName = null;
//        public String peoplePost = null;
//        public String companyName = null;
//        public String cityName = null;
//
//        private ReminderData( long aTime, int aType, String aSubj, String aDescr,
//                              String aPeopleName, String aPeoplePost, String aCompanyName, String aCityName ) {
//            time = aTime;
//            type = aType;
//            subj = aSubj;
//            descr = aDescr;
//            peopleName = aPeopleName;
//            peoplePost = aPeoplePost;
//            companyName = aCompanyName;
//            cityName = aCityName;
//        }
//    }
//
//}
