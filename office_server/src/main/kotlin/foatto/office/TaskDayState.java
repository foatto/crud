//package foatto.office;
//
//import foatto.core_server.service.CoreServiceWorker;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_client.util.StringFunction;
//import foatto.sql.AdvancedConnection;
//
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//
//public class TaskDayState extends CoreServiceWorker {
//
//    private static final GregorianCalendar toDay = new GregorianCalendar();
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "TaskDayState";
//            if( args.length == 1 ) {
//                new TaskDayState( args[ 0 ] ).run();
//                exitCode = 1;
//            }
//            else System.out.println( new StringBuilder( "Usage: " ).append( serviceWorkerName ).append( " <ini-file-name>" ).toString() );
//        }
//        catch( Throwable t ) {
//            t.printStackTrace();
//        }
//        System.exit( exitCode );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public TaskDayState( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    protected void initDB() {
//        for( int i = 0; i < alDBConfig.size(); i++ ) {
//            alConn.add( new AdvancedConnection( alDBConfig.get( i ) ) );
//            alStm.add( alConn.get( i ).createStatement() );
//        }
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
////    protected int parseConfig( ArrayList<String> alIni ) throws Throwable {
////        int pos = super.parseConfig( alIni );
////
////        bossUserID = Integer.parseInt( alIni.get( pos++ ) );
////        receptionUserID = Integer.parseInt( alIni.get( pos++ ) );
////        allowedTaskDelay = Integer.parseInt( alIni.get( pos++ ) );
////
////        return pos;
////    }
//
//    protected boolean isRunOnce() { return true; }
//
//    protected void cycle() throws Throwable {
//        //--- загрузка двух отдельных конфигурации пользователей,
//        //--- на всякий случай, чтобы итераторы во вложенных циклах не перемешались
//        HashMap<Integer,UserConfig>  hmUserConfigOut = UserConfig.getConfig( alStm.get( 0 ) );
//        HashMap<Integer,UserConfig>  hmUserConfigIn = UserConfig.getConfig( alStm.get( 0 ) );
//
//        int[] arrToday = StringFunction.DateTime_Arr( toDay );
//
//        for( int outUserID : hmUserConfigOut.keySet() )
//            for( int inUserID : hmUserConfigIn.keySet() ) {
//                int[] arrTaskState = TaskCalc.getTaskState( alStm.get( 0 ), outUserID, inUserID );
//
//                if( arrTaskState[ 0 ] > 0 ) {
//                    //--- запишем (обновим или добавим) статистику за сегодня
//                    String sqlUpdate = new StringBuilder( " UPDATE OFFICE_task_day_state SET " )
//                        .append( "   count_red = " ).append( arrTaskState[ 1 ] )
//                        .append( " , count_all = " ).append( arrTaskState[ 0 ] )
//                        .append( " WHERE out_user_id = " ).append( outUserID )
//                        .append( " AND in_user_id = " ).append( inUserID )
//                        .append( " AND ye = " ).append( arrToday[ 0 ] )
//                        .append( " AND mo = " ).append( arrToday[ 1 ] )
//                        .append( " AND da = " ).append( arrToday[ 2 ] ).toString();
//                    //--- попробуем обновить статистику
//                    if( alStm.get( 0 ).executeUpdate( sqlUpdate ) == 0 )
//                        //--- если не обновилось - добавляем запись
//                        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                            " INSERT INTO OFFICE_task_day_state ( id , out_user_id , in_user_id , ye , mo , da , count_red , count_all ) VALUES ( " )
//                            .append( alStm.get( 0 ).getNextID( "OFFICE_task_day_state", "id" ) ).append( " , " )
//                            .append( outUserID ).append( " , " )
//                            .append( inUserID ).append( " , " )
//                            .append( arrToday[ 0 ] ).append( " , " )
//                            .append( arrToday[ 1 ] ).append( " , " )
//                            .append( arrToday[ 2 ] ).append( " , " )
//                            .append( arrTaskState[ 1 ] ).append( " , " )
//                            .append( arrTaskState[ 0 ] ).append( " ) " ).toString() );
//                }
//            }
//        alConn.get( 0 ).commit();
//    }
//
//}
