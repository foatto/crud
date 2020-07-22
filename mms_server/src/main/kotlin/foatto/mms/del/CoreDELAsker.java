//package foatto.mms.del;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.*;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.service.CoreServiceWorker;
//
//import java.io.BufferedWriter;
//import java.net.InetSocketAddress;
//import java.nio.ByteOrder;
//import java.nio.channels.SocketChannel;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.TimeZone;
//
//import static foatto.core.util.CommonFunction.*;
//
//public abstract class CoreDELAsker extends CoreServiceWorker {
//
//    public static final String CONFIG_STATIC_DEL = "static_del";
//
//    public static final String CONFIG_DEL_IP = "del_ip";
//    public static final String CONFIG_DEL_PORT = "del_port";
//
//    public static final String CONFIG_DEL_INFO_FILE = "del_info_file";
//    private static final String CONFIG_METADATA_FILE = VideoFunction.CONFIG_FFMPEG_METADATA_FILE;   //"metadata_file";
//    private static final String CONFIG_COMMAND_FILE = "command_file";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public static final ArrayList<String> alDelParamName = new ArrayList<>();
//    public static final ArrayList<String> alDelParamDescr = new ArrayList<>();
//
//    static  {
//        alDelParamName.add( "drilling_rig_no" );
//        alDelParamDescr.add( "Номер СПУ" );
//
//        alDelParamName.add( "section_no" );
//        alDelParamDescr.add( "Номер цеха" );
//
//        alDelParamName.add( "crew_no" );
//        alDelParamDescr.add( "Номер бригады" );
//
//        alDelParamName.add( "oil_field_no" );
//        alDelParamDescr.add( "Номер месторождения" );
//
//        alDelParamName.add( "well_cluster_no" );
//        alDelParamDescr.add( "Номер куста" );
//
//        alDelParamName.add( "oil_well_no" );
//        alDelParamDescr.add( "Номер скважины" );
//
//        alDelParamName.add( "work_code" );
//        alDelParamDescr.add( "Код работ" );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private String staticDel = null;
//    private String delIP = null;
//    private int delPort = 0;
//    private int timeSystem = VideoFunction.TIME_SYSTEM_LINUX_BUILDROOT;
//    private String delInfoFileName = null;
//    private String metaDataFileName = null;
//    private String commandFileName = null;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private SocketChannel socketChannel = null;
//    private TimeZone timeZone = StringFunction.getTimeZone( 0 );
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected CoreDELAsker( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        staticDel = hmConfig.get( CONFIG_STATIC_DEL );
//
//        if( staticDel == null || staticDel.isEmpty() ) {
//            delIP = hmConfig.get( CONFIG_DEL_IP );
//            delPort = Integer.parseInt( hmConfig.get( CONFIG_DEL_PORT ) );
//        }
//
//        timeSystem = Integer.parseInt( hmConfig.get( VideoFunction.CONFIG_TIME_SYSTEM ) );
//
//        delInfoFileName = hmConfig.get( CONFIG_DEL_INFO_FILE );
//        metaDataFileName = hmConfig.get( CONFIG_METADATA_FILE );
//        commandFileName = hmConfig.get( CONFIG_COMMAND_FILE );
//    }
//
//    protected boolean isRunOnce() { return true; }
//
//    protected void cycle() throws Throwable {
//
//        int delID = 0;
//        Object[] arrDelParams = null;
//
//        if( staticDel == null || staticDel.isEmpty() )
//            try {
//                socketChannel = SocketChannel.open();
//                socketChannel.configureBlocking( true );
//                socketChannel.connect( new InetSocketAddress( delIP, delPort ) );
//
//                delID = getDelID();
//                AdvancedLogger.info( "ДЭЛ ID = " + delID );
//
//                int[] arrDelTime = getDelTime();
//
//                if( arrDelTime[ 0 ] > 0 ) {
//                    int[] arrDT = StringFunction.DateTime_Arr( timeZone, arrDelTime[ 0 ] * 1000L );
//
//                    AdvancedLogger.debug( new StringBuilder( "ДЭЛ unix time = " ).append( StringFunction.DateTime_DMYHMS( arrDT ) ) );
//                    AdvancedLogger.debug( "ДЭЛ up time = " + StringFunction.MillisInterval_SB( arrDelTime[ 1 ] * 1000L ) );
//
//                    //--- камера на данный момент может быть недоступна, это не повод падать в обморок
//                    try {
//                        VideoFunction.setTime( alStm.get( 0 ), timeSystem, arrDT );
//                    }
//                    catch( Throwable t ) {
//                        AdvancedLogger.error( t );
//                    }
//                }
//                arrDelParams = getDelParams();
//            }
//            catch( Throwable t ) {
//                AdvancedLogger.error( t );
//            }
//            finally {
//                if( socketChannel != null )
//                    try {
//                        socketChannel.close();
//                    }
//                    catch( Throwable t ) {}
//                    finally {
//                        socketChannel = null;
//                    }
//            }
//        else delID = Integer.parseInt( staticDel );
//
//        //--- если по какой-либо причине ДЭЛ-id не взялся - оставляем предыдущий
//        if( delID <= 0 ) return;
//
//        //--- обновляем инфу по текущей ДЭЛке
//        HashMap<String,String> hmDelInfo = CommonFunction.loadConfig( delInfoFileName );
//
//        hmDelInfo.put( "del_id", Integer.toString( delID ) );
//
//        if( arrDelParams != null )
//            for( int i = 0; i < alDelParamName.size(); i++ )
//                hmDelInfo.put( alDelParamName.get( i ), arrDelParams[ i ].toString() );
//
//        //--- если задано соединение с базой данных, возьмём оттуда данные по камерам
//        if( ! alDBConfig.isEmpty() ) {
//            int cameraNo = 0;
//            CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//                         " SELECT descr , login , pwd , url_0 , url_1 , url_mjpeg , url_image " )
//                .append( " FROM VC_camera WHERE id <> 0 ORDER BY descr " ) );
//            while( rs.next() ) {
//                hmDelInfo.put( "camera_descr_" + cameraNo, rs.getString( 1 ) );
//                hmDelInfo.put( "camera_login_" + cameraNo, rs.getString( 2 ) );
//                hmDelInfo.put( "camera_password_" + cameraNo, rs.getString( 3 ) );
//                hmDelInfo.put( "camera_url_0_" + cameraNo, rs.getString( 4 ) );
//                hmDelInfo.put( "camera_url_1_" + cameraNo, rs.getString( 5 ) );
//                hmDelInfo.put( "camera_url_mjpeg_" + cameraNo, rs.getString( 6 ) );
//                hmDelInfo.put( "camera_url_image_" + cameraNo, rs.getString( 7 ) );
//                //--- я бы убрал, но бубенцов развоняется
//                hmDelInfo.put( "is_enabled_"+ cameraNo, "1" );
//
//                cameraNo++;
//            }
//            rs.close();
//
//            hmDelInfo.put( "camera_count", Integer.toString( cameraNo ) );
//        }
//        WebConfigurator.saveConfigForHuentsov( hmDelInfo, delInfoFileName );
//
//        //--- сохраняем метадату для записи в видео
//
//        BufferedWriter bwText = getFileWriter( metaDataFileName, "Cp1251", false );
//        bwText.write( ";FFMETADATA1" );
//        bwText.newLine();
//        bwText.write( "copyright=Pulsar Video Server" );
//        bwText.newLine();
//        bwText.write( "title=" );
//        bwText.write( Integer.toString( delID ) );
//        bwText.newLine();
////        if( arrDelParams != null ) {
////            bwText.write( "comment=" );
////            //--- символы =;#\ запрещены в метадате, поэтому в качестве разделителя используем наиболее редкоиспользуемый символ ~
////            //bwText.write( "~drilling_rig_no~" );
////            bwText.write( "`nНомер СПУ: " );
////            bwText.write( prepareForMetaData( arrDelParams[ 0 ].toString() ) );
////            //bwText.write( "~section_no~" );
////            bwText.write( "`nНомер цеха: " );
////            bwText.write( prepareForMetaData( arrDelParams[ 1 ].toString() ) );
////            //bwText.write( "~crew_no~" );
////            bwText.write( "`nНомер бригады или экипажа: " );
////            bwText.write( prepareForMetaData( arrDelParams[ 2 ].toString() ) );
////            //bwText.write( "~oil_field_no~" );
////            bwText.write( "`nНомер месторождения: " );
////            bwText.write( prepareForMetaData( arrDelParams[ 3 ].toString() ) );
////            //bwText.write( "~well_cluster_no~" );
////            bwText.write( "`nНомер куста: " );
////            bwText.write( prepareForMetaData( arrDelParams[ 4 ].toString() ) );
////            //bwText.write( "~oil_well_no~" );
////            bwText.write( "`nНомер скважины: " );
////            bwText.write( prepareForMetaData( arrDelParams[ 5 ].toString() ) );
////            //bwText.write( "~work_code~" );
////            bwText.write( "`nКод работ: " );
////            bwText.write( prepareForMetaData( arrDelParams[ 6 ].toString() ) );
////            bwText.newLine();
////        }
//        bwText.close();
//
//        //--- сохраняем текущий DEL_ID для дальнейшего использования в интеграционных скриптах
//
//        bwText = getFileWriter( commandFileName, "Cp1251", false );
//        bwText.write( timeSystem == VideoFunction.TIME_SYSTEM_WINDOWS ? "SET DEL_ID=" : "DEL_ID=" );
//        bwText.write( Integer.toString( delID ) );
//        bwText.newLine();
//        bwText.close();
//    }
//
////---------------------------------------------------------------------------------------------------------------------------------------
//
//    private int getDelID() throws Throwable {
//        AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 16, ByteOrder.LITTLE_ENDIAN );
//        bbOut.putByte( 0x05 );      // signature + answer == 0
//        bbOut.putByte( 0 );         // owner
//        bbOut.putByte( 0 );         // READ_ID
//        bbOut.putByte( 0 );         // ERR_SUCCESS
//        bbOut.putByte( 0 );         // packetID
//        bbOut.putShort( 0 );        // packetSize
//        bbOut.putShort( CRC.crc16_modbus( bbOut.array(), bbOut.arrayOffset(), 7, false ) );
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() ) socketChannel.write( bbOut.getBuffer() );
//
//        int returnSize = 7 + 10 + 2;    // заголовок + ответ + CRC
//        AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.LITTLE_ENDIAN, returnSize,
//                                    new StringBuilder( "Connection lost to DEL = " ).append( delIP ).append( " : " ).append( delPort ) );
//        bbIn.getByte();     // signature
//        bbIn.getByte();     // owner
//        bbIn.getByte();     // READ_ID
//        boolean isSuccess = bbIn.getByte() == 0;    // ERR_SUCCESS
//        bbIn.getByte();     // packetID
//        bbIn.getShort();    // packetSize
//
//        int delID = bbIn.getInt();
//        bbIn.getShort();    // deviceType
//        bbIn.getShort();    // version
//        bbIn.getShort();    // tableSize
//
//        int crcReal = bbIn.getShort() & 0xFFFF;    // CRC
//        int crcCalc = CRC.crc16_modbus( bbIn.array(), bbIn.arrayOffset(), 7 + 10, false );
//
//        //--- явно отобразим, что это ДЭЛка отказалась
//        return isSuccess && crcReal == crcCalc ? delID : -1;
//    }
//
//    private int[] getDelTime() throws Throwable {
//        AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 16, ByteOrder.LITTLE_ENDIAN );
//        bbOut.putByte( 0x05 );      // signature + answer == 0
//        bbOut.putByte( 0 );         // owner
//        bbOut.putByte( 56 );        // READ_TIME
//        bbOut.putByte( 0 );         // ERR_SUCCESS
//        bbOut.putByte( 0 );         // packetID
//        bbOut.putShort( 0 );        // packetSize
//        bbOut.putShort( (short) CRC.crc16_modbus( bbOut.array(), bbOut.arrayOffset(), 7, false ) );
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() ) socketChannel.write( bbOut.getBuffer() );
//
//        int returnSize = 7 + 8 + 2;    // заголовок + ответ + CRC
//        AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.LITTLE_ENDIAN, returnSize,
//                                    new StringBuilder( "Connection lost to DEL = " ).append( delIP ).append( " : " ).append( delPort ) );
//        bbIn.getByte();     // signature
//        bbIn.getByte();     // owner
//        bbIn.getByte();     // READ_TIME
//        boolean isSuccess = bbIn.getByte() == 0;    // ERR_SUCCESS
//        bbIn.getByte();     // packetID
//        bbIn.getShort();    // packetSize
//
//        int unixTime = bbIn.getInt();
//        int upTime = bbIn.getInt();
//
//        int crcReal = bbIn.getShort() & 0xFFFF;    // CRC
//        int crcCalc = CRC.crc16_modbus( bbIn.array(), bbIn.arrayOffset(), 7 + 8, false );
//
//        //--- явно отобразим, что это ДЭЛка отказалась
//        return new int[] { isSuccess && crcReal == crcCalc ? unixTime : -1, isSuccess ? upTime : -1 };
//    }
//
//    private Object[] getDelParams() throws Throwable {
//        AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 16, ByteOrder.LITTLE_ENDIAN );
//        bbOut.putByte( 0x05 );      // signature + answer == 0
//        bbOut.putByte( 0 );         // owner
//        bbOut.putByte( 47 );        // READ_BINDING_PARAMS
//        bbOut.putByte( 0 );         // ERR_SUCCESS
//        bbOut.putByte( 0 );         // packetID
//        bbOut.putShort( 0 );        // packetSize
//        bbOut.putShort( (short) CRC.crc16_modbus( bbOut.array(), bbOut.arrayOffset(), 7, false ) );
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() ) socketChannel.write( bbOut.getBuffer() );
//
//        int returnSize = 7 + 68 + 2;    // заголовок + ответ + CRC
//
//        AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.LITTLE_ENDIAN, returnSize,
//                                    new StringBuilder( "Connection lost to DEL = " ).append( delIP ).append( " : " ).append( delPort ) );
//        bbIn.getByte();     // signature
//        bbIn.getByte();     // owner
//        bbIn.getByte();     // READ_TIME
//        boolean isSuccess = bbIn.getByte() == 0;    // ERR_SUCCESS
//        bbIn.getByte();     // packetID
//        bbIn.getShort();    // packetSize
//
//        String drillingRigNo = readDelString( bbIn, 16 );   // номер СПУ, (ascii)
//        String sectionNo = readDelString( bbIn, 10 );       // номер цеха, (bin)
//        String crewNo = readDelString( bbIn, 10 );          // номер бригады/экипажа,(bin)
//        String oilFieldNo = readDelString( bbIn, 10 );      // номер месторождения, (bin)
//        String wellClusterNo = readDelString( bbIn, 10 );   // номер куста, (ascii)
//        String oilWellNo = readDelString( bbIn, 10 );       // номер скважины, (ascii)
//        int workCode = bbIn.getShort() & 0xffff;
//
//        int crcReal = bbIn.getShort() & 0xFFFF;    // CRC
//        int crcCalc = CRC.crc16_modbus( bbIn.array(), bbIn.arrayOffset(), 7 + 68, false );
//
//        //--- явно отобразим, что это ДЭЛка отказалась
//        return isSuccess && crcReal == crcCalc ? new Object[] { drillingRigNo, sectionNo, crewNo, oilFieldNo, wellClusterNo, oilWellNo, workCode }: null;
//    }
//
//    private String readDelString( AdvancedByteBuffer bbIn, int maxStringLen ) throws Throwable {
//        byte[] arrB1 = new byte[ maxStringLen ];
//        bbIn.get( arrB1 );
//
//        byte[] arrB2 = new byte[ maxStringLen ];
//        Arrays.fill( arrB2, (byte) 32 );
//
//        for( int i = 0; i < maxStringLen; i++ ) {
//            byte b = arrB1[ i ];
//            if( b == 0 ) break;
//            arrB2[ i ] = b;
//        }
//        return new String( arrB2, "Cp1251" ).trim();
//    }
//
////    private String prepareForMetaData( String sour ) {
////        return sour.replace( '=', '_' ).replace( ';', '_' ).replace( '#', '_' ).replace( '\\', '_' )
////                   .replace( '\r', '_' ).replace( '\n', '_' );
////    }
//}
