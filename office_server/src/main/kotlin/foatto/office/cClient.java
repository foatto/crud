//package foatto.office;
//
//import foatto.core_server.app.server.cStandart;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_server.app.server.data.DataDate;
//import foatto.core_server.app.server.data.iData;
//import foatto.core_client.app.table.CoreTableCellInfo;
//
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.HashSet;
//
//public class cClient extends cStandart {
//
//    private GregorianCalendar toDay = new GregorianCalendar();
//
//    protected StringBuilder addSQLWhere( HashSet<String> hsTableRenameList ) {
//        StringBuilder sb = super.addSQLWhere( hsTableRenameList );
//
//        mPeople m = (mPeople) model;
//        sb.append( " AND " ).append( renameTableName( hsTableRenameList, m.getColumnPeopleWorkState().getTableName() ) ).append( '.' )
//          .append( m.getColumnPeopleWorkState().getFieldName( 0 ) ).append( " = " ).append(
//            aliasConfig.getAlias().equals( "office_client_not_need" ) ? mPeople.WORK_STATE_NOT_NEED :
//            aliasConfig.getAlias().equals( "office_client_in_work" )  ? mPeople.WORK_STATE_IN_WORK :
//                                                                        mPeople.WORK_STATE_OUT_WORK );
//        if( aliasConfig.getAlias().equals( "office_client_not_need" ) )
//            //--- здесь используется именно жёсткое использование имени поля user_id,
//            //--- т.к. в режиме "клиент" вместо user_id будет использоваться manager_id
//            sb.append( " AND OFFICE_people.user_id = 0 " );
//
//        return sb;
//    }
//
//    protected CoreTableCellInfo setTableGroupColumnStyle( HashMap<iColumn,iData> hmColumnData, iColumn column, CoreTableCellInfo tci ) {
//        super.setTableGroupColumnStyle( hmColumnData, column, tci );
//
//        if( aliasConfig.getAlias().equals( "office_client_in_work" ) ) {
//            mPeople mp = (mPeople) model;
//            if( column.equals( mp.getColumnClientPlanDate() ) ) {
//                GregorianCalendar gc = ( (DataDate) hmColumnData.get( mp.getColumnClientPlanDate() ) ).getValue();
//                gc.add( GregorianCalendar.DAY_OF_MONTH, 1 );
//                if( gc.before( toDay ) ) {
//                    tci.foreColorType = CoreTableCellInfo.FORE_COLOR_TYPE_DEFINED;
//                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL;
//                }
//            }
//        }
//        return tci;
//    }
//}
