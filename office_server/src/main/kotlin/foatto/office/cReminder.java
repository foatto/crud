//package foatto.office;
//
//import foatto.core_server.app.server.cStandart;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_server.app.server.data.DataBoolean;
//import foatto.core_server.app.server.data.DataDate;
//import foatto.core_server.app.server.data.DataTime;
//import foatto.core_server.app.server.data.iData;
//import foatto.core_client.app.table.CoreTableCellInfo;
//
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.HashSet;
//
//public class cReminder extends cStandart {
//
//    private GregorianCalendar toDay = new GregorianCalendar();
//
//    protected StringBuilder addSQLWhere( HashSet<String> hsTableRenameList ) {
//        StringBuilder sb = super.addSQLWhere( hsTableRenameList );
//
//        mReminder m = (mReminder) model;
//        if( m.getType() != -1 )
//            sb.append( " AND " ).append( renameTableName( hsTableRenameList, m.getColumnType().getTableName() ) ).append( '.' )
//              .append( m.getColumnType().getFieldName( 0 ) ).append( " = " ).append( m.getType() );
//
//        return sb;
//    }
//
//    protected CoreTableCellInfo setTableGroupColumnStyle( HashMap<iColumn,iData> hmColumnData, iColumn column, CoreTableCellInfo tci ) {
//        super.setTableGroupColumnStyle( hmColumnData, column, tci );
//
//        mReminder mr = (mReminder) model;
//        if( column.equals( mr.getColumnDate() ) ) {
//            GregorianCalendar gc = ( (DataDate) hmColumnData.get( mr.getColumnDate() ) ).getValue();
//            gc.add( GregorianCalendar.DAY_OF_MONTH, 1 );
//            if( gc.before( toDay ) ) {
//                tci.foreColorType = CoreTableCellInfo.FORE_COLOR_TYPE_DEFINED;
//                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL;
//            }
//        }
//        else if( column.equals( mr.getColumnTime() ) ) {
//            DataDate date = (DataDate) hmColumnData.get( mr.getColumnDate() );
//            DataTime time = (DataTime) hmColumnData.get( mr.getColumnTime() );
//            GregorianCalendar gc = new GregorianCalendar( date.getYear(), date.getMonth() - 1, date.getDay(),
//                                                          time.getHour(), time.getMinute(), 0 );
//            if( gc.before( toDay ) ) {
//                tci.foreColorType = CoreTableCellInfo.FORE_COLOR_TYPE_DEFINED;
//                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL;
//            }
//        }
//        return tci;
//    }
//
//    protected void getTableColumnStyle( int rowNo, boolean isNewRow, HashMap<iColumn,iData> hmColumnData, iColumn column, CoreTableCellInfo tci ) {
//        super.getTableColumnStyle( rowNo, isNewRow, hmColumnData, column, tci );
//
//        mReminder mr = (mReminder) model;
//        if( ( (DataBoolean) hmColumnData.get( mr.getColumnArchive() ) ).getValue() ) {
//            tci.foreColorType = CoreTableCellInfo.FORE_COLOR_TYPE_DEFINED;
//            tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED;
//        }
//    }
//}
