//package foatto.mms;
//
//import java.util.*;
//
//import javax.mail.Message;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
//
//import foatto.core.app.graphic.CoreGraphicDataContainerLine;
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.StringFunction;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.service.CoreServiceWorker;
//import foatto.mms.core_mms.ObjectConfig;
//import foatto.mms.core_mms.ZoneData;
//import foatto.mms.core_mms.calc.ObjectCalc;
//import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler;
//import foatto.mms.core_mms.sensor.config.SensorConfig;
//import foatto.mms.core_mms.sensor.SensorConfigA;
//import foatto.sql.AdvancedConnection;
//import kotlin.Pair;
//
//public class OverSensorAlerter extends CoreServiceWorker {
//
//    private static final String CONFIG_USER_ID = "user_id";
//    private static final String CONFIG_CHECK_PERIOD = "check_period";
//
//    private static final String CONFIG_SMTP_SERVER = "smtp_server";
//    private static final String CONFIG_SMTP_PORT = "smtp_port";
//    private static final String CONFIG_SMTP_LOGIN = "smtp_login";
//    private static final String CONFIG_SMTP_PASSWORD = "smtp_password";
//    private static final String CONFIG_SMTP_OPTION_NAME_ = "smtp_option_name_";
//    private static final String CONFIG_SMTP_OPTION_VALUE_ = "smtp_option_value_";
//
//    private static HashMap<Integer,String> hmOverSensorType = new HashMap<>();
//    static {
//        hmOverSensorType.put( SensorConfig.SENSOR_WEIGHT, "Датчик веса" );
//        hmOverSensorType.put( SensorConfig.SENSOR_TURN, "Датчик оборотов" );
//        hmOverSensorType.put( SensorConfig.SENSOR_PRESSURE, "Датчик давления" );
//        hmOverSensorType.put( SensorConfig.SENSOR_TEMPERATURE, "Датчик температуры" );
//        hmOverSensorType.put( SensorConfig.SENSOR_VOLTAGE, "Датчик напряжения" );
////        //??? liquid_level - просто уровень жидкости, без заправок/сливов
////        //??? energo_power - электрическая мощность
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private int userID = 0;
//    private int checkPeriod = 0;
//
//    private String smtpServer = null;
//    private int smtpPort = 0;
//    private String smtpLogin = null;
//    private String smtpPassword = null;
//    private HashMap<String,String> hmSmtpOption = new HashMap<>();
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private TimeZone timeZone = TimeZone.getDefault();
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "OverSensorAlerter";
//            if( args.length == 1 ) {
//                new OverSensorAlerter( args[ 0 ] ).run();
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
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public OverSensorAlerter( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        userID = Integer.parseInt( hmConfig.get( CONFIG_USER_ID ) );
//        checkPeriod = Integer.parseInt( hmConfig.get( CONFIG_CHECK_PERIOD ) );
//
//        smtpServer = hmConfig.get( CONFIG_SMTP_SERVER );
//        smtpPort = Integer.parseInt( hmConfig.get( CONFIG_SMTP_PORT ) );
//
//        smtpLogin = hmConfig.get( CONFIG_SMTP_LOGIN );
//        smtpPassword = hmConfig.get( CONFIG_SMTP_PASSWORD );
//
//        int index = 0;
//        while( true ) {
//            String optionName = hmConfig.get( CONFIG_SMTP_OPTION_NAME_ + index );
//            if( optionName == null ) break;
//
//            hmSmtpOption.put( optionName, hmConfig.get( CONFIG_SMTP_OPTION_VALUE_ + index ) );
//
//            index++;
//        }
//    }
//
//    protected void initDB() {
//        for( int i = 0; i < alDBConfig.size(); i++ ) {
//            alConn.add( new AdvancedConnection( alDBConfig.get( i ) ) );
//            alStm.add( alConn.get( i ).createStatement() );
//        }
//    }
//
//    protected boolean isRunOnce() { return true; }
//
//    protected void cycle() throws Throwable {
//
//        HashMap<Integer,String> hmObjectEmail = new HashMap<>();
//
//        //--- загрузка общего списка объектов для оповещения
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT SYSTEM_users.e_mail , MMS_object.id " )
//            .append( " FROM SYSTEM_users , MMS_object , MMS_sensor " )
//            .append( " WHERE SYSTEM_users.id = MMS_object.user_id " )
//            .append( " AND MMS_sensor.object_id = MMS_object.id " )
//            .append( " AND SYSTEM_users.is_disabled = 0 " )
//            .append( " AND MMS_object.is_disabled = 0 " )
//            .append( " AND MMS_sensor.sensor_type IN ( " ).append(
//                StringFunction.getSBFromIterable( hmOverSensorType.keySet(), "," ) ).append( " ) " ) );
//        while( rs.next() ) {
//            String email = rs.getString( 1 );
//            if( email == null || email.trim().isEmpty() ) continue;
//
//            hmObjectEmail.put( rs.getInt( 2 ), email );
//        }
//        rs.close();
//
//        //--- по быстрому выходим, если некого и незачем оповещать
//        if( hmObjectEmail.isEmpty() ) return;
//
//        UserConfig userConfig = UserConfig.getConfig( alStm.get( 0 ), userID );
//        HashMap<Integer,ZoneData> hmZoneData = ZoneData.getZoneData( alStm.get( 0 ), userConfig, 0 );
//
//        GregorianCalendar gcBeg = new GregorianCalendar( timeZone );
//        gcBeg.add( GregorianCalendar.MINUTE, - checkPeriod );
//        GregorianCalendar gcEnd = new GregorianCalendar( timeZone );
//        long begTime = gcBeg.getTimeInMillis();
//        long endTime = gcEnd.getTimeInMillis();
//
//        AnalogGraphicHandler graphicHandler = new AnalogGraphicHandler();
//
//        //--- проходимся по списку объектов
//        for( Integer objectID : hmObjectEmail.keySet() ) {
//            ObjectConfig objectConfig = ObjectConfig.getObjectConfig( alStm.get( 0 ), userConfig, objectID );
//
//            //--- единоразово загрузим данные по всем датчикам объекта
//            Pair<ArrayList<Long>,ArrayList<AdvancedByteBuffer>> pair = ObjectCalc.loadAllSensorData( alStm, objectConfig, begTime, endTime );
//            ArrayList<Long> alRawTime = pair.component1();
//            ArrayList<AdvancedByteBuffer> alRawData = pair.component2();
//
//            //--- по списку контролируемых датчиков
//            for( Integer sensorType : hmOverSensorType.keySet() ) {
//                HashMap<Integer,SensorConfig> hmSensorConfig = objectConfig.hmSensorConfig.get( sensorType );
//                if( hmSensorConfig == null || hmSensorConfig.isEmpty() ) continue;
//
//                for( int portNum : hmSensorConfig.keySet() ) {
//                    SensorConfigA sca = (SensorConfigA) hmSensorConfig.get( portNum );
//
//                    CoreGraphicDataContainerLine aLine = new CoreGraphicDataContainerLine( 0, alRawTime.size(), 1 );
//                    ObjectCalc.getSmoothAnalogGraphicData( alRawTime, alRawData, objectConfig, sca, begTime, endTime, 0, 0,
//                                                           null, null, null, aLine, graphicHandler );
//                    if( aLine.alGLD.isEmpty() ) continue;
//
//                    CoreGraphicDataContainerLine.GraphicLineData gdl = aLine.alGLD.get( aLine.alGLD.size() - 1 );
//                    byte colorIndex = gdl.colorIndex;
//                    if( colorIndex != graphicHandler.getLineNormalColorIndex() ) {
//                        //double over = colorIndex == graphicHandler.getLineCriticalColorIndex() ? gdl.y - sca.maxLimit : sca.minLimit - gdl.y;
//
//                        TreeSet<String> tsZoneName = new TreeSet<>();
//                        //--- могут быть объекты без GPS-датчиков
//                        if( gdl.coord != null ) ObjectCalc.fillZoneList( hmZoneData, 0, gdl.coord, tsZoneName );
//
//                        sendMail( hmObjectEmail.get( objectID ),
//                                  new StringBuilder( objectConfig.name ).append( " : " ).append( hmOverSensorType.get( sensorType ) ).toString(),
//                                  new StringBuilder( "Значение: " ).append( gdl.y )
//                                            .append( "\nМесто: " ).append( StringFunction.getSBFromIterable( tsZoneName, "\n" ) ).toString() );
//                    }
//                }
//            }
//        }
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
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
//
//        AdvancedLogger.debug( new StringBuilder( "Mail sended." )
//                                        .append( "\nSubj: " ).append( subj ).append( "\nBody: " ).append( body ) );
//    }
//
//}
