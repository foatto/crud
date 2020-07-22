//package foatto.mms.core_mms.vc;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.CommonFunction;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.service.CoreServiceWorker;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.nio.ByteOrder;
//import java.nio.channels.SocketChannel;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.TreeMap;
//import java.util.TreeSet;
//
//public abstract class CoreImageSender extends CoreServiceWorker {
//
//    private static final String CONFIG_SERVER_IP_ = "server_ip_";
//    private static final String CONFIG_SERVER_PORT_ = "server_port_";
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
//    public static final String CONFIG_CAMERA_DESCR_ = "camera_descr_";
//    private static final String CONFIG_CAMERA_LOGIN_ = "camera_login_";
//    private static final String CONFIG_CAMERA_PASSWORD_ = "camera_password_";
//    private static final String CONFIG_CAMERA_URL_IMAGE_ = "camera_url_image_";
//    private static final String CONFIG_CAMERA_URL_VIDEO_ = "camera_url_video_";
//
//    private static final long CONFIG_RELOAD_INTERVAL = 15 * 60 * 1000L;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private String ffmpegPath = null;
//
//    private ArrayList<String> alServerIP = new ArrayList<>();
//    private ArrayList<Integer> alServerPort = new ArrayList<>();
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
//    private HashMap<String,String> hmObjectInfo = null;
//    private TreeMap<String,CameraInfo> tmCameraInfo = new TreeMap<>();
//
//    private SocketChannel socketChannel = null;
//
//    private int serverNum = 0;                    // номер текущего сервера
//
//    private AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 512 * 1024 );  // 4 картинки по 100 кб обычно
//
//    private TreeSet<String> tsDescr = new TreeSet<>();
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected CoreImageSender( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
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
//    }
//
//    protected boolean isRunOnce() { return false; }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected void cycle() throws Throwable {
//        //--- периодически перезагружаем/обновляем определения камер и информацию по объекту
//        reloadCameraConfig();
//
//        //--- переоткрыть соединение при необходимости
//        //--- там же отправится инфа по объекту и камерам
//        reconnectToServer();
//
//        //--- в виде ответа всегда получаем запрос на снимки
//        //--- сначала считаем переменный размер запроса
//        AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.BIG_ENDIAN, 4,
//                                            new StringBuilder( "Connection lost to VideoServer = " ).append( alServerIP.get( serverNum ) )
//                                                      .append( " : " ).append( alServerPort.get( serverNum ) ) );
//        int dataSize = bbIn.getInt();
//        //--- затем сам список камер
//        CommonFunction.readChannelToBuffer( socketChannel, bbIn, ByteOrder.BIG_ENDIAN, dataSize,
//                                            new StringBuilder( "Connection lost to VideoServer = " ).append( alServerIP.get( serverNum ) )
//                                                      .append( " : " ).append( alServerPort.get( serverNum ) ) );
//
//        //--- если запросов на снимки не будет
//        tsDescr.clear();
//        int descrCount = bbIn.getShort();
//        if( descrCount > 0 ) {
//            for( int i = 0; i < descrCount; i++ ) {
//                String descr = bbIn.getShortString();
//                tsDescr.add( descr );
//                CameraInfo ci = tmCameraInfo.get( descr );
//                //--- предварительно удаляем файл, возможно оставшийся от предыдущей итерации
//                ci.file.delete();
//                VideoFunction.getImageFromCamera( ffmpegPath, ci.urlVideo, ci.urlImage, ci.login, ci.itPassword, 0, 0, ci.file );
//            }
//        }
//        //--- снимки не запрашивались, сделаем паузу перед отправкой пустого результата
//        else Thread.sleep( 1000 );
//
//        //--- начинаем готовить данные к отправке
//        bbOut.clear();
//
//        //--- camera shots ---
//
//        dataSize = 1;
//        for( String descr : tsDescr ) {
//            File file = tmCameraInfo.get( descr ).file;
//            dataSize += 4 + ( file.exists() ? (int) file.length() /*+ 2 + 2*/ : 0 );
//        }
//
//        bbOut.putInt( dataSize );
//
//        bbOut.putByte( VideoServer.TAG_CAMERA_SHOT ); // tag
//        for( String descr : tsDescr ) {
//            File file = tmCameraInfo.get( descr ).file;
//            if( file.exists() ) {
//                CommonFunction.readFileToBuffer( file, bbOut, true );
//                file.delete();
//            }
//            //--- если снимок просили, но его нет - возвращаем 0
//            else bbOut.putInt( 0 );
//        }
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() )
//            socketChannel.write( bbOut.getBuffer() );
//    }
//
//    protected void reloadCameraConfig() {
//        //--- периодически перезагружаем/обновляем определения камер и информацию по объекту
//        if( System.currentTimeMillis() - lastConfigReloadTime > CONFIG_RELOAD_INTERVAL ) {
//            tmCameraInfo.clear();
//
//            if( cameraInfoFileName == null ) {
//                CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery(
//                    " SELECT descr , login , pwd , url_image , url_0 FROM VC_camera WHERE id <> 0 " );
//                while( rs.next() ) {
//                    String descr = rs.getString( 1 );
//                    tmCameraInfo.put( descr, new CameraInfo( tempDirName, descr,
//                                                        rs.getString( 2 ), rs.getString( 3 ), rs.getString( 4 ), rs.getString( 5 ) ) );
//                }
//                rs.close();
//            }
//            else {
//                HashMap<String,String> hmCameraInfo = CommonFunction.loadConfig( cameraInfoFileName );
//                int index = 0;
//                while( true ) {
//                    String descr = hmCameraInfo.get( CONFIG_CAMERA_DESCR_ + index );
//                    if( descr == null ) break;
//
//                    tmCameraInfo.put( descr, new CameraInfo( tempDirName, descr, hmCameraInfo.get( CONFIG_CAMERA_LOGIN_ + index ),
//                                                                                 hmCameraInfo.get( CONFIG_CAMERA_PASSWORD_ + index ),
//                                                                                 hmCameraInfo.get( CONFIG_CAMERA_URL_IMAGE_ + index ),
//                                                                                 hmCameraInfo.get( CONFIG_CAMERA_URL_VIDEO_ + index ) ) );
//                    index++;
//                }
//            }
//
//            //--- загрузим информацию по объекту
//            hmObjectInfo = CommonFunction.loadConfig( objectInfoFileName );
//            //--- дополним инфой по камерам, чтобы сервер мог автоматически создать определения камер
//            int index = 0;
//            for( String descr : tmCameraInfo.keySet() ) {
//                hmObjectInfo.put( CONFIG_CAMERA_DESCR_ + index, descr );
//                index++;
//            }
//
//            lastConfigReloadTime = System.currentTimeMillis();
//        }
//    }
//
//    private void reconnectToServer() throws Throwable {
//        //--- переоткрыть соединение при необходимости
//        //--- перебор серверов
//        int lastServerNum = serverNum;
//        while( true ) {
//            try {
//                //--- переоткрываем соединение при необходимости
//                if( socketChannel == null || ! socketChannel.isOpen() ) {
//                    socketChannel = SocketChannel.open();
//                    socketChannel.configureBlocking( true );
//                }
//                //--- после обрыва связи канал может остаться открытым, но не подключенным
//                if( ! socketChannel.isConnected() ) {
//                    socketChannel.connect( new InetSocketAddress( alServerIP.get( serverNum ), alServerPort.get( serverNum ) ) );
//                    sendHeader();
//                }
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
//                //--- переключимся на другой сервер/порт
//                serverNum = ( serverNum + 1 ) % alServerIP.size();
//                //--- если мы перебрали все адреса -
//                //--- выкидываем ошибку дальше, поскольку возможные сервера закончились
//                if( serverNum == lastServerNum ) throw ioe;
//            }
//        }
//    }
//
//    //--- отправка стартового заголовка при подсоединении к серверу
//    private void sendHeader() throws Throwable {
//        bbOut.clear();
//
//        //--- магический заголовок и версия протокола
//        bbOut.putInt( VideoServer.MAGIC_WORD );
//        bbOut.putByte( 0 );     // protocol version
//
//        //--- object & camera info ---
//
//        int dataSize = 1 + 2;   // tag + hmObjectInfo.size()
//        for( String key : hmObjectInfo.keySet() ) {
//            dataSize += 2 + key.length() * 2;
//            dataSize += 2 + hmObjectInfo.get( key ).length() * 2;
//        }
//
//        bbOut.putInt( dataSize );
//
//        bbOut.putByte( VideoServer.TAG_OBJECT_AND_CAMERA_INFO );
//        bbOut.putShort( hmObjectInfo.size() );
//        for( String key : hmObjectInfo.keySet() ) {
//            bbOut.putShortString( key );
//            bbOut.putShortString( hmObjectInfo.get( key ) );
//        }
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() )
//            socketChannel.write( bbOut.getBuffer() );
//    }
//
//    private static class CameraInfo {
//        public String descr = null;
//
//        public String login = null;
//        public String itPassword = null;
//        public String urlImage = null;
//        public String urlVideo = null;
//
//        public File file = null;
//
//        CameraInfo( String aTempDirName, String aDescr, String aLogin, String aPassword, String aUrlImage, String aUrlVideo ) {
//            descr = aDescr;
//            login = aLogin;
//            itPassword = aPassword;
//            urlImage = aUrlImage;
//            urlVideo = aUrlVideo;
//
//            file = new File( aTempDirName, new StringBuilder( "image_sender_" ).append( descr ).append( ".jpg" ).toString() );
//        }
//    }
//}
