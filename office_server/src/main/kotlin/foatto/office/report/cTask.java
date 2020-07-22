//package foatto.office.report;
//
//import foatto.core_server.app.server.OtherOwnerData;
//import foatto.core_server.app.server.data.DataBoolean;
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
//public class cTask extends cOfficeReport {
//
////---------------------------------------------------------------------------------------------------
//
//    public String doSave( AdvancedByteBuffer bbIn, HashMap<String,Object> hmOut ) throws Throwable {
//
//        String returnURL = super.doSave( bbIn,  hmOut );
//        if( returnURL != null ) return returnURL;
//
//        mUS m = (mUS) model;
//
//        //--- выборка данных параметров для отчета
//        hmReportParam.put( "report_user", ( (DataInt) hmColumnData.get( m.getColumnReportUser() ) ).getValue() );
//        hmReportParam.put( "report_sum_only", ( (DataBoolean) hmColumnData.get( m.getColumnSumOnly() ) ).getValue() );
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
//        printKeyW = 2;
//        printKeyH = 3;
//    }
//
//    protected void postReport( WritableSheet sheet ) throws Throwable {
//
//        TreeMap<String,TaskData> tmResult = calcReport();
//
//        //--- загрузка стартовых параметров
////        int reportUser = (Integer) hmReportParam.get( "report_user" );
//        boolean reportSumOnly = (Boolean) hmReportParam.get( "report_sum_only" );
//
////        //--- если отчет получается слишком длинный, то включаем режим вывода только сумм
////        if( tmResult.size() > Short.MAX_VALUE ) reportSumOnly = true;
//
//        try {defineFormats( 8, 2, 0 );}
//        catch( Throwable aThrowable ) {
//            aThrowable.printStackTrace();
//        }
//
//        int offsY = 1;  // пропустим наверху строчку, чтобы водяной знак (красиво) уместился в нестандартном расположении
//        sheet.addCell( new Label( 0, offsY++, aliasConfig.getDescr(), wcfTitleL ) );
//        offsY++;
//
//        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
//        ArrayList<Integer> alDim = new ArrayList<>();
//        alDim.add( 71 );    // Ф.И.О.
//        alDim.add(  5 );    // Просроченных
//        alDim.add(  5 );    // Всего
//        alDim.add(  9 );    // Срок первого поручения
//
//        for( int i = 0; i < alDim.size(); i++ ) {
//            CellView cvNN = new CellView();
//            cvNN.setSize( alDim.get( i ) * 256 );
//            sheet.setColumnView( i, cvNN );
//        }
//
//        //--- вывод заголовка
//        sheet.addCell( new Label( 0, offsY, "Ф.И.О.", wcfCaptionHC ) );
//        sheet.addCell( new Label( 1, offsY, "Просроченных", wcfCaptionHC ) );
//        sheet.addCell( new Label( 2, offsY, "Всего", wcfCaptionHC ) );
//        sheet.addCell( new Label( 3, offsY, "Срок первого поручения", wcfCaptionHC ) );
//
//        offsY++;
//
//        //--- в подробном режиме вывода между заголовком и первой строкой вставим пустую строку для красоты
//        if( ! reportSumOnly ) offsY++;
//
//        int sumRed = 0;
//        int sumAll = 0;
//        for( String userName : tmResult.keySet() ) {
//            TaskData td = tmResult.get( userName );
//
//            sheet.addCell( new Label( 0, offsY, td.userName, wcfCellLB ) );
//            sheet.addCell( new Label( 1, offsY, Integer.toString( td.countRed ), td.countRed == 0 ? wcfCellCB : wcfCellCBRedStd ) );
//            sheet.addCell( new Label( 2, offsY, Integer.toString( td.countAll ), wcfCellCB ) );
//            sheet.addCell( new Label( 3, offsY, td.firstDate, td.countRed == 0 ? wcfCellCB : wcfCellCBRedStd ) );
//
//            offsY++;
//
//            if( ! reportSumOnly ) {
//                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
//                offsY++;
//
//                for( String taskStr : td.alTask ) {
//                    sheet.addCell( new Label( 0, offsY,
//                                              taskStr.substring( 0, Math.min( 100, taskStr.length() ) ).replace( '\n', ' ' ),
//                                              wcfCellL ) );
//                    sheet.mergeCells( 0, offsY, 3, offsY );
//                    offsY++;
//                }
//                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
//                offsY ++;
//            }
//
//            sumRed += td.countRed;
//            sumAll += td.countAll;
//        }
//        //--- добавляем пустую строку только в режиме вывода только сумм, иначе лишняя строка получается
//        if( reportSumOnly ) offsY++;
//
//        sheet.addCell( new Label( 0, offsY, "ИТОГО:", wcfCellCB ) );
//        sheet.addCell( new Label( 1, offsY, Integer.toString( sumRed ), wcfCellCBRedStd ) );
//        sheet.addCell( new Label( 2, offsY, Integer.toString( sumAll ), wcfCellCB ) );
//
//        offsY += 2;
//
//        sheet.addCell( new Label( 0, offsY, new StringBuilder( "Подготовлено: " )
//                                     .append( StringFunction.DateTime_DMYHMS( new GregorianCalendar() ) ).toString(),
//                                     wcfCellL ) );
//        //sheet.mergeCells( 4, offsY, 5, offsY );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private TreeMap<String,TaskData> calcReport() throws Throwable {
//
//        TreeMap<String,TaskData> tmResult = new TreeMap<>();
//
//        //--- загрузка стартовых параметров
//        int reportUser = (Integer) hmReportParam.get( "report_user" );
//        boolean reportSumOnly = (Boolean) hmReportParam.get( "report_sum_only" );
//
////        int timeOffset = Integer.parseInt( userConfig.getUserProperty( iAppContainer.UP_TIME_OFFSET ) );
//
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( " SELECT id FROM SYSTEM_alias WHERE name = 'office_task_out' " );
//        rs.next();
//        int taskOutAliasID = rs.getInt( 1 );
//        rs.close();
//
//        HashSet<String> hsObjectPermission = userConfig.getUserPermission().get( "office_task_out" );
//        HashMap<Integer,String> hmUserName = userConfig.getUserFullNames();
//        GregorianCalendar toDay = new GregorianCalendar();
//
//        StringBuilder sb = new StringBuilder(
//                     " SELECT id , out_user_id , in_user_id , ye , mo , da , subj " )
//            .append( " FROM OFFICE_task " )
//            .append( " WHERE id <> 0 AND in_archive = 0 " );
//        if( reportUser == 0 )
//            sb.append( " AND in_user_id <> 0 " );
//        else
//            sb.append( " AND in_user_id = " ).append( reportUser );
//            sb .append( " ORDER BY ye , mo , da " );
//        rs = dataWorker.alStm.get( 0 ).executeQuery( sb.toString() );
//        while( rs.next() ) {
//            int rID = rs.getInt( 1 );
//            int uID = rs.getInt( 2 );
//
//            if( ! checkPerm( userConfig, hsObjectPermission, PERM_TABLE,
//                             OtherOwnerData.getOtherOwner( dataWorker.alStm.get( 0 ), taskOutAliasID, rID, uID, userConfig.getUserID() ) ) )
//                continue;
//
//            String userName = hmUserName.get( rs.getInt( 3 ) );
//            int ye = rs.getInt( 4 );
//            int mo = rs.getInt( 5 );
//            int da = rs.getInt( 6 );
//            String subj = rs.getString( 7 );
//
//            GregorianCalendar gc = new GregorianCalendar( ye, mo - 1, da );
//            boolean isRed = gc.before( toDay );
//
//            TaskData td = tmResult.get( userName );
//            if( td == null ) {
//                td = new TaskData( userName, 0, 0, StringFunction.DateTime_DMY( gc ) );
//                tmResult.put( userName, td );
//            }
//            if( isRed ) td.countRed++;
//            td.countAll++;
//            if( ! reportSumOnly )
//                td.alTask.add( new StringBuilder( StringFunction.DateTime_DMY( gc ) )
//                    .append( " - " ).append( subj ).toString() );
//        }
//        rs.close();
//
//        return tmResult;
//    }
//
//    private static class TaskData {
//        public String userName = null;
//        public int countRed = 0;
//        public int countAll = 0;
//        public String firstDate = null;
//
//        public ArrayList<String> alTask = new ArrayList<>();
//
//        private TaskData( String aUserName, int aCountRed, int aCountAll, String aFirstDate ) {
//            userName = aUserName;
//            countRed = aCountRed;
//            countAll = aCountAll;
//            firstDate = aFirstDate;
//        }
//    }
//
//}
