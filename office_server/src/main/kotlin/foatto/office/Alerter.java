//package foatto.office;
//
//import foatto.core_server.service.CoreServiceWorker;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_client.sql.CoreAdvancedResultSet;
//import foatto.core_client.util.AdvancedLogger;
//import foatto.core_client.util.StringFunction;
//import foatto.sql.AdvancedConnection;
//
//import javax.mail.*;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
//import java.util.*;
//
//public class Alerter extends CoreServiceWorker {
//
//    private static final String CONFIG_SMTP_SERVER = "smtp_server";
//    private static final String CONFIG_SMTP_PORT = "smtp_port";
//    private static final String CONFIG_SMTP_LOGIN = "smtp_login";
//    private static final String CONFIG_SMTP_PASSWORD = "smtp_password";
//    private static final String CONFIG_SMTP_OPTION_COUNT = "smtp_option_count";
//    private static final String CONFIG_SMTP_OPTION_NAME_ = "smtp_option_name_";
//    private static final String CONFIG_SMTP_OPTION_VALUE_ = "smtp_option_value_";
//    private static final String CONFIG_POP3_SERVER = "pop3_server";
//    private static final String CONFIG_POP3_LOGIN = "pop3_login";
//    private static final String CONFIG_POP3_PASSWORD = "pop3_password";
//    private static final String CONFIG_POP3_OPTION_COUNT = "pop3_option_count";
//    private static final String CONFIG_POP3_OPTION_NAME_ = "pop3_option_name_";
//    private static final String CONFIG_POP3_OPTION_VALUE_ = "pop3_option_value_";
//    private static final String CONFIG_CYCLE_PAUSE = "cycle_pause";
//
//    private static final int ACTION_EMPTY       = 0;
//    private static final int ACTION_DELETE      = 1;
//    private static final int ACTION_CHANGE_USER = 2;
//    private static final int ACTION_TIME_SHIFT  = 3;
//    private static final int ACTION_TIME_SET    = 4;
//
//    private String smtpServer = null;
//    private int smtpPort = 0;
//    private String smtpLogin = null;
//    private String smtpPassword = null;
//    private HashMap<String,String> hmSmtpOption = new HashMap<>();
//
//    private String pop3Server = null;
//    private String pop3Login = null;
//    private String pop3Password = null;
//    private HashMap<String,String> hmPop3Option = new HashMap<>();
//    private long cyclePause = 0;
//
//    private HashMap<Integer,UserConfig> hmUserConfig = null;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "Alerter";
//            if( args.length == 1 ) {
//                new Alerter( args[ 0 ] ).run();
//                exitCode = 1;
//            }
//            else System.out.println( new StringBuilder( "Usage: " ).append( serviceWorkerName ).append( " <ini-file-name>" ).toString() );
//        }
//        catch( Throwable t ) {
//            t.printStackTrace();
//        }
//        exitProcess( exitCode );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public Alerter( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    protected boolean isRunOnce() { return false; }
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
//        smtpServer = hmConfig.get( CONFIG_SMTP_SERVER );
//        smtpPort = Integer.parseInt( hmConfig.get( CONFIG_SMTP_PORT ) );
//
//        smtpLogin = hmConfig.get( CONFIG_SMTP_LOGIN );
//        smtpPassword = hmConfig.get( CONFIG_SMTP_PASSWORD );
//
//        int smtpOptionCount = Integer.parseInt( hmConfig.get( CONFIG_SMTP_OPTION_COUNT ) );
//        for( int i = 0; i < smtpOptionCount; i++ )
//            hmSmtpOption.put( hmConfig.get( CONFIG_SMTP_OPTION_NAME_ + i ),
//                              hmConfig.get( CONFIG_SMTP_OPTION_VALUE_ + i ) );
//
//        pop3Server = hmConfig.get( CONFIG_POP3_SERVER );
//
//        pop3Login = hmConfig.get( CONFIG_POP3_LOGIN );
//        pop3Password = hmConfig.get( CONFIG_POP3_PASSWORD );
//
//        int pop3OptionCount = Integer.parseInt( hmConfig.get( CONFIG_POP3_OPTION_COUNT ) );
//        for( int i = 0; i < pop3OptionCount; i++ )
//            hmPop3Option.put( hmConfig.get( CONFIG_POP3_OPTION_NAME_ + i ),
//                              hmConfig.get( CONFIG_POP3_OPTION_VALUE_ + i ) );
//
//        cyclePause = Integer.parseInt( hmConfig.get( CONFIG_CYCLE_PAUSE ) ) * 1000L;
//    }
//
//    protected void cycle() throws Throwable {
//        //--- между циклами/проходами могут появиться новые данные по адресам электронной почты,
//        //--- лучше их перезагружать каждый раз, не дожидаясь очередной перезагрузки
//        /*if( hmUserConfig == null )*/ hmUserConfig = UserConfig.getConfig( alStm.get( 0 ) );
//
//        //--- загрузка оповещений на отправку
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder( " SELECT " )
//            .append( alStm.get( 0 ).getPreLimit( 1 ) )
//            .append( " id , tag , row_id FROM SYSTEM_alert " )
//            .append( " WHERE alert_time <= " ).append( System.currentTimeMillis() / 1000 )
//            .append( alStm.get( 0 ).getMidLimit( 1 ) )
//            .append( " ORDER BY alert_time " )
//            .append( alStm.get( 0 ).getPostLimit( 1 ) ) );
//        if( rs.next() ) {
//            int alertID = rs.getInt( 1 );
//            String tag = rs.getString( 2 );
//            int rowID = rs.getInt( 3 );
//            rs.close();
//
//            if( tag.equals( mTask.ALERT_TAG ) ) {
//                sendTask( rowID );
//                AdvancedLogger.info( "New Task Sended." );
//            }
//            else if( tag.equals( mTaskThread.ALERT_TAG ) ) {
//                sendTaskThread( rowID );
//                AdvancedLogger.info( "New TaskThread Sended." );
//            }
//            else if( tag.equals( mReminder.ALERT_TAG ) ) {
//                sendReminder( rowID );
//                AdvancedLogger.info( "Reminder Sended." );
//            }
//            else AdvancedLogger.error( new StringBuilder( "Unknown tag = " ).append( tag ).toString() );
//
//            alStm.get( 0 ).executeUpdate( new StringBuilder( " DELETE FROM SYSTEM_alert WHERE id = " ).append( alertID ) );
//            alConn.get( 0 ).commit();
//        }
//        //--- если отправлять нечего, посмотрим, нет ли входящей почты по оповещениям
//        else {
//            rs.close();
//            AdvancedLogger.info( "No outgoing alerts. Incoming mail checking..." );
//
//            Object[] arrMail = receiveMail();
//            //--- если входящей почты нет, сделаем паузу
//            if( arrMail == null ) {
//                AdvancedLogger.info( new StringBuilder( "No incoming mail. Pause " ).append( cyclePause / 1000 ).append( " sec." ).toString() );
//                Thread.sleep( cyclePause );
//            }
//            else {
//                //--- парсим тему письма, ищем TAG и ROW_ID
////                "Office" ).append( '#' ).append( mTaskThread.ALERT_TAG )
////                                 .append( '#' ).append( taskID ).append( '#' ).append( arrUserID[ i ] ).append( '#' )
//                StringTokenizer st = new StringTokenizer( (String) arrMail[ 1 ], "#" );
//                String tag = "";
//                try {
//                    st.nextToken(); // пропускаем начальные "Re:" и/или "Office"
//                    tag = st.nextToken();
//                }
//                catch( NoSuchElementException nsee ) {}
//
//                //--- обрабатываем подходящий тег
//                //--- ответ на создание задачи ложится в виде сообщения в переписку
//                if( tag.equals( mTask.ALERT_TAG ) || tag.equals( mTaskThread.ALERT_TAG ) ) {
//                    int taskID = 0;
//                    int userID = 0;
//                    try {
//                        taskID = Integer.parseInt( st.nextToken() );
//                        userID = Integer.parseInt( st.nextToken() );
//                    }
//                    catch( NoSuchElementException | NumberFormatException e ) {}
//
//                    //--- если получены параметры поручения
//                    if( taskID != 0 && userID != 0 ) {
//                        receiveTaskThread( taskID, userID, (Date) arrMail[ 0 ], (String) arrMail[ 2 ] );
//                        AdvancedLogger.info( "New TaskThread Received." );
//                    }
//                }
//            }
//        }
//    }
//
//    private void sendTask( int rowID ) throws Throwable {
//        //--- загрузим поручение
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                " SELECT out_user_id , in_user_id , subj , ye , mo , da FROM OFFICE_task WHERE id = " )
//                .append( rowID ).toString() );
//        if( ! rs.next() ) {
//            rs.close();
//            AdvancedLogger.error( new StringBuilder( "Task not found for ID = " ).append( rowID ).toString() );
//            return;
//        }
//        int outUserID = rs.getInt( 1 );
//        int inUserID = rs.getInt( 2 );
//        String taskSubj = rs.getString( 3 );
//        int[] arrDT = { rs.getInt( 4 ), rs.getInt( 5 ), rs.getInt( 6 ), 0, 0, 0 };
//        rs.close();
//
//AdvancedLogger.debug( "--- send new task ---" );
//AdvancedLogger.debug( new StringBuilder( "from UserID = " ).append( outUserID ).toString() );
//AdvancedLogger.debug( new StringBuilder( "to UserID = " ).append( inUserID ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Task ID = " ).append( rowID ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Task Subj = " ).append( taskSubj ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Task Day = " ).append( arrDT[ 2 ] ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Task Month = " ).append( arrDT[ 1 ] ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Task Year = " ).append( arrDT[ 0 ] ).toString() );
//
//        //--- если этого пользователя не существует или нет e-mail для оповещений, пропускаем из обработки
//        String eMail = hmUserConfig.get( inUserID ) == null ? null : hmUserConfig.get( inUserID ).getEMail();
//AdvancedLogger.debug( new StringBuilder( "to User e-mail: " ).append( eMail ).toString() );
//        if( eMail != null && ! eMail.trim().isEmpty() && eMail.contains( "@" ) ) {
//            //--- проверим, а не прочитано ли уже оппонентом это сообщение
//            rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                        " SELECT * FROM SYSTEM_new WHERE table_name = 'OFFICE_task' AND row_id = " )
//                        .append( rowID ).append( " AND user_id = " ).append( inUserID ).toString() );
//            boolean isReaded = rs.next();
//            rs.close();
//            //--- если ещё не прочитано, то стоит оповестить
//            if( ! isReaded ) {
//                //--- готовим тему письма
//                StringBuilder sbMailSubj = new StringBuilder( "Office" ).append( '#' ).append( mTask.ALERT_TAG )
//                                 .append( '#' ).append( rowID ).append( '#' ).append( inUserID ).append( '#' );
//                //--- готовим текст самого письма
//                StringBuilder sbMailBody = new StringBuilder( "Новое поручение:" )
//                    .append( "\nПоручитель: " ).append( outUserID == 0 ? "" : hmUserConfig.get( outUserID ).getUserFullName() )
//                    .append( "\nТема поручения: " ).append( taskSubj )
//                    .append( "\nСрок исполнения: " ).append( StringFunction.DateTime_DMYHMS( arrDT ) );
//                //--- отправляем письмо
//                sendMail( eMail, sbMailSubj.toString(), sbMailBody.toString() );
//            }
//            else AdvancedLogger.debug( "Task not sended: task already readed by user." );
//        }
//        AdvancedLogger.debug( "---------------------" );
//    }
//
//    private void sendTaskThread( int rowID ) throws Throwable {
//        //--- загрузим новое сообщение из переписки
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                    " SELECT user_id , task_id FROM OFFICE_task_thread WHERE id = " ).append( rowID ).toString() );
//        if( ! rs.next() ) {
//            rs.close();
//            AdvancedLogger.error( new StringBuilder( "Task thread not found for ID = " ).append( rowID ).toString() );
//            return;
//        }
//        int curUserID = rs.getInt( 1 );
//        int taskID = rs.getInt( 2 );
//        rs.close();
//
//        //--- загрузим само поручение
//        rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                    " SELECT out_user_id , in_user_id , subj " )
//           .append( " FROM OFFICE_task WHERE id = " ).append( taskID ).toString() );
//        if( ! rs.next() ) {
//            rs.close();
//            AdvancedLogger.error( new StringBuilder( "Task not found for ID = " ).append( taskID ).toString() );
//            return;
//        }
//        int[] arrUserID = { rs.getInt( 1 ), rs.getInt( 2 ) };
//        String taskSubj = rs.getString( 3 );
//        rs.close();
//
//AdvancedLogger.debug( "--- send new task thread ---" );
//AdvancedLogger.debug( new StringBuilder( "Current UserID = " ).append( curUserID ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Task ID = " ).append( taskID ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Task Subj = " ).append( taskSubj ).toString() );
//
//        //--- для каждой стороны поручения
//        for( int i = 0; i < arrUserID.length; i++ ) {
//            //--- автора последнего сообщения пропускаем из рассмотрения
//            if( arrUserID[ i ] == curUserID ) continue;
//            //--- если у этого пользователя нет e-mail для оповещений, пропускаем из обработки
//            String eMail = hmUserConfig.get( arrUserID[ i ] ).getEMail();
//AdvancedLogger.debug( new StringBuilder( "to User e-mail: " ).append( eMail ).toString() );
//            if( eMail == null || eMail.trim().isEmpty() || ! eMail.contains( "@") ) continue;
//
//            //--- проверим, а не прочитано ли уже оппонентом это сообщение
//            rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                        " SELECT * FROM SYSTEM_new WHERE table_name = 'OFFICE_task_thread' AND row_id = " )
//                .append( rowID ).append( " AND user_id = " ).append( arrUserID[ i ] ).toString() );
//            boolean isReaded = rs.next();
//            rs.close();
//            //--- если ещё не прочитано, то стоит оповестить
//            if( ! isReaded ) {
//                StringBuilder sbThread = new StringBuilder();
//                boolean isOriginalMessagePosted = false;
//                //--- загрузим последние сообщения из переписки (в обратном порядке)
//                rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                            " SELECT id , user_id , ye , mo , da , ho , mi , message " )
//                   .append( " FROM OFFICE_task_thread WHERE task_id = " ).append( taskID )
//                   .append( " ORDER BY ye DESC , mo DESC , da DESC , ho DESC , mi DESC " ).toString() );
//                while( rs.next() ) {
//                    int id = rs.getInt( 1 );
//                    int userID = rs.getInt( 2 );
//                    int[] arrDT = { rs.getInt( 3 ), rs.getInt( 4 ), rs.getInt( 5 ), rs.getInt( 6 ), rs.getInt( 7 ), 0 };
//                    String msg = rs.getString( 8 );
//
//                    String userFullName = userID == 0 ? "" : hmUserConfig.get( userID ).getUserFullName();
//
//                    StringBuilder sbTmp = new StringBuilder( userFullName )
//                        .append( " [" ).append( StringFunction.DateTime_DMYHMS( arrDT ) ).append( "]:\n" )
//                        .append( msg ).append( "\n\n" );
//                    //--- сообщения в тело письма складываем в прямом порядке
//                    sbThread.insert( 0, sbTmp );
//                    //--- собственно оригинальное сообщение, ради которого весь шум, уже добавлено в письмо?
//                    if( id == rowID ) isOriginalMessagePosted = true;
//                    //--- если оригинальное сообщение уже добавлено в письмо,
//                    //--- то складываем вплоть до предыдущего сообщения от оппонента
//                    //--- (чтобы оппонент не потерял нити обсуждения)
//                    if( isOriginalMessagePosted && userID != curUserID ) break;
//                }
//                rs.close();
//                //--- готовим тему письма
//                StringBuilder sbMailSubj = new StringBuilder( "Office" ).append( '#' ).append( mTaskThread.ALERT_TAG )
//                                 .append( '#' ).append( taskID ).append( '#' ).append( arrUserID[ i ] ).append( '#' );
//                //--- готовим текст самого письма
//                StringBuilder sbMailBody = new StringBuilder( "Обсуждение по поручению:\n" )
//                    .append( taskSubj ).append( "\n-----------------------------------\n" ).append( sbThread );
//                //--- отправляем письмо
//                sendMail( eMail, sbMailSubj.toString(), sbMailBody.toString() );
//            }
//            else AdvancedLogger.debug( "Task thread not sended: task already readed by user." );
//        }
//        AdvancedLogger.debug( "---------------------" );
//    }
//
//    private void sendReminder( int rowID ) throws Throwable {
//        //--- загрузим напоминание
////  people_id         INT,
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                " SELECT user_id , type , ye , mo , da , ho , mi , subj , descr FROM OFFICE_reminder WHERE id = " )
//                .append( rowID ).toString() );
//        if( ! rs.next() ) {
//            rs.close();
//            AdvancedLogger.error( new StringBuilder( "Reminder not found for ID = " ).append( rowID ).toString() );
//            return;
//        }
//        int userID = rs.getInt( 1 );
//        int type = rs.getInt( 2 );
//        int[] arrDT = { rs.getInt( 3 ), rs.getInt( 4 ), rs.getInt( 5 ), rs.getInt( 6 ), rs.getInt( 7 ), 0 };
//        String subj = rs.getString( 8 );
//        String descr = rs.getString( 9 );
//        rs.close();
//
//AdvancedLogger.debug( "--- send new reminder ---" );
//AdvancedLogger.debug( new StringBuilder( "Reminder ID = " ).append( rowID ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Type = " ).append( type ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Day = " ).append( arrDT[ 2 ] ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Month = " ).append( arrDT[ 1 ] ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Year = " ).append( arrDT[ 0 ] ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Hour = " ).append( arrDT[ 3 ] ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Minute = " ).append( arrDT[ 4 ] ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Subj = " ).append( subj ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Reminder Descr = " ).append( descr ).toString() );
//
//        //--- если этого пользователя не существует или нет e-mail для оповещений, пропускаем из обработки
//        String eMail = hmUserConfig.get( userID ) == null ? null : hmUserConfig.get( userID ).getEMail();
//AdvancedLogger.debug( new StringBuilder( "to User e-mail: " ).append( eMail ).toString() );
//        if( eMail != null && ! eMail.trim().isEmpty() && eMail.contains( "@" ) ) {
//            //--- готовим тему письма
//            StringBuilder sbMailSubj = new StringBuilder( "Office" ).append( '#' ).append( mReminder.ALERT_TAG );
//            //.append( '#' ).append( rowID ).append( '#' ).append( userID ).append( '#' ); - без нужды
//            //--- готовим текст самого письма
//            StringBuilder sbMailBody =
//                new StringBuilder( "Напоминание:" ).append( mReminder.hmReminderName.get( type ) )
//                .append( "\nВремя: " ).append( StringFunction.DateTime_DMYHMS( arrDT ) )
//                .append( "\nТема: " ).append( subj )
//                .append( "\nПримечания: " ).append( descr );
//            //--- отправляем письмо
//            sendMail( eMail, sbMailSubj.toString(), sbMailBody.toString() );
//        }
//        AdvancedLogger.debug( "---------------------" );
//    }
//
//    //--- отправка письма
//    private void sendMail( String eMail, String subj, String body ) throws Throwable {
//        Properties props = System.getProperties();
//        for( String key : hmSmtpOption.keySet() )
//            props.put( key, hmSmtpOption.get( key ) );
//
//        Session session = Session.getDefaultInstance( props, null );
//        Transport transport = session.getTransport( "smtp" );
//
//        transport.connect( smtpServer, smtpPort, smtpLogin, smtpPassword );
//
//        Message msg = new MimeMessage( session );
//        msg.setSentDate( new Date() );
//        msg.setFrom( new InternetAddress( smtpLogin ) );
//        msg.setRecipient( Message.RecipientType.TO, new InternetAddress( eMail ) );
//        msg.setSubject( subj );
//        msg.setText( body );
//        msg.saveChanges();
//
//        transport.sendMessage( msg, new InternetAddress[] { new InternetAddress( eMail ) } );
//        transport.close();
//    }
//
//    //--- получение письма с удалением его из почтового ящика
//    private Object[] receiveMail() throws Throwable {
//        Object[] arrResult = null;
//
//        Properties props = new Properties();
//        for( String key : hmPop3Option.keySet() )
//            props.put( key, hmPop3Option.get( key ) );
//
//        //--- по совету на одном из форумов - одно из многочисленнейших условий для корректной работы с MS Exchange
//        //Session session = Session.getDefaultInstance( props, null );
//        Session session = Session.getInstance( props, null );
//
//        Store store = session.getStore( "pop3" );
//        store.connect( pop3Server, pop3Login, pop3Password );
//
//        Folder folder = store.getFolder( "INBOX" );
//        folder.open( Folder.READ_WRITE );
//
//        //--- вариант получения списка писем с одновременным удалением их из ящика:
//        //--- Message[] arrMessage = folder.expunge();
//        //--- но:
//        //--- 1. не самый удачный вариант, т.к. в процессе разбора писем программа может вылететь с ошибкой и
//        //--- недообработать письма, а они уже удалены
//        //--- 2. якобы поддерживается не всеми серверами, лучше использовать каноничный
//        //--- Message.setFlag( Flags.Flag.DELETED, true );
//
//        Message[] arrMessage = folder.getMessages();
//        for( int i = 0; i < arrMessage.length; i++ ) {
//            arrResult = new Object[] { arrMessage[ i ].getSentDate(),
//                                       arrMessage[ i ].getSubject(), arrMessage[ i ].getContent().toString() };
//            //--- пометим на удаление
//            arrMessage[ i ].setFlag( Flags.Flag.DELETED, true );
//            //--- будем пока по одному письму разбирать
//            break;
//        }
//        //--- Закрыть соединение с удалением помеченных записей
//        folder.close( true );
//        store.close();
//
//        return arrResult;
//    }
//
//    private void receiveTaskThread( int taskID, int userID, Date date, String mailBody ) {
//        int[] arrDT = StringFunction.DateTime_Arr( TimeZone.getDefault(), date.getTime() );
//
//AdvancedLogger.debug( "--- MAIL BODY start ---" );
//AdvancedLogger.debug( mailBody );
//AdvancedLogger.debug( "--- MAIL BODY end ---" );
//
//        StringTokenizer stMailBody = new StringTokenizer( mailBody, "\n" );
//
//AdvancedLogger.debug( "--- New Task Thread Message ---" );
//        //--- вырезаем и нормализуем первую строку сообщения - возможную команду/данные
//        String cmd = "";
//        StringBuilder sbHtmlMessage = null;
//        if( stMailBody.hasMoreElements() ) {
//            cmd = stMailBody.nextToken().trim().toUpperCase();
//AdvancedLogger.debug( new StringBuilder( "First row = " ).append( cmd ).toString() );
//            //--- пошла новая неотключаемая мода у Apple - письма исключительно в html-формате
//            if( cmd.contains( "<HTML>" ) ) {
//                sbHtmlMessage = new StringBuilder();
//                //--- перематываем до <BODY
//                while( stMailBody.hasMoreTokens() ) {
//                    cmd = stMailBody.nextToken().trim().toUpperCase();
//                    if( cmd.contains( "<BODY" ) ) break;
//                }
//                //--- теперь сама команда
//                cmd = stMailBody.nextToken().trim().toUpperCase().replace( "<BR>", "" ).replace( "&NBSP;", "" );
//                //--- собираем инфу до <DIV
//                while( stMailBody.hasMoreTokens() ) {
//                    String msg = stMailBody.nextToken().toUpperCase();
//                    if( msg.contains( "<DIV" ) ) break;
//                    sbHtmlMessage.append( msg.replace( "<BR>", " " ).replace( "&NBSP;", " " ) );
//                }
//AdvancedLogger.debug( new StringBuilder( "First row from HTML = " ).append( cmd ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Rest HTML message= '" ).append( sbHtmlMessage ).append( '\'' ).toString() );
//            }
//        }
//
//        int action = ACTION_EMPTY;
//        String actionDescr = null;
//        int newUserID = 0;
//        int dayShift = 0;
//        int newDa = 0, newMo = 0, newYe = 0;
//        //--- удалить (а точнее, перенести задачу в архив)
//        if( cmd.equals( "УДАЛИТЬ" ) ) {
//            action = ACTION_DELETE;
//            actionDescr = "# Поручение перемещено в архив #";
//        }
//        else {
//            //--- если есть однозначное совпадение (содержание в полном имени) пользователя, то переназначить
//            UserConfig userConfig = hmUserConfig.get( userID );
//            HashMap<Integer,String> hmUserName = userConfig.getUserFullNames();
//            for( Integer uID : hmUserName.keySet() ) {
//                String userName = hmUserName.get( uID ).toUpperCase();
//                //--- есть такой пользователь
//                if( userName.contains( cmd ) ) {
//                    //--- если это в первый (единственный) раз, то запомним этого пользователя
//                    if( newUserID == 0 ) newUserID = uID;
//                    //--- если это уже не в первый раз, то налицо неоднозначность поиска -
//                    //--- обнуляем предыдущий результат поиска и немедленно выходим
//                    else {
//                        newUserID = 0;
//                        break;
//                    }
//                }
//            }
//            if( newUserID != 0 ) {
//                action = ACTION_CHANGE_USER;
//                actionDescr = new StringBuilder( "# Поручение перенаправлено пользователю: " )
//                                        .append( hmUserName.get( newUserID ) ).append( " #" ).toString();
//            }
//            else {
//                //--- проверям на наличии числа - сдвига срока или даты, на которое перенести поручение
//                StringTokenizer st = new StringTokenizer( cmd, "." );
//                switch( st.countTokens() ) {
//                //--- вероятно, там просто число - сдвиг даты поручения
//                case 1:
//                    try {
//                        //--- попытаемся его преобразовать в число
//                        dayShift = Integer.parseInt( cmd );
//                        action = ACTION_TIME_SHIFT;
//                        actionDescr = new StringBuilder( "# Поручение продлено на " ).append( dayShift ).append( " дней #" ).toString();
//                    }
//                    catch( NoSuchElementException | NumberFormatException e ) {}
//                    break;
//                //--- вероятно, там новый срок поручения в виде ДД.ММ текущего года
//                case 2:
//                    try {
//                        //--- попытаемся преобразовать числа даты\месяца
//                        newDa = Integer.parseInt( st.nextToken() );
//                        newMo = Integer.parseInt( st.nextToken() );
//                        //--- достаточно пока простой проверки
//                        if( newDa > 0 && newDa < 32 && newMo > 0 && newMo < 32 ) {
//                            newYe = arrDT[ 0 ]; // берём текущий год
//                            action = ACTION_TIME_SET;
//                            actionDescr = new StringBuilder( "# Поручение продлено до " )
//                                        .append( newDa ).append( '.' ).append( newMo ).append( '.' ).append( newYe ).append( " #" ).toString();
//                        }
//                    }
//                    catch( NoSuchElementException | NumberFormatException e ) {}
//                    break;
//                //--- вероятно, там новый срок поручения в виде ДД.ММ.ГГ или ДД.ММ.ГГГГ текущего года
//                case 3:
//                    try {
//                        //--- попытаемся преобразовать числа даты\месяца
//                        newDa = Integer.parseInt( st.nextToken() );
//                        newMo = Integer.parseInt( st.nextToken() );
//                        newYe = Integer.parseInt( st.nextToken() );
//                        //--- достаточно пока простой проверки
//                        if( newDa > 0 && newDa < 32 && newMo > 0 && newMo < 32 && newYe > 0 ) {
//                            //--- если там двузначное число года, то добавим 2000
//                            if( newYe < 2000 ) newYe += 2000;
//                            action = ACTION_TIME_SET;
//                            actionDescr = new StringBuilder( "# Поручение продлено до " )
//                                        .append( newDa ).append( '.' ).append( newMo ).append( '.' ).append( newYe ).append( " #" ).toString();
//                        }
//                    }
//                    catch( NoSuchElementException | NumberFormatException e ) {}
//                    break;
//                }
//            }
//        }
//AdvancedLogger.debug( new StringBuilder( "Action = " ).append( action ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Action Descr = " ).append( actionDescr ).toString() );
//AdvancedLogger.debug( new StringBuilder( "New User ID = " ).append( newUserID ).toString() );
//AdvancedLogger.debug( new StringBuilder( "Day Shift = " ).append( dayShift ).toString() );
//AdvancedLogger.debug( new StringBuilder( "New Day = " ).append( newDa ).toString() );
//AdvancedLogger.debug( new StringBuilder( "New Month = " ).append( newMo ).toString() );
//AdvancedLogger.debug( new StringBuilder( "New Year = " ).append( newYe ).toString() );
//
//        //--- если никакая команда не распознана, то просто сдвигаем срок на один день
//        if( action == ACTION_EMPTY ) {
//            dayShift = 1;
//            action = ACTION_TIME_SHIFT;
//            actionDescr = new StringBuilder( cmd ).append( '\n' ).append( "# Поручение продлено на " ).append( dayShift ).append( " день #" ).toString();
//        }
//
//        //--- составляем сообщение в систему
//        StringBuilder sbMailBody = new StringBuilder( actionDescr ).append( '\n' );
//        //--- если это обычное письмо в обычном текстовом формате - пропускаем строки-цитаты из письма
//        if( sbHtmlMessage == null ) {
//            while( stMailBody.hasMoreTokens() ) {
//                String s = stMailBody.nextToken();  //.trim()
//                if( ! s.startsWith( ">" ) )
//                    sbMailBody.append( sbMailBody.length() == 0 ? "" : "\n" ).append( s );
//            }
//        }
//        //--- если это новомодное письмо в html-формате - колдуем дальше
//        else {
//            sbMailBody.append( sbHtmlMessage );
//            //--- остальное игнорируем нафиг
//        }
//
//        //--- в любом случае пишем сообщение
//        int rowID = alStm.get( 0 ).getNextID( "OFFICE_task_thread", "id" );
//        alStm.get( 0 ).executeUpdate( new StringBuilder(
//            " INSERT INTO OFFICE_task_thread ( id , user_id , task_id , ye , mo , da , ho , mi , message ) VALUES ( " )
//            .append( rowID ).append( " , " ).append( userID ).append( " , " ).append( taskID ).append( " , " )
//            .append( arrDT[ 0 ] ).append( " , " ).append( arrDT[ 1 ] ).append( " , " ).append( arrDT[ 2 ] )
//            .append( " , " ).append( arrDT[ 3 ] ).append( " , " ).append( arrDT[ 4 ] ).append( " , '" )
//            .append( StringFunction.prepareForSQL( sbMailBody.length() > alConn.get( 0 ).getDialect().getTextFieldMaxSize() / 2 ?
//                     sbMailBody.substring( 0, alConn.get( 0 ).getDialect().getTextFieldMaxSize() / 2 ) : sbMailBody.toString() ) )
//                .append( "' ) " ).toString() );
//        //--- и создаём оповещение по этому сообщению
//        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                    " INSERT INTO SYSTEM_alert ( id , alert_time , tag , row_id ) VALUES ( " )
//                .append( alStm.get( 0 ).getNextID( "SYSTEM_alert", "id" ) )
//                .append( " , " ).append( -1 ).append( " , '" ).append( mTaskThread.ALERT_TAG )
//                .append( "' , " ).append( rowID ).append( " ) " ).toString() );
//
//        //--- если отправитель сообщения является автором поручения, то выполним его команды
//        GregorianCalendar gcNextDay;
//        switch( action ) {
//        case ACTION_DELETE:
//            alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " UPDATE OFFICE_task SET in_archive = 1 WHERE id = " ).append( taskID )
//                .append( " AND out_user_id = " ).append( userID ).toString() );
//            break;
//        case ACTION_CHANGE_USER:
//            //--- сменим исполнителя и продлим поручение на 1 день позже даты отправления его сообщения
//            gcNextDay = new GregorianCalendar( arrDT[ 0 ], arrDT[ 1 ] - 1, arrDT[ 2 ] );
//            gcNextDay.add( GregorianCalendar.DAY_OF_MONTH, 1 );
//            arrDT = StringFunction.DateTime_Arr( gcNextDay );
//            alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " UPDATE OFFICE_task SET in_user_id = " ).append( newUserID ).append( " , ye = " ).append( arrDT[ 0 ] )
//                .append( " , mo = " ).append( arrDT[ 1 ] ).append( " , da = " ).append( arrDT[ 2 ] )
//                .append( " WHERE id = " ).append( taskID )
//                .append( " AND out_user_id = " ).append( userID ).toString() );
//            break;
//        case ACTION_TIME_SHIFT:
//            //--- продлим поручение на N дней позже даты отправления его сообщения
//            gcNextDay = new GregorianCalendar( arrDT[ 0 ], arrDT[ 1 ] - 1, arrDT[ 2 ] );
//            gcNextDay.add( GregorianCalendar.DAY_OF_MONTH, dayShift );
//            arrDT = StringFunction.DateTime_Arr( gcNextDay );
//            alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " UPDATE OFFICE_task SET ye = " ).append( arrDT[ 0 ] ).append( " , mo = " ).append( arrDT[ 1 ] )
//                .append( " , da = " ).append( arrDT[ 2 ] )
//                .append( " WHERE id = " ).append( taskID )
//                .append( " AND out_user_id = " ).append( userID ).toString() );
//            break;
//        case ACTION_TIME_SET:
//            //--- продлим поручение до указанного срока
//            alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " UPDATE OFFICE_task SET ye = " ).append( newYe ).append( " , mo = " ).append( newMo )
//                .append( " , da = " ).append( newDa )
//                .append( " WHERE id = " ).append( taskID )
//                .append( " AND out_user_id = " ).append( userID ).toString() );
//            break;
//        }
//        alConn.get( 0 ).commit();
//AdvancedLogger.debug( "-------------------------------" );
//    }
//
//}
