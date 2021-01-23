//package foatto.mms.del;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.CRC;
//import foatto.core.util.CommonFunction;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.service.CoreServiceWorker;
//
//import javax.imageio.ImageIO;
//import java.awt.geom.AffineTransform;
//import java.awt.image.AffineTransformOp;
//import java.awt.image.BufferedImage;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.nio.ByteOrder;
//import java.nio.channels.FileChannel;
//import java.nio.channels.SocketChannel;
//import java.util.ArrayList;
//import java.util.HashMap;
//
//public abstract class CoreImageDel extends CoreServiceWorker {
//
//    public static final String CONFIG_SERVER_IP_ = "server_ip_";
//    public static final String CONFIG_SERVER_PORT_ = "server_port_";
//
//    public static final String CONFIG_DEL_ROOT = "del_root";
//
////    private static final String CONFIG_PROXY_ADDR = "proxy_addr";
////    private static final String CONFIG_PROXY_PORT = "proxy_port";
////    private static final String CONFIG_PROXY_USER = "proxy_user";
////    private static final String CONFIG_PROXY_PASS = "proxy_pass";
//
//    private static final String CONFIG_CAMERA_INFO_FILE = "camera_info_file";
//    private static final String CONFIG_OBJECT_INFO_FILE = "object_info_file";
//
//    //--- параметры для задания конфигурации камер в виде текстового файла
//
//    private static final String CONFIG_CAMERA_COUNT = "camera_count";
//    private static final String CONFIG_CAMERA_DESCR_ = "camera_descr_";
//    private static final String CONFIG_CAMERA_LOGIN_ = "camera_login_";
//    private static final String CONFIG_CAMERA_PASSWORD_ = "camera_password_";
//    private static final String CONFIG_CAMERA_URL_IMAGE_ = "camera_url_image_";
//    private static final String CONFIG_CAMERA_URL_VIDEO_ = "camera_url_video_";
//
//    private static final long CONFIG_RELOAD_INTERVAL = 15 * 60 * 1000L;
//
//    private static final String CONFIG_SERVER_NO = "server_no";
//    private static final String CONFIG_CONNECT_TIME_OUT = "connect_time_out";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private String delRoot = null;
//
//    private String ffmpegPath = null;
//
//    private ArrayList<String> alServerIP = new ArrayList<>();
//    private ArrayList<Integer> alServerPort = new ArrayList<>();
//
//    private int serverNo = 0;
//
////    private boolean isWrapHTTP = false;
////    private String proxyAddr = null;
////    private String proxyPort = null;
////    private String proxyUser = null;
////    private String proxyPass = null;
//
//    private String cameraInfoFileName = null;
//    private String objectInfoFileName = null;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private long lastConfigReloadTime = 0;
//    private ArrayList<String> alCameraDescr = new ArrayList<>();
//    private ArrayList<String> alCameraLogin = new ArrayList<>();
//    private ArrayList<String> alCameraPassword = new ArrayList<>();
//    private ArrayList<String> alCameraURLImage = new ArrayList<>();
//    private ArrayList<String> alCameraURLVideo = new ArrayList<>();
//    private HashMap<String,String> hmObjectInfo = null;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private SocketChannel socketChannel = null;
//
//    private AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 512 * 1024 );  // 4 картинки по 100 кб примерно
//
//    private volatile int lastWorkTime = (int) ( System.currentTimeMillis() / 1000 );
//    private ArrayList<Integer> alCameraIndex = new ArrayList<>( 4 );
//    private ArrayList<File> alCameraFile = new ArrayList<>( 4 );
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected CoreImageDel( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        delRoot = hmConfig.get( CONFIG_DEL_ROOT );
//
//        ffmpegPath = hmConfig.get( VideoFunction.CONFIG_FFMPEG_PATH );
//
//        CommonFunction.loadLinkConfig( hmConfig, CONFIG_SERVER_IP_, CONFIG_SERVER_PORT_, alServerIP, alServerPort );
//
////        String wrapProtocol = hmConfig.get( CONFIG_WRAP_PROTOCOL );
////        if( wrapProtocol != null ) isWrapHTTP = wrapProtocol.toUpperCase().equals( "HTTP" );
////        //--- если включен протокол обёртки HTTP, то подгрузим значения прокси (если заданы)
////        if( isWrapHTTP ) {
////            proxyAddr = hmConfig.get( CONFIG_PROXY_ADDR );
////            proxyPort = hmConfig.get( CONFIG_PROXY_PORT );
////            proxyUser = hmConfig.get( CONFIG_PROXY_USER );
////            proxyPass = hmConfig.get( CONFIG_PROXY_PASS );
////        }
//
//        cameraInfoFileName = hmConfig.get( CONFIG_CAMERA_INFO_FILE );
//        objectInfoFileName = hmConfig.get( CONFIG_OBJECT_INFO_FILE );
//
//        serverNo = Integer.parseInt( hmConfig.get( CONFIG_SERVER_NO ) );
//
//        if( serverNo >= alServerIP.size() ) exitProcess( 0 );
//
//        final int connectTimeOut = Integer.parseInt( hmConfig.get( CONFIG_CONNECT_TIME_OUT ) );
//        new Thread( new Runnable() { public void run() {
//            while( true ) {
//                if( System.currentTimeMillis() / 1000 - lastWorkTime > connectTimeOut ) exitProcess( 1 );
//                try { Thread.sleep( connectTimeOut * 1000L ); } catch( InterruptedException ie ) {}
//            }
//        } } ).start();
//    }
//
//    protected boolean isRunOnce() { return false; }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected void cycle() throws Throwable {
//
//        //--- периодически перезагружаем/обновляем определения камер и информацию по объекту
//        reloadCameraConfig();
//
//        //--- переоткрыть соединение при необходимости
//        reconnectToToucanServer();
//
//        //--- прием команды (унаследовано как 3 байта, хотя для команды 0x03 get-info хорошо бы сразу 5 байт)
//        AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.BIG_ENDIAN, 3,
//                                                    new StringBuilder( "Connection lost to DEL = " ).append( alServerIP.get( serverNo ) )
//                                                              .append( " : " ).append( alServerPort.get( serverNo ) ) );
//        bbOut.clear();
//
//        switch( bbIn.getByte() ) {
//        //--- ping
//        case 0:
//            bbIn.getByte(); // SKIP
//            bbIn.getByte(); // SKIP
//            //--- nothing out
//            break;
//
//        //--- get object & camera info
//        case 1:
//            bbIn.getByte(); // SKIP
//            bbIn.getByte(); // SKIP
//
//            int dataSize = 4;
//            for( String key : hmObjectInfo.keySet() ) {
//                dataSize += 2 + key.length();
//                dataSize += 2 + hmObjectInfo.get( key ).length();
//            }
//
//            bbOut.putByte( 1 );
//            bbOut.putInt( dataSize );
//            bbOut.putInt( hmObjectInfo.size() );
//            for( String key : hmObjectInfo.keySet() ) {
//                bbOut.putShort( key.length() );
//                bbOut.put( key.getBytes( "Cp1251" ) );
//                bbOut.putShort( hmObjectInfo.get( key ).length() );
//                bbOut.put( hmObjectInfo.get( key ).getBytes( "Cp1251" ) );
//            }
//            break;
//
//        //--- get images
//        case 2:
//            int cameraNo = bbIn.getByte() & 0xFF;
//            int scaleDiv = Math.max( bbIn.getByte() & 0xFF, 1 );
//
//            alCameraIndex.clear();
//            if( cameraNo == -1 )
//                for( int cIndex = 0; cIndex < alCameraDescr.size(); cIndex++ )
//                    alCameraIndex.add( cIndex );
//            else {
//                cameraNo = Math.max( cameraNo, 0 );
//                cameraNo = Math.min( cameraNo, alCameraDescr.size() - 1 );
//                alCameraIndex.add( cameraNo );
//            }
//
//            alCameraFile.clear();
//            for( int cIndex : alCameraIndex )
//                alCameraFile.add( getCameraImage( cIndex, scaleDiv ) );
//
//            dataSize = 4;
//            for( File file : alCameraFile )
//                dataSize += 4 + 4 + ( file.exists() ? (int) file.length() : 0 );
//
//            bbOut.putByte( 2 );
//            bbOut.putInt( dataSize );
//            bbOut.putInt( alCameraFile.size() );
//            for( File file : alCameraFile ) {
//                if( file.exists() ) {
//                    //--- в процессе подготовки данных
//                    lastWorkTime = (int) ( System.currentTimeMillis() / 1000 );
//
//                    bbOut.putInt( (int) ( System.currentTimeMillis() / 1000 ) );
//                    CommonFunction.readFileToBuffer( file, bbOut, true );
//                    file.delete();
//                }
//                else {
//                    bbOut.putInt( 0 );
//                    bbOut.putInt( 0 );
//                }
//            }
//            break;
//
//        //--- set object info
//        case 3:
//            //--- дождаться всех 4 байт для полного int dataSize
//            CommonFunction.readChannelToBuffer( socketChannel, bbIn, ByteOrder.BIG_ENDIAN, 4,
//                                                    new StringBuilder( "Connection lost to DEL = " ).append( alServerIP.get( serverNo ) )
//                                                              .append( " : " ).append( alServerPort.get( serverNo ) ) );
//            dataSize = bbIn.getInt();
//            CommonFunction.readChannelToBuffer( socketChannel, bbIn, ByteOrder.BIG_ENDIAN, dataSize,
//                                                    new StringBuilder( "Connection lost to DEL = " ).append( alServerIP.get( serverNo ) )
//                                                              .append( " : " ).append( alServerPort.get( serverNo ) ) );
//            int infoCount = bbIn.getInt();
//            for( int i = 0; i < infoCount; i++ ) {
//                int arrLen = bbIn.getShort();
//                byte[] arrByte = new byte[ arrLen ];
//                bbIn.get( arrByte );
//                String key = new String( arrByte, "Cp1251" );
//
//                arrLen = bbIn.getShort();
//                arrByte = new byte[ arrLen ];
//                bbIn.get( arrByte );
//                String value = new String( arrByte, "Cp1251" );
//
//                hmObjectInfo.put( key, value );
//            }
//
//            WebConfigurator.saveConfigForHuentsov( hmObjectInfo, objectInfoFileName );
//
//            bbOut.putByte( 3 );
//            bbOut.putInt( 0 );
//            break;
//
////Лучше объединить команды и чтобы была возможность выдергивать по кускам.
////Ибо 20метров одним запросом не вытащить.
////
////Запрос:
////command tag = (выгрузка произвольного файла) =1 byte
////int offset = 4 bytes (смещение в файле)
////int packetsize = 4 bytes (максимальный размер куска данных)
////relative path len = 2 byte
////path = Cp1251 string
////
////
////Ответ:
////
////command tag = (выгрузка произвольного файла) =1 byte
////int offset = 4 bytes (смещение в файле)
////int packetsize = 4 bytes (размер куска данных, если packetsize
////запроса и ответа равны значит будет запрошен следующий кусок данных файла)
////data
////
////
////И очень желательно на пакеты иметь crc. при передаче через 3G\LTE
////TCP/IP совсем может и не работать. битое приходит с вырванными
////байтами.
//
//        //--- выгрузка произвольного файла
//        case 4:
//            //--- дождаться всех 6 байт для полного offset, size, path_len
//            CommonFunction.readChannelToBuffer( socketChannel, bbIn, ByteOrder.BIG_ENDIAN, 6,
//                                                    new StringBuilder( "Connection lost to DEL = " ).append( alServerIP.get( serverNo ) )
//                                                              .append( " : " ).append( alServerPort.get( serverNo ) ) );
//            int offset = bbIn.getInt();
//            int size = bbIn.getInt();
//
//            int arrLen = bbIn.getShort();
//            byte[] arrByte = new byte[ arrLen ];
//            bbIn.get( arrByte );
//            String path = new String( arrByte, "Cp1251" );
//
//            //--- простейшее экранирование
//            path = path.replace( "..", "" );    // сначала убираем двойные точки, после них могут остаться двойные слэши
//            path = path.replace( "//", "/" );   // потом сокращаем двойные слэши
//
//            File file = new File( delRoot, path );
//            bbOut.putByte( 4 );
//            if( file.exists() ) {
//                //--- size == 0, если размер файла заранее неизвестен или может динамически меняться
//                if( size == 0 ) size = (int) ( file.length() - offset );
//
//                bbOut.putInt( offset );
//                bbOut.putInt( size );
//
//                //--- перед чтением проверяем/расширяем буфер, т.к. fileChannel.read этого делать не будет
//                bbOut.checkSize( size );
//                FileInputStream fis = new FileInputStream( file );
//                fis.skip( offset );
//                for( int i = 0; i < size; i++ ) bbOut.putByte( fis.read() );
//                fis.close();
//
//                bbOut.putShort( (short) CRC.crc16_modbus( bbOut.array(), bbOut.arrayOffset(), 1 + 4 + 4 + size, true ) );
//            }
//            else {
//                bbOut.putInt( 0 );
//                bbOut.putInt( 0 );
//            }
//            break;
//        }
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() ) {
//            //--- в процессе отправки данных
//            lastWorkTime = (int) ( System.currentTimeMillis() / 1000 );
//            socketChannel.write( bbOut.getBuffer() );
//        }
//
//        //--- после отправки данных
//        lastWorkTime = (int) ( System.currentTimeMillis() / 1000 );
//    }
//
//    protected void reloadCameraConfig() {
//        //--- периодически перезагружаем/обновляем определения камер и информацию по объекту
//        if( System.currentTimeMillis() - lastConfigReloadTime > CONFIG_RELOAD_INTERVAL ) {
//            alCameraDescr.clear();
//            alCameraLogin.clear();
//            alCameraPassword.clear();
//            alCameraURLImage.clear();
//            alCameraURLVideo.clear();
//
//            if( cameraInfoFileName == null ) {
//                CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery(
//                    " SELECT descr , login , pwd , url_image , url_0 FROM VC_camera WHERE id <> 0 ORDER BY descr " );
//                while( rs.next() ) {
//                    alCameraDescr.add( rs.getString( 1 ) );
//                    alCameraLogin.add( rs.getString( 2 ) );
//                    alCameraPassword.add( rs.getString( 3 ) );
//                    alCameraURLImage.add( rs.getString( 4 ) );
//                    alCameraURLVideo.add( rs.getString( 5 ) );
//                }
//                rs.close();
//            }
//            else {
//                HashMap<String,String> hmCameraInfo = CommonFunction.loadConfig( cameraInfoFileName );
//                int cameraCount = Integer.parseInt( hmCameraInfo.get( CONFIG_CAMERA_COUNT ) );
//                for( int i = 0; i < cameraCount; i++ ) {
//                    alCameraDescr.add( hmCameraInfo.get( CONFIG_CAMERA_DESCR_ + i ) );
//                    alCameraLogin.add( hmCameraInfo.get( CONFIG_CAMERA_LOGIN_ + i ) );
//                    alCameraPassword.add( hmCameraInfo.get( CONFIG_CAMERA_PASSWORD_ + i ) );
//                    alCameraURLImage.add( hmCameraInfo.get( CONFIG_CAMERA_URL_IMAGE_ + i ) );
//                    alCameraURLVideo.add( hmCameraInfo.get( CONFIG_CAMERA_URL_VIDEO_ + i ) );
//                }
//            }
//
//            hmObjectInfo = CommonFunction.loadConfig( objectInfoFileName );
//
//            lastConfigReloadTime = System.currentTimeMillis();
//        }
//    }
//
//    private void reconnectToToucanServer() throws Throwable {
//        //--- переоткрыть соединение при необходимости
//        //--- перебор серверов
//        while( true ) {
//            try {
//                //--- переоткрываем соединение при необходимости
//                if( socketChannel == null || ! socketChannel.isOpen() ) {
//                    socketChannel = SocketChannel.open();
//                    socketChannel.configureBlocking( true );
//                }
//                //--- после обрыва связи канал может остаться открытым, но не подключенным
//                if( ! socketChannel.isConnected() )
//                    socketChannel.connect( new InetSocketAddress( alServerIP.get( serverNo ), alServerPort.get( serverNo ) ) );
//                break;
//            }
//            catch( IOException ioe ) {
//                if( socketChannel != null )
//                    try {
//                        socketChannel.close();
//                    }
//                    catch( Throwable t ) {}
//                    finally {
//                        socketChannel = null;
//                    }
//                throw ioe;
//            }
//        }
//    }
//
//    private File getCameraImage( int cameraIndex, int scaleDiv ) throws Throwable {
//        File imageFile = new File( tempDirName, new StringBuilder().append( CommonFunction.getRandomInt() ).append( ".jpg" ).toString() );
//
//        //--- берём картинку без пережатия, дожимаем в зависимости от заданного/переданного ДЭЛ-сервером scaleDiv
//        VideoFunction.getImageFromCamera( ffmpegPath, alCameraURLVideo.get( cameraIndex ), alCameraURLImage.get( cameraIndex ),
//                                     alCameraLogin.get( cameraIndex ), alCameraPassword.get( cameraIndex ), 0, 0, imageFile );
//
//        //--- есть необходимость пережимать картинки
//        if( scaleDiv > 1 && imageFile.exists() ) {
//            BufferedImage biSour = ImageIO.read( imageFile );
//            BufferedImage biDest = new BufferedImage( biSour.getWidth() / scaleDiv, biSour.getHeight() / scaleDiv,
//                                                      biSour.getType() );
//            AffineTransform at = new AffineTransform();
//            at.scale( 1.0 / scaleDiv, 1.0 / scaleDiv );
//            AffineTransformOp atOp = new AffineTransformOp( at, AffineTransformOp.TYPE_BICUBIC );
//            atOp.filter( biSour, biDest );
//            ImageIO.write( biDest, "jpg", imageFile );
//        }
//        return imageFile;
//    }
//}
