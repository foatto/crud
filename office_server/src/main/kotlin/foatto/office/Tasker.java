//package foatto.office;
//
//import foatto.core_server.service.CoreServiceWorker;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.sql.AdvancedConnection;
//
//import java.util.*;
//
//public class Tasker extends CoreServiceWorker {
//
//    private static final String CONFIG_BOSS_USER_ID = "boss_user_id";
//    private static final String CONFIG_RECEPTION_USER_ID = "reception_user_id";
//    private static final String CONFIG_ALLOWED_TASK_DELAY = "allowed_task_delay";
//
//    private static final GregorianCalendar toDay = new GregorianCalendar();
//
//    private int bossUserID = 0;
//    private int receptionUserID = 0;
//    private int allowedTaskDelay = 0;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "Tasker";
//            if( args.length == 1 ) {
//                new Tasker( args[ 0 ] ).run();
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
//    public Tasker( String aConfigFileName ) {
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
//    public void loadConfig() {
//        super.loadConfig();
//
//        bossUserID = Integer.parseInt( hmConfig.get( CONFIG_BOSS_USER_ID ) );
//        receptionUserID = Integer.parseInt( hmConfig.get( CONFIG_RECEPTION_USER_ID ) );
//        allowedTaskDelay = Integer.parseInt( hmConfig.get( CONFIG_ALLOWED_TASK_DELAY ) );
//    }
//
//    protected boolean isRunOnce() { return true; }
//
//    protected void cycle() throws Throwable {
//        //--- загрузка конфигурации пользователей
//        HashMap<Integer,UserConfig> hmUserConfig = UserConfig.getConfig( alStm.get( 0 ) );
//
//        for( int userID : hmUserConfig.keySet() ) {
//            //--- босс сам себя наказывать не будет (наверное)
//            if( userID == bossUserID ) continue;
//            //--- приёмную наказывать тоже не за что - она инструмент наказания
//            if( userID == receptionUserID ) continue;
//
//            //--- ищем самое старое просроченное поручение с последним ответом от босса
//
//            //--- загрузим список просроченных поручений
//            ArrayList<Integer> alTaskID = new ArrayList<>();
//            ArrayList<String> alTaskSubj = new ArrayList<>();
//            CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                         " SELECT ye , mo , da , id , subj " )
//                .append( " FROM OFFICE_task " )
//                .append( " WHERE out_user_id = " ).append( bossUserID )
////                .append( " WHERE out_user_id IN ( " ).append( bossUserID ).append( " , " ).append( receptionUserID ).append( " ) " )
//                .append( " AND in_user_id = " ).append( userID )
//                .append( " AND in_archive = 0 " )
//                .append( " ORDER BY ye , mo , da " ).toString() );
//            while( rs.next() ) {
//                GregorianCalendar gc = new GregorianCalendar( rs.getInt( 1 ), rs.getInt( 2 ) - 1, rs.getInt( 3 ) );
//                //--- наказания за не очень просроченное поручение не будет
//                gc.add( GregorianCalendar.DAY_OF_MONTH, allowedTaskDelay );
//                //--- уже пошли поздние даты, можно выходить
//                if( gc.after( toDay ) ) break;
//                alTaskID.add( rs.getInt( 4 ) );
//                alTaskSubj.add( rs.getString( 5 ) );
//            }
//            rs.close();
//
//            //--- среди просроченных ищем поручение с последним сообщением от босса
//            //--- (т.е. поручение висит в просроченных не по вине босса)
//            for( int taskIndex = 0; taskIndex < alTaskID.size(); taskIndex++ ) {
//                int taskID = alTaskID.get( taskIndex );
//                //--- если переписка пуста, значит подчинённый должен был ответить
//                int messageUserID = bossUserID;
//                rs = alStm.get( 0 ).executeQuery( new StringBuilder( " SELECT " )
//                    .append( alStm.get( 0 ).getPreLimit( 1 ) )
//                    .append( " user_id " )
//                    .append( " FROM OFFICE_task_thread " )
//                    .append( " WHERE task_id = " ).append( taskID )
//                    .append( alStm.get( 0 ).getMidLimit( 1 ) )
//                    .append( " ORDER BY ye DESC , mo DESC , da DESC , ho DESC , mi DESC " )
//                    .append( alStm.get( 0 ).getPostLimit( 1 ) ) );
//                if( rs.next() ) messageUserID = rs.getInt( 1 );
//                rs.close();
//                //--- если последнее сообщение было от босса,
//                //--- то сейчас последует наказание
//                if( messageUserID == bossUserID ) {
//                    //--- перенести поручение на приёмную, вставить в тему поручения "! Пригласить" и фамилию ответственного
//                    String sqlTask = new StringBuilder(
//                            " UPDATE OFFICE_task SET in_user_id = " ).append( receptionUserID )
//                        .append( " , ye = " ).append( toDay.get( GregorianCalendar.YEAR ) )
//                        .append( " , mo = " ).append( toDay.get( GregorianCalendar.MONTH ) + 1 )
//                        .append( " , da = " ).append( toDay.get( GregorianCalendar.DAY_OF_MONTH ) )
//                        .append( " , subj = '" ).append( "! Пригласить: " ).append( hmUserConfig.get( userID ).getUserFullName() )
//                            .append( "\n по поручению: " ).append( alTaskSubj.get( taskIndex ) )
//                        .append( "' WHERE id = " ).append( taskID ).toString();
//                    //--- в переписку добавить "!" и userID ответственного для последующих отчетов
//                    int rowID = alStm.get( 0 ).getNextID( "OFFICE_task_thread", "id" );
//                    String sqlTaskThread = new StringBuilder(
//                        " INSERT INTO OFFICE_task_thread ( id , user_id , task_id , ye , mo , da , ho , mi , message ) VALUES ( " )
//                        .append( rowID ).append( " , " ).append( bossUserID ).append( " , " ).append( taskID ).append( " , " )
//                        .append( toDay.get( GregorianCalendar.YEAR ) ).append( " , " )
//                        .append( toDay.get( GregorianCalendar.MONTH ) + 1 ).append( " , " )
//                        .append( toDay.get( GregorianCalendar.DAY_OF_MONTH ) ).append( " , 0 , 0 , '!" )
//                        .append( userID ).append( "' ) " ).toString();
//
//                    alStm.get( 0 ).executeUpdate( sqlTask );
//                    alStm.get( 0 ).executeUpdate( sqlTaskThread );
//
//                    //--- одного наказания за один раз достаточно
//                    break;
//                }
//            }
//        }
//        alConn.get( 0 ).commit();
//    }
//
//}
