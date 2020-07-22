//package foatto.office.report;
//
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.util.AdvancedByteBuffer;
//import foatto.core_client.util.StringFunction;
//import foatto.office.mPeople;
//import jxl.CellView;
//import jxl.format.PageOrientation;
//import jxl.format.PaperSize;
//import jxl.write.Label;
//import jxl.write.WritableSheet;
//
//import java.util.ArrayList;
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//
//public class cClientWorkState extends cOfficeReport {
//
//    protected boolean isFormAutoClick() {
//        return true;
//    }
//
//    public String doSave( AdvancedByteBuffer bbIn, HashMap<String,Object> hmOut ) throws Throwable {
//
//        String returnURL = super.doSave( bbIn,  hmOut );
//        if( returnURL != null ) return returnURL;
//
////        mTask m = (mTask) model;
////
////        //--- выборка данных параметров для отчета
////        hmReportParam.put( "report_task", ( (DataInt) hmColumnData.get( m.getColumnReportTask() ) ).getStaticValue() );
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
////        //--- загрузка стартовых параметров
////        int reportTask = (Integer) hmReportParam.get( "report_task" );
//
//        HashMap<Integer,String> hmBusiness = new HashMap<>();
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery(
//            " SELECT id , name FROM OFFICE_business" );
//        while( rs.next() ) hmBusiness.put( rs.getInt( 1 ), rs.getString( 2 ) );
//        rs.close();
//
//        try {defineFormats( 8, 2, 0 );}
//        catch( Throwable aThrowable ) {
//            aThrowable.printStackTrace();
//        }
//
//        int offsY = 0;
//        sheet.addCell( new Label( 1, offsY++, aliasConfig.getDescr(), wcfTitleL ) );
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY, "Исполнитель:", wcfTitleName ) );
//        sheet.addCell( new Label( 2, offsY, userConfig.getUserFullNames().get( userConfig.getUserID() ), wcfTitleValue ) );
//        offsY += 2;
//
//
//        //--- установка размеров заголовков (общая ширина = 140 для А4-ландшафт поля по 10 мм)
//        ArrayList<Integer> alDim = new ArrayList<>();
//        alDim.add(  5 );    // "N п/п"
//        alDim.add( 13 );    // "Направление деятельности"
//        alDim.add( 12 );    // "Ф.И.О."
//        alDim.add( 15 );    // "Должность, Компания, Город"
//        alDim.add(  9 );    // "Дата/время последнего действия"
//        alDim.add( 25 );    // "Последнее действие"
//        alDim.add( 25 );    // "Результат"
//        alDim.add(  9 );    // "Дата/время планируемого действия"
//        alDim.add( 15 );    // "Планирумое действие"
//        alDim.add( 12 );    // "Менеджер"
//
//        for( int i = 0; i < alDim.size(); i++ ) {
//            CellView cvNN = new CellView();
//            cvNN.setSize( alDim.get( i ) * 256 );
//            sheet.setColumnView( i, cvNN );
//        }
//
//        //--- вывод заголовка
//        sheet.addCell( new Label( 0, offsY, "№ п/п", wcfCaptionHC ) );
//        sheet.addCell( new Label( 1, offsY, "Направление деятельности", wcfCaptionHC ) );
//        sheet.addCell( new Label( 2, offsY, "Ф.И.О.", wcfCaptionHC ) );
//        sheet.addCell( new Label( 3, offsY, "Должность,\nКомпания,\nГород", wcfCaptionHC ) );
//        sheet.addCell( new Label( 4, offsY, "Дата / время последнего действия", wcfCaptionHC ) );
//        sheet.addCell( new Label( 5, offsY, "Последнее действие", wcfCaptionHC ) );
//        sheet.addCell( new Label( 6, offsY, "Результат", wcfCaptionHC ) );
//        sheet.addCell( new Label( 7, offsY, "Дата / время планируе-мого действия", wcfCaptionHC ) );
//        sheet.addCell( new Label( 8, offsY, "Планирумое действие", wcfCaptionHC ) );
//        sheet.addCell( new Label( 9, offsY, "Последний исполнитель", wcfCaptionHC ) );
//
//        offsY++;
//
//        rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT OFFICE_people.name , OFFICE_people.business_id , " )
//            .append( " OFFICE_people.post , OFFICE_company.name , OFFICE_city.name , " )
//            .append( " OFFICE_client_work.action_ye , OFFICE_client_work.action_mo , " )
//            .append( " OFFICE_client_work.action_da , OFFICE_client_work.action_ho , OFFICE_client_work.action_mi , " )
//            .append( " OFFICE_client_work.action_descr , OFFICE_client_work.action_result , " )
//            .append( " OFFICE_client_work.plan_ye , OFFICE_client_work.plan_mo , " )
//            .append( " OFFICE_client_work.plan_da , OFFICE_client_work.plan_ho , OFFICE_client_work.plan_mi , " )
//            .append( " OFFICE_client_work.plan_action , OFFICE_client_work.user_id " )
//            .append( " FROM OFFICE_client_work , OFFICE_people , OFFICE_company , OFFICE_city " )
//            .append( " WHERE OFFICE_client_work.client_id = OFFICE_people.id " )
//            .append( " AND OFFICE_people.company_id = OFFICE_company.id " )
//            .append( " AND OFFICE_company.city_id = OFFICE_city.id " )
//            .append( " AND OFFICE_people.manager_id = " ).append( userConfig.getUserID() )
//            .append( " AND OFFICE_people.work_state = " ).append( mPeople.WORK_STATE_IN_WORK )
//            .append( " ORDER BY OFFICE_people.name ASC , " )
//            .append( " OFFICE_client_work.action_ye DESC , OFFICE_client_work.action_mo DESC , " )
//            .append( " OFFICE_client_work.action_da DESC , OFFICE_client_work.action_ho DESC , OFFICE_client_work.action_mi DESC " ) );
//
//        int countNN = 1;
//        String lastClientName = "";
//        while( rs.next() ) {
//            String clientName = rs.getString( 1 );
//            if( clientName.equals( lastClientName ) ) continue;
//            lastClientName = clientName;
//
//            sheet.addCell( new Label( 0, offsY, (countNN++).toString(), wcfNN ) );
//            sheet.addCell( new Label( 1, offsY, hmBusiness.get( rs.getInt( 2 ) ), wcfCellC ) );
//            sheet.addCell( new Label( 2, offsY, clientName, wcfCellL ) );
//            sheet.addCell( new Label( 3, offsY, new StringBuilder( rs.getString( 3 ) ).append( '\n' )
//                .append( rs.getString( 4 ) ).append( '\n' ).append( rs.getString( 5 ) ).toString(), wcfCellL ) );
//            sheet.addCell( new Label( 4, offsY, StringFunction.DateTime_DMYHM(
//                    new int[] { rs.getInt( 6 ), rs.getInt( 7 ), rs.getInt( 8 ), rs.getInt( 9 ), rs.getInt( 10 ), 0 } )
//                , wcfCellC ) );
//            sheet.addCell( new Label( 5, offsY, rs.getString( 11 ), wcfCellL ) );
//            sheet.addCell( new Label( 6, offsY, rs.getString( 12 ), wcfCellL ) );
//            sheet.addCell( new Label( 7, offsY, StringFunction.DateTime_DMYHM(
//                    new int[] { rs.getInt( 13 ), rs.getInt( 14 ), rs.getInt( 15 ), rs.getInt( 16 ), rs.getInt( 17 ), 0 } )
//                , wcfCellC ) );
//            sheet.addCell( new Label( 8, offsY, rs.getString( 18 ), wcfCellL ) );
//            sheet.addCell( new Label( 9, offsY, userConfig.getUserFullNames().get( rs.getInt( 19 ) ), wcfCellC ) );
//
//            offsY++;
//        }
//        rs.close();
//        offsY++;
//
//        sheet.addCell( new Label( 1, offsY, new StringBuilder( "Подготовлено: " )
//                                     .append( StringFunction.DateTime_DMYHMS( new GregorianCalendar() ) ).toString(),
//                                     wcfCellL ) );
//        sheet.mergeCells( 1, offsY, 2, offsY );
//    }
//
//}
