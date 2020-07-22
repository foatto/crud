//package foatto.mms;
//
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.nio.channels.SelectionKey;
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.TimeZone;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.StringFunction;
//import foatto.core_server.app.server.HTTPServer;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//import foatto.mms.core_mms.ObjectConfig;
//import foatto.mms.core_mms.calc.ObjectCalc;
//
//public class ObjectCalcServer extends HTTPServer {
//
//    private static final String CONFIG_USER_ID = "user_id";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private static final String PARAM_OBJECT_NAME = "object_name";
//    private static final String PARAM_DT_1 = "dt1";
//    private static final String PARAM_DT_2 = "dt2";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private int userID = 0;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private TimeZone tz = TimeZone.getDefault();
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public void init( CoreDataServer aDataServer, SelectionKey aSelectionKey ) {
//        super.init( aDataServer, aSelectionKey );
//
//        userID = Integer.parseInt( dataServer.hmConfig.get( CONFIG_USER_ID ) );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected StringBuilder prepareQuery( CoreDataWorker dataWorker, HashMap<String,String> hmParam ) {
//        StringBuilder sbOut = new StringBuilder();
//
//        try {
//            String objectName = hmParam.get( PARAM_OBJECT_NAME );
//            int objectID = 0;
//
//            CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                " SELECT id FROM MMS_object WHERE name = '" ).append( objectName ).append( "' " ) );
//            if( rs.next() ) objectID = rs.getInt( 1 );
//            rs.close();
//
//            if( objectID == 0 ) {
//                sbOut.append( "error=Объект '" ).append( objectName ).append( "' не найден." );
//            }
//            else {
//                String dt1 = hmParam.get( PARAM_DT_1 );
//                String dt2 = hmParam.get( PARAM_DT_2 );
//
//                long begTime = new GregorianCalendar( Integer.parseInt( dt1.substring( 0, 4 ) ),
//                                                      Integer.parseInt( dt1.substring( 5, 7 ) ) - 1,
//                                                      Integer.parseInt( dt1.substring( 8, 10 ) ),
//                                                      Integer.parseInt( dt1.substring( 11, 13 ) ),
//                                                      Integer.parseInt( dt1.substring( 14, 16 ) ),
//                                                      Integer.parseInt( dt1.substring( 17, 19 ) ) ).getTimeInMillis();
//                long endTime = new GregorianCalendar( Integer.parseInt( dt2.substring( 0, 4 ) ),
//                                                      Integer.parseInt( dt2.substring( 5, 7 ) ) - 1,
//                                                      Integer.parseInt( dt2.substring( 8, 10 ) ),
//                                                      Integer.parseInt( dt2.substring( 11, 13 ) ),
//                                                      Integer.parseInt( dt2.substring( 14, 16 ) ),
//                                                      Integer.parseInt( dt2.substring( 17, 19 ) ) ).getTimeInMillis();
//
//                UserConfig userConfig = UserConfig.getConfig( dataWorker.alStm.get( 0 ), userID );
//                ObjectConfig objectConfig = ObjectConfig.getObjectConfig( dataWorker.alStm.get( 0 ), userConfig, objectID );
//                ObjectCalc objectCalc = ObjectCalc.calcObject( dataWorker.alStm, userConfig, objectConfig, begTime, endTime );
//
//                if( objectCalc.gcd != null ) {
//                    sbOut.append( "run=" ).append( objectCalc.gcd.run ).append( ";\r\n" );
//                    sbOut.append( "out_time=" ).append( StringFunction.DateTime_YMDHMS( tz, objectCalc.gcd.outTime ) ).append( ";\r\n" );
//                    sbOut.append( "in_time=" ).append( StringFunction.DateTime_YMDHMS( tz, objectCalc.gcd.inTime ) ).append( ";\r\n" );
//                    sbOut.append( "moving_time=" ).append( StringFunction.MillisInterval_SB( objectCalc.gcd.movingTime ) ).append( ";\r\n" );
//                    sbOut.append( "parking_count=" ).append( objectCalc.gcd.parkingCount ).append( ";\r\n" );
//                    sbOut.append( "parking_time=" ).append( StringFunction.MillisInterval_SB( objectCalc.gcd.parkingTime ) ).append( ";\r\n" );
//                }
//                int index = 0;
//                for( String descr : objectCalc.tmWorkCalc.keySet() ) {
//                    sbOut.append( "work_descr_" ).append( index ).append( '=' ).append( descr ).append( ";\r\n" );
//                    sbOut.append( "work_time_" ).append( index ).append( '=' ).append( StringFunction.MillisInterval_SB(
//                                                                            objectCalc.tmWorkCalc.get( descr ).onTime ) ).append( ";\r\n" );
//                    index++;
//                }
//                index = 0;
//                for( String descr : objectCalc.tmLiquidUsingCalc.keySet() ) {
//                    sbOut.append( "fuel_descr_" ).append( index ).append( '=' ).append( descr ).append( ";\r\n" );
//                    sbOut.append( "fuel_value_" ).append( index ).append( '=' ).append(
//                                                                    objectCalc.tmLiquidUsingCalc.get( descr ).usingTotal ).append( ";\r\n" );
//                    index++;
//                }
//            }
//        }
//        catch( Throwable t ) {
//            StringWriter sw = new StringWriter();
//            t.printStackTrace( new PrintWriter( sw ) );
//            sbOut.append( "error='" ).append( sw.toString() ).append( "';" );
//        }
//        return sbOut;
//    }
//
//}
