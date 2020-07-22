//package foatto.core_server.service;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.nio.ByteOrder;
//import java.nio.channels.SocketChannel;
//import java.util.ArrayList;
//
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.CommonFunction;
//
//public abstract class CoreCommander extends CoreServiceWorker {
//
//    private static final String CONFIG_SERVER_IP_ = "server_ip_";
//    private static final String CONFIG_SERVER_PORT_ = "server_port_";
//
////    private static final String CONFIG_PROXY_ADDR = "proxy_addr";
////    private static final String CONFIG_PROXY_PORT = "proxy_port";
////    private static final String CONFIG_PROXY_USER = "proxy_user";
////    private static final String CONFIG_PROXY_PASS = "proxy_pass";
//
//    private static final String CONFIG_SERIAL_NO = "serial_no";
//
//    private static final String CONFIG_MAGIC_WORD = "magic_word";
//    private static final String CONFIG_PROTOCOL_VERSION = "protocol_version";
//    private static final String CONFIG_TAG_COMMAND = "tag_command";
//
//    private static final String CONFIG_CYCLE_PAUSE = "cycle_pause";
//
////----------------------------------------------------------------------------------------------------------------------------------------
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
//    private int serialNo = 0;
//
//    private int magicWord = 0;
//    private int protocolVersion = 0;
//    private int tagCommand = 0;
//
//    private long cyclePause = 0;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private SocketChannel socketChannel = null;
//
//    private int serverNum = 0;                    // номер текущего сервера
//
//    private AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 512 * 1024 );  // 4 картинки по 100 кб обычно
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected CoreCommander( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        CommonFunction.loadLinkConfig( hmConfig, CONFIG_SERVER_IP_, CONFIG_SERVER_PORT_, alServerIP, alServerPort );
//
//        serialNo = Integer.parseInt( hmConfig.get( CONFIG_SERIAL_NO ) );
//
//        magicWord = Integer.parseInt( hmConfig.get( CONFIG_MAGIC_WORD ) );
//        protocolVersion = Integer.parseInt( hmConfig.get( CONFIG_PROTOCOL_VERSION ) );
//        tagCommand = Integer.parseInt( hmConfig.get( CONFIG_TAG_COMMAND ) );
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
//        cyclePause = Integer.parseInt( hmConfig.get( CONFIG_CYCLE_PAUSE ) );
//    }
//
//    protected boolean isRunOnce() { return false; }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected void cycle() throws Throwable {
//        //--- переоткрыть соединение при необходимости
//        //--- там же отправится инфа по объекту и камерам
//        reconnectToServer();
//
//        //--- в виде ответа всегда получаем команду ---
//
//        //--- сначала считаем переменный размер команды
//        AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.BIG_ENDIAN, 4,
//                                            new StringBuilder( "Connection lost to CommandServer = " ).append( alServerIP.get( serverNum ) )
//                                                      .append( " : " ).append( alServerPort.get( serverNum ) ) );
//        int dataSize = bbIn.getInt();
//
//        if( dataSize > 0 ) {
//            //--- затем саму команду
//            CommonFunction.readChannelToBuffer( socketChannel, bbIn, ByteOrder.BIG_ENDIAN, dataSize,
//                                                new StringBuilder( "Connection lost to CommandServer = " ).append( alServerIP.get( serverNum ) )
//                                                          .append( " : " ).append( alServerPort.get( serverNum ) ) );
//
//            String cmd = bbIn.getShortString();
////AdvancedLogger.debug( "cmd = " + cmd );
//            CommonFunction.runCommand( null, StringFunction.getStringArray( cmd, " " ) );
//        }
//        //--- команды не было - держим паузу
//        else Thread.sleep( cyclePause );
//
//        //--- снова отправляем запрос на команду
//        bbOut.clear();
//
//        sendAsk();
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() )
//            socketChannel.write( bbOut.getBuffer() );
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
//        bbOut.putInt( magicWord );
//        bbOut.putByte( protocolVersion );
//
//        //--- отправляем запрос на команду сразу после заголовка
//        sendAsk();
//
//        bbOut.flip();
//        while( bbOut.hasRemaining() )
//            socketChannel.write( bbOut.getBuffer() );
//    }
//
//    //--- отправка запроса на команду
//    private void sendAsk() throws Throwable {
//        int dataSize = 1 + 4;   // tag + serial_no
//
//        bbOut.putInt( dataSize );
//
//        bbOut.putByte( tagCommand );
//        bbOut.putInt( serialNo );
//    }
//
//}
