//package foatto.mms;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.StringFunction;
//import foatto.core_server.service.CoreServiceWorker;
//import foatto.mms.core_mms.sensor.SensorConfig;
//import foatto.sql.AdvancedConnection;
//import org.w3c.dom.NodeList;
//import org.w3c.dom.Node;
//
//import javax.xml.soap.*;
//import javax.xml.transform.Source;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.stream.StreamResult;
//import java.io.File;
//import java.net.URL;
//import java.util.*;
//
//public class WaybillImport1C81 extends CoreServiceWorker {
//
//    private static final String ENGINE_DESCR = "Двигатель";
//    private static final String EQUIP_DESCR = "Верхнее оборудование";
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private static final String WAYBILL_NO = "m:WayListNumber";
//    private static final String WAYBILL_BEG_DOC = "m:DateStart";
//    private static final String WAYBILL_END_DOC = "m:DateEnd";
//    private static final String WAYBILL_BEG_FACT = "m:DateStartFact";
//    private static final String WAYBILL_END_FACT = "m:DateEndFact";
//    private static final String WAYBILL_RUN = "m:Running";
//    private static final String WAYBILL_ENGINE = "m:HourEquipment";
//    private static final String WAYBILL_EQUIP = "m:HourTopEquipment";
//    private static final String GOS_NO = "m:CarGosNo";
//    private static final String DRIVER_TAB = "m:DriverTab";
//    private static final String DRIVER_FIO = "m:DriverName";
//    private static final String FUEL_DATA = "m:FuelData";
//
//    private static final String FUEL_NAME = "m:FuelName";
//    private static final String FUEL_VALUE = "m:FuelValue";
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private static TimeZone timeZone = TimeZone.getDefault();
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private static final String CONFIG_WAYBILL_ACTUAL_PERIOD = "waybill_actual_period";
//    private static final String CONFIG_SOAP_URL = "soap_url";
//
//    private long waybillActualPeriod = 0;
//    private String soapURL = null;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public static void main( String[] args ) {
//        int exitCode = 0;   // нормальный выход с прерыванием цикла запусков
//        try {
//            serviceWorkerName = "WaybillImport1C81";
//            if( args.length == 1 ) {
//                new WaybillImport1C81( args[ 0 ] ).run();
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
//    public WaybillImport1C81( String aConfigFileName ) {
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
//        waybillActualPeriod = Long.parseLong( hmConfig.get( CONFIG_WAYBILL_ACTUAL_PERIOD ) ) * 31 * 24 * 60 * 60 * 1000;
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
//        long timeEnd = System.currentTimeMillis();
//        long timeBeg = timeEnd - waybillActualPeriod;
//
//long begTime = System.currentTimeMillis();
//long bt = System.currentTimeMillis();
//
//        //--- загрузка списка а/м
//        ArrayList<String> alObjectName = new ArrayList<>();
//        ArrayList<Integer> alObjectID = new ArrayList<>();
//        ArrayList<Integer> alUserID = new ArrayList<>();
//        HashMap<String,Integer> hmWaybillID = new HashMap<>();
//        HashMap<String,Integer> hmWorkerID = new HashMap<>();
//
//        //--- на импорт путевок по каждому а/м будет уходить примерно минута,
//        //--- итого на 500 а/м - около 10 часов.
//        //--- при каждой критической ошибке испорта список а/м будет обрабатываться по новой в том же порядке.
//        //--- при частых ошибках приведет это к тому, что последние в списке а/м никогда не обработаются.
//        //--- во избежание этого будем грузить список а/м каждый раз в разном порядке
//        String[] arrOrderBy = { " name ASC ", " name DESC ", " id ASC ", " id DESC ", " user_id ASC ", " user_id DESC " };
//
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT name , id , user_id FROM MMS_object WHERE id <> 0 " )
//            .append( " ORDER BY " ).append( arrOrderBy[ (int) ( Math.random() * arrOrderBy.length ) ] ) );
//        while( rs.next() ) {
//            alObjectName.add( rs.getString( 1 ) );   // специально для сохранения порядка сортировки
//            alObjectID.add( rs.getInt( 2 ) );
//            alUserID.add( rs.getInt( 3 ) );
//        }
//        rs.close();
//
//        rs = alStm.get( 0 ).executeQuery( " SELECT shift_no , id FROM MMS_work_shift " );
//        while( rs.next() ) hmWaybillID.put( rs.getString( 1 ), rs.getInt( 2 ) );
//        rs.close();
//
//        rs = alStm.get( 0 ).executeQuery( " SELECT tab_no , id FROM MMS_worker " );
//        while( rs.next() ) hmWorkerID.put( rs.getString( 1 ), rs.getInt( 2 ) );
//        rs.close();
//
//AdvancedLogger.info( "TIME: load auto & fuel & driver data = " + ( System.currentTimeMillis() - bt ) );
//bt = System.currentTimeMillis();
//
//        int allStoredWaybillCount = 0;
//        for( int ai = 0; ai < alObjectName.size(); ai++ ) {
//            String gosNo = alObjectName.get( ai );
//            int autoID = alObjectID.get( ai );
//            int userID = alUserID.get( ai );
//
//            //--- создаем соединение
//            SOAPConnectionFactory scf = SOAPConnectionFactory.newInstance();
//            SOAPConnection connection = scf.createConnection();
//
//            SOAPFactory sf = SOAPFactory.newInstance();
//
//            //--- создаем сообщение
//            MessageFactory mf = MessageFactory.newInstance();
//            SOAPMessage message = mf.createMessage();
//
//            //--- создаем объекты сообщения
//            SOAPPart soapPart = message.getSOAPPart();
//            SOAPEnvelope envelope = soapPart.getEnvelope();
//            //--- за ненадобностью пропущен SOAPHeader header = envelope.fillHeader();
//            SOAPBody body = envelope.getBody();
//
//            //--- заполняем тело сообщения
//            SOAPBodyElement bodyElement = body.addBodyElement( sf.createName( "GetDate", null, "http://www.foat.good" ) );
//
//            SOAPElement element = bodyElement.addChildElement( sf.createName( "DateStart" ) );
//            element.addTextNode( StringFunction.DateTime_YMDHMS( timeZone, timeBeg ).toString().replace( '.', '-' ) );
//
//            element = bodyElement.addChildElement( sf.createName( "DateEnd" ) );
//            element.addTextNode( StringFunction.DateTime_YMDHMS( timeZone, timeEnd ).toString().replace( '.', '-' ) );
//
//            element = bodyElement.addChildElement( sf.createName( "CarGosNo" ) );
//            element.addTextNode( gosNo );
//
//            if( AdvancedLogger.isDebugEnabled() ) {
//                //--- в отладочных целях: показать XML-исходник сообщения
//                System.out.println( "SOAP Request Sent:" );
//                message.writeTo( System.out );
//            }
//
//            //--- отправка сообщения и получение ответа
//            SOAPMessage response = connection.call( message, new URL( soapURL ) );
//            //--- закрыть соединение после получения ответа
//            connection.close();
//
//AdvancedLogger.info( new StringBuilder( "TIME: SOAP query & response for " ).append( gosNo ).append( " = " )
//               .append( System.currentTimeMillis() - bt ).toString() );
//bt = System.currentTimeMillis();
//
//            if( AdvancedLogger.isDebugEnabled() ) {
//                System.out.println( "SOAP Response Received:" );
//                //--- создаем транформер
//                TransformerFactory tf = TransformerFactory.newInstance();
//                Transformer transformer = tf.newTransformer();
//                //--- содержимое ответа
//                Source content = response.getSOAPPart().getContent();
//                //--- вывалить в файл
//                StreamResult result = new StreamResult( new File( "soaptest.xml" ) );
//                transformer.transform( content, result );
//            }
//
//            SOAPBody responseBody = response.getSOAPBody();
//
//            ArrayList<WaybillImport1C81Data> alWaybillData = new ArrayList<>();
//
//            NodeList waybillList = responseBody.getElementsByTagName( "m:Elements" );
//            for( int wi = 0; wi < waybillList.getLength(); wi++ ) {
//                WaybillImport1C81Data wid = new WaybillImport1C81Data();
//
//                NodeList waybillAttrList = waybillList.item( wi ).getChildNodes();
//                for( int wai = 0; wai < waybillAttrList.getLength(); wai++ ) {
//                    Node waybillAttr = waybillAttrList.item( wai );
//                    String nodeName = waybillAttr.getNodeName();
//
//                    if( nodeName.equals( WAYBILL_NO ) || nodeName.equals( WAYBILL_RUN ) ||
//                        nodeName.equals( WAYBILL_ENGINE ) || nodeName.equals( WAYBILL_EQUIP ) ||
//                        nodeName.equals( WAYBILL_BEG_DOC ) || nodeName.equals( WAYBILL_END_DOC ) ||
//                        nodeName.equals( WAYBILL_BEG_FACT ) || nodeName.equals( WAYBILL_END_FACT ) ||
//                        nodeName.equals( GOS_NO ) ||
//                        nodeName.equals( DRIVER_TAB ) || nodeName.equals( DRIVER_FIO ) ) {
//
//                        NodeList waybillAttrValue = waybillAttr.getChildNodes();
//                        if( waybillAttrValue.getLength() == 1 ) wid.hmData.put( nodeName, waybillAttrValue.item( 0 ).getNodeValue() );
//                    }
//                    else if( nodeName.equals( FUEL_DATA ) ) {
//                        String fuelName = null;
//                        String fuelValue = null;
//                        NodeList fuelData = waybillAttr.getChildNodes();
//                        for( int fdi = 0; fdi < fuelData.getLength(); fdi++ ) {
//                            Node fuelAttr = fuelData.item( fdi );
//                            String fuelAttrName = fuelAttr.getNodeName();
//
//                            if( fuelAttrName.equals( FUEL_NAME ) ) {
//                                NodeList fuelAttrData = fuelAttr.getChildNodes();
//                                if( fuelAttrData.getLength() == 1 ) fuelName = fuelAttrData.item( 0 ).getNodeValue();
//                            }
//                            else if( fuelAttrName.equals( FUEL_VALUE ) ) {
//                                NodeList fuelAttrData = fuelAttr.getChildNodes();
//                                if( fuelAttrData.getLength() == 1 ) fuelValue = fuelAttrData.item( 0 ).getNodeValue();
//                            }
//                        }
//                        if( fuelName != null && fuelValue != null ) wid.hmFuel.put( fuelName, fuelValue );
//                    }
//                }
//                alWaybillData.add( wid );
//            }
//
//            if( AdvancedLogger.isDebugEnabled() ) {
//                AdvancedLogger.debug( new StringBuilder( "Waybill parsing result for " ).append( gosNo ).append( ": " ).toString() );
//                for( int i = 0; i < alWaybillData.size(); i++ ) {
//                    WaybillImport1C81Data wid = alWaybillData.get( i );
//                    AdvancedLogger.debug( "-------------------- " + i );
//                    for( String name : wid.hmData.keySet() )
//                        AdvancedLogger.debug( name + " = " + wid.hmData.get( name ) );
//                    for( String name : wid.hmFuel.keySet() )
//                        AdvancedLogger.debug( name + " = " + wid.hmFuel.get( name ) );
//                }
//            }
//AdvancedLogger.info( new StringBuilder( "TIME: SOAP response parsing for " ).append( gosNo ).append( " = " )
//               .append( System.currentTimeMillis() - bt ).toString() );
//AdvancedLogger.info( new StringBuilder( "COUNT: SOAP parsed waybills for " ).append( gosNo ).append( " = " )
//               .append( alWaybillData.size() ).toString() );
//bt = System.currentTimeMillis();
//
//            //--- основная часть - по каждой путевке
//            int storedWaybillCount = 0;
//            for( WaybillImport1C81Data wid : alWaybillData ) {
//                String curGosNo = wid.hmData.get( GOS_NO );
//                if( curGosNo == null ) {
//                    AdvancedLogger.debug( new StringBuilder( "Wrong response gos_no = NULL " )
//                                           .append( " for query gos_no = " ).append( gosNo ).toString() );
//                    continue;
//                }
//                curGosNo = curGosNo.trim();
//                //--- проверка на случай, если в ответе будут данные по чужим а/м
//                if( ! curGosNo.equals( gosNo ) ) {
//                    AdvancedLogger.debug( new StringBuilder( "Wrong response gos_no = " ).append( curGosNo )
//                                           .append( " for query gos_no = " ).append( gosNo ).toString() );
//                    continue;
//                }
//
//                String waybillNo = wid.hmData.get( WAYBILL_NO );
//                if( waybillNo == null ) {
//                    AdvancedLogger.debug( new StringBuilder( "No waybill_no for gos_no = " ).append( gosNo ).toString() );
//                    continue;
//                }
//                waybillNo = StringFunction.prepareForSQL( waybillNo );
//
//                Integer waybillID = hmWaybillID.get( waybillNo );
//                String sWaybillBegDoc = wid.hmData.get( WAYBILL_BEG_DOC );
//                String sWaybillEndDoc = wid.hmData.get( WAYBILL_END_DOC );
//                String sWaybillBegFact = wid.hmData.get( WAYBILL_BEG_FACT );
//                String sWaybillEndFact = wid.hmData.get( WAYBILL_END_FACT );
//AdvancedLogger.debug( "waybillNo = " + waybillNo );
//AdvancedLogger.debug( "WAYBILL_BEG_DOC = " + sWaybillBegDoc );
//AdvancedLogger.debug( "WAYBILL_END_DOC = " + sWaybillEndDoc );
//AdvancedLogger.debug( "WAYBILL_BEG_FACT = " + sWaybillBegFact );
//AdvancedLogger.debug( "WAYBILL_END_FACT = " + sWaybillEndFact );
//                int waybillBegDoc = getTimeFromString( sWaybillBegDoc );
//                int waybillEndDoc = getTimeFromString( sWaybillEndDoc );
//                int waybillBegFact = getTimeFromString( sWaybillBegFact );
//                int waybillEndFact = getTimeFromString( sWaybillEndFact );
////                        if( waybillBeg == null || waybillEnd == null ) {
////                            wiLog.debug( new StringBuilder( "No beg_time or end_time for waybill = " ).append( waybillNo )
////                                                   .append( " and gos_no = " ).append( gosNo ).toString() );
////                            continue;
////                        }
//                boolean isDeleteWaybill = ( waybillBegDoc == 0 || waybillEndDoc == 0 || waybillBegFact == 0 || waybillEndFact == 0 );
//
//                //--- предыдущие данные по путевке в любом случае удаляем и (возможно) передобавляем
//                if( waybillID != null )
//                    alStm.get( 0 ).executeUpdate( new StringBuilder(
//                        " DELETE FROM MMS_work_shift_data WHERE shift_id = " ).append( waybillID ) );
//
//                if( isDeleteWaybill ) {
//                    if( waybillID != null ) {
////                        alStm.get( 0 ).executeUpdate( new StringBuilder(
////                                " DELETE FROM PLA_waybill_route WHERE waybill_id = " ).append( waybillID ) );
//                        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " DELETE FROM MMS_work_shift WHERE id = " ).append( waybillID ) );
//                    }
//                }
//                else {
//                    double waybillRun = 0;
//                    double waybillEngine = 0;
//                    double waybillEquip = 0;
//                    Integer driverID = 0;
//
//                    try { waybillRun = Double.parseDouble( wid.hmData.get( WAYBILL_RUN ).replace( ',', '.' ) ); } catch( Throwable t ) {}
//                    try { waybillEngine = Double.parseDouble( wid.hmData.get( WAYBILL_ENGINE ).replace( ',', '.' ) ); } catch( Throwable t ) {}
//                    try { waybillEquip = Double.parseDouble( wid.hmData.get( WAYBILL_EQUIP ).replace( ',', '.' ) ); } catch( Throwable t ) {}
//
//                    //--- водитель
//                    String driverTabNo = wid.hmData.get( DRIVER_TAB );
//                    driverTabNo = driverTabNo == null ? "-" : StringFunction.prepareForSQL( driverTabNo );
//                    if( driverTabNo != null ) {
//                        driverID = hmWorkerID.get( driverTabNo );
//                        String driverFIO = wid.hmData.get( DRIVER_FIO );
//                        driverFIO = driverFIO == null ? "-" : StringFunction.prepareForSQL( driverFIO );
//                        if( driverID == null ) {
//                            driverID = alStm.get( 0 ).getNextID( "MMS_worker", "id" );
//                            alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                    " INSERT INTO MMS_worker ( id , user_id , tab_no , name ) VALUES ( " )
//                                    .append( driverID ).append( " , " )
//                                    .append( userID ).append( " , '" )
//                                    .append( driverTabNo ).append( "' , '" )
//                                    .append( driverFIO ).append( "' ) " ) );
//
//                            hmWorkerID.put( driverTabNo, driverID );
//                        }
//                        else {
//                            alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                    " UPDATE MMS_worker SET user_id = " ).append( userID ).append( " , " )
//                                    .append( " name = '" ).append( driverFIO ).append( "' " )
//                                    .append( " WHERE id = " ).append( driverID ) );
//                        }
//                    }
//                    //--- путевка
//                    if( waybillID == null ) {
//                        waybillID = alStm.get( 0 ).getNextID( "MMS_work_shift", "id" );
//                        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " INSERT INTO MMS_work_shift ( id , user_id , object_id , worker_id , shift_no , " )
//                                .append( " beg_dt , end_dt , beg_dt_fact , end_dt_fact , run ) VALUES ( " )
//                                .append( waybillID ).append( " , " ).append( userID ).append( " , " ).append( autoID ).append( " , " )
//                                .append( driverID ).append( " , '" ).append( waybillNo ).append( "' , " )
//                                .append( waybillBegDoc ).append( " , " ).append( waybillEndDoc ).append( " , " )
//                                .append( waybillBegFact ).append( " , " ).append( waybillEndFact ).append( " , " )
//                                .append( waybillRun ).append( " ) " ) );
//                    }
//                    else {
//                        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " UPDATE MMS_work_shift SET user_id = " ).append( userID ).append( " , " )
//                                .append( " object_id = " ).append( autoID ).append( " , " )
//                                .append( " worker_id = " ).append( driverID ).append( " , " )
//                                .append( " shift_no = '" ).append( waybillNo ).append( "' , " )
//                                .append( " beg_dt = " ).append( waybillBegDoc ).append( " , " )
//                                .append( " end_dt = " ).append( waybillEndDoc ).append( " , " )
//                                .append( " beg_dt_fact = " ).append( waybillBegFact ).append( " , " )
//                                .append( " end_dt_fact = " ).append( waybillEndFact ).append( " , " )
//                                .append( " run = " ).append( waybillRun )
//                                .append( " WHERE id = " ).append( waybillID ) );
//                    }
//                    //--- работа оборудования
//                    if( waybillEngine != 0 )
//                        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                            " INSERT INTO MMS_work_shift_data ( id , shift_id , data_type , " )
//                            .append( " name , descr , data_value ) VALUES ( " )
//                            .append( alStm.get( 0 ).getNextID( "MMS_work_shift_data", "id" ) ).append( " , " )
//                            .append( waybillID ).append( " , " )
//                            .append( SensorConfig.SENSOR_WORK ).append( " , '' , '" )
//                            .append( ENGINE_DESCR ).append( "' , " ).append( waybillEngine ).append( " ) " ) );
//                    if( waybillEquip != 0 )
//                        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                            " INSERT INTO MMS_work_shift_data ( id , shift_id , data_type , " )
//                            .append( " name , descr , data_value ) VALUES ( " )
//                            .append( alStm.get( 0 ).getNextID( "MMS_work_shift_data", "id" ) ).append( " , " )
//                            .append( waybillID ).append( " , " )
//                            .append( SensorConfig.SENSOR_WORK ).append( " , '' , '" )
//                            .append( EQUIP_DESCR ).append( "' , " ).append( waybillEquip ).append( " ) " ) );
//
//                    //--- топливо
//                    for( String fuelName : wid.hmFuel.keySet() ) {
//                        double fuelUsing = 0;
//                        try { fuelUsing = Double.parseDouble( wid.hmFuel.get( fuelName ).replace( ',', '.' ) ); } catch( Throwable t ) {}
//                        alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " INSERT INTO MMS_work_shift_data ( id , shift_id , data_type , " )
//                                .append( " name , descr , data_value ) VALUES ( " )
//                                .append( alStm.get( 0 ).getNextID( "MMS_work_shift_data", "id" ) ).append( " , " )
//                                .append( waybillID ).append( " , " )
//                                .append( SensorConfig.SENSOR_LIQUID_USING ).append( " , '' , '" )
//                                .append( fuelName ).append( "' , " ).append( fuelUsing ).append( " ) " ) );
//                    }
//                }
//
//                storedWaybillCount++;
//                allStoredWaybillCount++;
//            }
//            alConn.get( 0 ).commit();
//AdvancedLogger.info( new StringBuilder( "COUNT: waybill replication for " ).append( gosNo ).append( " = " )
//               .append( storedWaybillCount ).toString() );
//AdvancedLogger.info( new StringBuilder( "TIME: waybill replication for " ).append( gosNo ).append( " = " )
//               .append( System.currentTimeMillis() - bt ).toString() );
//AdvancedLogger.info( "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -" );
//        }
//        //--- заточено под PostgreSQL-диалект
//        if( allStoredWaybillCount > 0 ) {
//            alStm.get( 0 ).executeUpdate( " REINDEX TABLE MMS_work_shift " );
//            alConn.get( 0 ).commit();
//            alStm.get( 0 ).executeUpdate( " REINDEX TABLE MMS_work_shift_data " );
//            alConn.get( 0 ).commit();
////            DBFunction.executeUpdate( stmPM, new StringBuilder( " ALTER INDEX ALL ON PLA_auto_waybill REBUILD " ).toString() );
////            connPM.commit();
////            DBFunction.executeUpdate( stmPM, new StringBuilder( " ALTER INDEX ALL ON PLA_auto_waybill_fuel REBUILD " ).toString() );
////            connPM.commit();
////            DBFunction.executeUpdate( stmPM, new StringBuilder( " ALTER INDEX ALL ON PLA_waybill_route REBUILD " ).toString() );
////            connPM.commit();
//        }
//
//        //--- вывод статистики ---------------------------------------------------------------------------------
//
//        if( allStoredWaybillCount == 0 ) AdvancedLogger.info( "No waybill data." );
//        else {
//            //--- обработка одного события за цикл может оказаться слишком быстрой и время может оказаться == 0.
//            //--- для этого случая установим время == 1 сек.
//            long workTime = ( System.currentTimeMillis() - begTime ) / 1000;
//            if( workTime == 0 ) workTime = 1;
//
//            AdvancedLogger.info( new StringBuilder( "DATA: waybill count = " ).append( allStoredWaybillCount ).toString() );
//            AdvancedLogger.info( new StringBuilder( "DATA: time = " ).append( workTime ).append( " [sec]" ).toString() );
//            AdvancedLogger.info( new StringBuilder( "DATA: speed = " ).append( allStoredWaybillCount * 60L / workTime ).append( " [row/min]" ).toString() );
//        }
//        AdvancedLogger.info( "------------------------------------------------------------" );
//    }
//
//    private int getTimeFromString( String dt ) {
//        if( dt == null ) return 0;
//        //--- dt = 2015-09-07 07:00:00
//        //---      0123456789012345678
//        return (int) ( new GregorianCalendar( Integer.parseInt( dt.substring( 0, 4 ) ),
//                                              Integer.parseInt( dt.substring( 5, 7 ) ) - 1,
//                                              Integer.parseInt( dt.substring( 8, 10 ) ),
//                                              Integer.parseInt( dt.substring( 11, 13 ) ),
//                                              Integer.parseInt( dt.substring( 14, 16 ) ),
//                                              0 ).getTimeInMillis() / 1000 );
//    }
//
//    private static class WaybillImport1C81Data {
//        public HashMap<String,String> hmData = new HashMap<>();
//        public HashMap<String,String> hmFuel = new HashMap<>();
//    }
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
