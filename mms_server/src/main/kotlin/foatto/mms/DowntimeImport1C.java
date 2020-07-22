//package foatto.mms;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.StringFunction;
//import foatto.core_server.service.CoreServiceWorker;
//import foatto.sql.AdvancedConnection;
//import org.w3c.dom.*;
//
//import javax.xml.soap.*;
//import javax.xml.transform.Source;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.stream.StreamResult;
//import java.io.File;
//import java.net.URL;
//import java.util.HashMap;
//import java.util.TimeZone;
//
////--- Добавил новую функцию GetNonExitCars. Она возвращает список ТС на дату, которые не вышли на линию.
////--- Возвращаемые поля Date, CarGosNumber, Status
//
//public class DowntimeImport1C extends CoreServiceWorker {
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private static final String DT_DATE = "m:Date";
//    private static final String DT_GOS_NO = "m:CarGosNumber";
//    private static final String DT_STATUS = "m:Status";
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private static TimeZone timeZone = TimeZone.getDefault();
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private static final String CONFIG_SOAP_URL = "soap_url";
//
//    private String soapURL = null;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "DowntimeImport1C";
//            if( args.length == 1 ) {
//                new DowntimeImport1C( args[ 0 ] ).run();
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
//    public DowntimeImport1C( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    protected boolean isRunOnce() { return true; }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        soapURL = hmConfig.get( CONFIG_SOAP_URL );
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
//    protected void cycle() throws Throwable {
//
//        String curDate = StringFunction.DateTime_YMDHMS( timeZone, System.currentTimeMillis() ).toString().replace( '.', '-' );
//
//long begTime = System.currentTimeMillis();
//long bt = System.currentTimeMillis();
//
//        //--- загрузка списка а/м
//        HashMap<String,Integer> hmObjectID = new HashMap<>();
//        HashMap<String,Integer> hmUserID = new HashMap<>();
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT name , id , user_id FROM MMS_object WHERE id <> 0 " ) );
//        while( rs.next() ) {
//            String name = rs.getString( 1 );
//            hmObjectID.put( name, rs.getInt( 2 ) );
//            hmUserID.put( name, rs.getInt( 3 ) );
//        }
//        rs.close();
//
//AdvancedLogger.info( "TIME: load auto data = " + ( System.currentTimeMillis() - bt ) );
//bt = System.currentTimeMillis();
//
//        //--- создаем соединение
//        SOAPConnectionFactory scf = SOAPConnectionFactory.newInstance();
//        SOAPConnection connection = scf.createConnection();
//
//        SOAPFactory sf = SOAPFactory.newInstance();
//
//        //--- создаем сообщение
//        MessageFactory mf = MessageFactory.newInstance();
//        SOAPMessage message = mf.createMessage();
//
//        //--- создаем объекты сообщения
//        SOAPPart soapPart = message.getSOAPPart();
//        SOAPEnvelope envelope = soapPart.getEnvelope();
//        //--- за ненадобностью пропущен SOAPHeader header = envelope.fillHeader();
//        SOAPBody body = envelope.getBody();
//
//        //--- заполняем тело сообщения
//        SOAPBodyElement bodyElement = body.addBodyElement( sf.createName( "GetNonExitCars", null, "http://www.foat.good" ) );
//
//        SOAPElement element = bodyElement.addChildElement( sf.createName( "DateStart" ) );
//        element.addTextNode( curDate.substring( 0, 10 ) );
//
//        if( AdvancedLogger.isDebugEnabled() ) {
//            //--- в отладочных целях: показать XML-исходник сообщения
//            System.out.println( "SOAP Request Sent:" );
//            message.writeTo( System.out );
//        }
//
//        //--- отправка сообщения и получение ответа
//        SOAPMessage response = connection.call( message, new URL( soapURL ) );
//        //--- закрыть соединение после получения ответа
//        connection.close();
//
//AdvancedLogger.info( new StringBuilder( "TIME: SOAP query & response = " ).append( System.currentTimeMillis() - bt ).toString() );
//bt = System.currentTimeMillis();
//
//        if( AdvancedLogger.isDebugEnabled() ) {
//            System.out.println( "SOAP Response Received:" );
//            //--- создаем транформер
//            TransformerFactory tf = TransformerFactory.newInstance();
//            Transformer transformer = tf.newTransformer();
//            //--- содержимое ответа
//            Source content = response.getSOAPPart().getContent();
//            //--- вывалить в файл
//            StreamResult result = new StreamResult( new File( "soaptest.xml" ) );
//            transformer.transform( content, result );
//        }
//
//        SOAPBody responseBody = response.getSOAPBody();
//
//        NodeList downtimeList = responseBody.getElementsByTagName( "m:Elements" );
//AdvancedLogger.info( "downtimeList = " + downtimeList.getLength() );
//
//        //--- предварительно удаляем все записи за указанную дату (для случаев перезагрузки обновлённых данных)
//        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                " DELETE FROM MMS_downtime WHERE ye = " ).append( curDate.substring( 0, 4 ) )
//                .append( " AND mo = " ).append( curDate.substring( 5, 7 ) )
//                .append( " AND da = " ).append( curDate.substring( 8, 10 ) ) );
//
//        int allStoredDowntimeCount = 0;
//        for( int dti = 0; dti < downtimeList.getLength(); dti++ ) {
//            NodeList downtimeAttrList = downtimeList.item( dti ).getChildNodes();
//
//            String dt = null;
//            String name = null;
//            String reason = null;
//            for( int dtai = 0; dtai < downtimeAttrList.getLength(); dtai++ ) {
//                org.w3c.dom.Node downtimeAttr = downtimeAttrList.item( dtai );
//                String nodeName = downtimeAttr.getNodeName();
//
//                if( nodeName.equals( DT_DATE ) ) {
//                    NodeList downtimeAttrValue = downtimeAttr.getChildNodes();
//                    if( downtimeAttrValue.getLength() == 1 ) dt = downtimeAttrValue.item( 0 ).getNodeValue();
//                }
//                else if( nodeName.equals( DT_GOS_NO ) ) {
//                    NodeList downtimeAttrValue = downtimeAttr.getChildNodes();
//                    if( downtimeAttrValue.getLength() == 1 ) name = downtimeAttrValue.item( 0 ).getNodeValue();
//                }
//                else if( nodeName.equals( DT_STATUS ) ) {
//                    NodeList downtimeAttrValue = downtimeAttr.getChildNodes();
//                    if( downtimeAttrValue.getLength() == 1 ) reason = downtimeAttrValue.item( 0 ).getNodeValue();
//                }
//            }
//AdvancedLogger.debug( " date = " + dt );
//AdvancedLogger.debug( " gos_no = " + name );
//AdvancedLogger.debug( " status = " + reason );
//
//            Integer objectID = hmObjectID.get( name );
//            if( objectID != null ) {
//                int userID = hmUserID.get( name );
//                String ye = dt.substring( 0, 4 );
//                String mo = dt.substring( 5, 7 );
//                String da = dt.substring( 8, 10 );
//
////---UPDATE нет, т.к. предварительно сносятся все записи за указанную дату
////                if( alStm.get( 0 ).executeUpdate( new StringBuilder(
////                        " UPDATE MMS_downtime SET user_id = " ).append( userID ).append( " , " )
////                        .append( " reason = '" ).append( reason ).append( "' " )
////                        .append( " WHERE object_id = " ).append( objectID )
////                        .append( " AND ye = " ).append( ye )
////                        .append( " AND mo = " ).append( mo )
////                        .append( " AND da = " ).append( da ) ) == 0 )
//
//                alStm.get( 0 ).executeUpdate( new StringBuilder(
//                        " INSERT INTO MMS_downtime ( id , user_id , object_id , ye , mo , da , reason ) VALUES ( " )
//                        .append( alStm.get( 0 ).getNextID( "MMS_downtime", "id" ) ).append( " , " )
//                        .append( userID ).append( " , " ).append( objectID ).append( " , " )
//                        .append( ye ).append( " , " ).append( mo ).append( " , " )
//                        .append( da ).append( " , '" ).append( reason ).append( "' ) " ) );
//
//                allStoredDowntimeCount++;
//            }
//            else AdvancedLogger.error( "Unknown object name: " + name );
//        }
//        alConn.get( 0 ).commit();
//
//        //--- заточено под PostgreSQL-диалект
//        if( allStoredDowntimeCount > 0 ) {
//            alStm.get( 0 ).executeUpdate( " REINDEX TABLE MMS_downtime " );
//            alConn.get( 0 ).commit();
////            DBFunction.executeUpdate( stmPM, new StringBuilder( " ALTER INDEX ALL ON MMS_downtime REBUILD " ).toString() );
////            connPM.commit();
//        }
//
//        //--- вывод статистики ---------------------------------------------------------------------------------
//
//        if( allStoredDowntimeCount == 0 ) AdvancedLogger.info( "No downtime data." );
//        else {
//            //--- обработка одного события за цикл может оказаться слишком быстрой и время может оказаться == 0.
//            //--- для этого случая установим время == 1 сек.
//            int workTime = ( getCurrentTimeInt() - begTime );
//            if( workTime == 0 ) workTime = 1;
//
//            AdvancedLogger.info( new StringBuilder( "DATA: downtime count = " ).append( allStoredDowntimeCount ).toString() );
//            AdvancedLogger.info( new StringBuilder( "DATA: time = " ).append( workTime ).append( " [sec]" ).toString() );
//            AdvancedLogger.info( new StringBuilder( "DATA: speed = " ).append( allStoredDowntimeCount * 60L / workTime ).append( " [row/min]" ).toString() );
//        }
//        AdvancedLogger.info( "------------------------------------------------------------" );
//    }
//
//}
//
////--- 1-й этап отладки: показать XML-исходник ответа
////
////            System.out.println( "SOAP Response Received:" );
////            //--- создаем транформер
////            TransformerFactory tf = TransformerFactory.newInstance();
////            Transformer transformer = tf.newTransformer();
////            //--- содержимое ответа
////            Source content = response.getSOAPPart().getContent();
////            //--- показать на экране
////            StreamResult result = new StreamResult( System.out );
////            transformer.transform( content, result );
//
////--- 2-й этап отладки: показать общую структуру ответа
////
////            Iterator it = responseBody.getChildElements( sf.createName( "GetDateResponse", null, "http://www.foat.good" ) );
////            while( it.hasNext() ) {
////                SOAPBodyElement be = (SOAPBodyElement) it.next();
////                System.out.println( "bodyElement 0 = " + be.getTagName() );
////
////                Iterator it1 = be.getChildElements( sf.createName( "return", null, "http://www.foat.good" ) );
////                while( it1.hasNext() ) {
////                    SOAPBodyElement be1 = (SOAPBodyElement) it1.next();
////                    System.out.println( "-bodyElement 1 = " + be1.getTagName() );
////
////                    Iterator it2 = be1.getChildElements( sf.createName( "Elements", null, "http://www.foat.good" ) );
////                    while( it2.hasNext() ) {
////                        SOAPBodyElement be2 = (SOAPBodyElement) it2.next();
////                        System.out.println( "--bodyElement 2 = " + be2.getTagName() );
////
////                        Iterator it3 = be2.getChildElements( sf.createName( "WayListNumber", null, "http://www.foat.good" ) );
////                        while( it3.hasNext() ) {
////                            SOAPBodyElement be3 = (SOAPBodyElement) it3.next();
////                            System.out.println( "---bodyElement 3 = " + be3.getTagName() );
////                        }
////                    }
////                }
////            }
//
////--- 3-й этап отладки: сокращенный/оптимизированный разбор ответа
////
////            NodeList nl = responseBody.getElementsByTagName( "m:Elements" );
////            for( int i = 0; i < nl.getLength(); i++ ) {
////                Node n = nl.item( i );
////                System.out.println("n = " + n.getNodeName());
////                System.out.println("v = " + n.getNodeValue());
////
////                NodeList nl1 = n.getChildNodes();
////                for( int i1 = 0; i1 < nl1.getLength(); i1++ ) {
////                    Node n1 = nl1.item( i1 );
////                    System.out.println("|- n1 = " + n1.getNodeName());
////                    System.out.println("|- v1 = " + n1.getNodeValue());
////
////                    NodeList nl2 = n1.getChildNodes();
////                    for( int i2 = 0; i2 < nl2.getLength(); i2++ ) {
////                        Node n2 = nl2.item( i2 );
////                        System.out.println("   |- n2 = " + n2.getNodeName());
////                        System.out.println("   |- v2 = " + n2.getNodeValue());
////
////                        NodeList nl3 = n2.getChildNodes();
////                        for( int i3 = 0; i3 < nl3.getLength(); i3++ ) {
////                            Node n3 = nl3.item( i3 );
////                            System.out.println("      |- n3 = " + n3.getNodeName());
////                            System.out.println("      |- v3 = " + n3.getNodeValue());
////                        }
////                    }
////                }
////            }
