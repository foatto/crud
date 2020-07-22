//package foatto.office;
//
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.sql.CoreAdvancedStatement;
//
//import java.util.GregorianCalendar;
//
//public class TaskCalc {
//
//    public static int[] getTaskState( CoreAdvancedStatement stm, int outUserID, int inUserID ) throws Throwable {
//        GregorianCalendar toDay = new GregorianCalendar();
//        int countAll = 0;
//        int countRed = 0;
//        //--- загрузим список поручений по данной паре автор/исполнитель
//        CoreAdvancedResultSet rs = stm.executeQuery( new StringBuilder( " SELECT ye , mo , da " ).append( " FROM OFFICE_task " ).append( " WHERE out_user_id = " ).append( outUserID ).append( " AND in_user_id = " ).append( inUserID ).append( " AND in_archive = 0 " ).toString() );
//        while( rs.next() ) {
//            GregorianCalendar gc = new GregorianCalendar( rs.getInt( 1 ), rs.getInt( 2 ) - 1, rs.getInt( 3 ) );
//            if( gc.before( toDay ) ) countRed++;
//            countAll++;
//        }
//        rs.close();
//
//        return new int[] { countAll, countRed };
//    }
//
//}
