//package foatto.mms.del;
//
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.CommonFunction;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.service.CoreServiceWorker;
//
//import java.io.File;
//import java.net.InetSocketAddress;
//import java.nio.ByteOrder;
//import java.nio.channels.SocketChannel;
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import javax.mail.search.IntegerComparisonTerm;
//
//public abstract class CoreRelayWorker extends CoreServiceWorker {
//
//    private static final String CONFIG_RELAY_IP = "relay_ip";
//    private static final String CONFIG_RELAY_PORT = "relay_port";
//
//    private static final String CONFIG_RELAY_RACK_BUTTON = "relay_rack_button";
//    private static final String CONFIG_RELAY_RACK_LED = "relay_rack_led";
//
//    private static final String CONFIG_RELAY_CAMERA_POWER = "relay_camera_power";
//    private static final String CONFIG_RELAY_CAMERA_LED_ = "relay_camera_led_";
//
//    private static final String CONFIG_RELAY_USB_POWER_ = "relay_usb_power_";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected String relayIP = null;
//    private int relayPort = 0;
//    protected int relayRackButton = 0;
//    protected int relayRackLed = 0;
//    protected int relayCameraPower = 0;
//    protected HashMap<Integer,Integer> hmRelayCameraLed = new HashMap<>();
//    protected HashMap<Integer,Integer> hmRelayUSBPower = new HashMap<>();
////    protected int relayDoor = 0;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private HashMap<Integer,File> hmGPIOInit = new HashMap<>();
//    private File dirTemp = null;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected CoreRelayWorker( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        relayIP = hmConfig.get( CONFIG_RELAY_IP );
//        if( relayIP != null ) relayPort = Integer.parseInt( hmConfig.get( CONFIG_RELAY_PORT ) );
//
//        relayRackButton = Integer.parseInt( hmConfig.get( CONFIG_RELAY_RACK_BUTTON ) );
//        relayRackLed = Integer.parseInt( hmConfig.get( CONFIG_RELAY_RACK_LED ) );
//
//        relayCameraPower = Integer.parseInt( hmConfig.get( CONFIG_RELAY_CAMERA_POWER ) );
//
//        int index = 0;
//        while( true ) {
//            String sCameraLed = hmConfig.get( CONFIG_RELAY_CAMERA_LED_ + index );
//            if( sCameraLed == null ) break;
//            hmRelayCameraLed.put( index, Integer.parseInt( sCameraLed ) );
//            index++;
//        }
//
//        index = 0;
//        while( true ) {
//            String sUSBPower = hmConfig.get( CONFIG_RELAY_USB_POWER_ + index );
//            if( sUSBPower == null ) break;
//            hmRelayUSBPower.put( index, Integer.parseInt( sUSBPower ) );
//            index++;
//        }
//
//        dirTemp = new File( tempDirName );
////        relayDoor = Integer.parseInt( hmConfig.get( CONFIG_DOOR_BUTTON_GPIO ) );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected void writeSingleCoil( int coilNo, boolean status ) throws Throwable {
//        int absCoilNo = Math.abs( coilNo );
//        //--- через GPIO через SysFS
//        if( relayIP == null ) {
//            if( ! hmGPIOInit.containsKey( coilNo ) ) {
//                CommonFunction.runCommand( null, VideoFunction.GPIO_OUT_INIT, Integer.toString( absCoilNo ) );
//                hmGPIOInit.put( coilNo, new File( tempDirName, Integer.toString( absCoilNo ) ) );
//            }
//            CommonFunction.runCommand( null, VideoFunction.GPIO_OUT, Integer.toString( absCoilNo ),
//                                       Integer.toString( ( coilNo < 0 ) != status ? 1 : 0 ) );
//        }
//        //--- через Ethernet-relay
//        else {
//            SocketChannel socketChannel = SocketChannel.open();
//            socketChannel.configureBlocking( true );
//            socketChannel.connect( new InetSocketAddress( relayIP, relayPort ) );
//
//            AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 16 );
//            bbOut.putShort( 0x1973 );   // transaction ID (от балды)
//            bbOut.putShort( 0x0000 );   // ModBus protocol ID (const)
//            bbOut.putShort( 6 );        // packet len
//            bbOut.putByte( 1 );         // unit ID (const)
//            bbOut.putByte( 5 );         // function ID = Write Single Coil
//            bbOut.putShort( absCoilNo );
//            bbOut.putShort( ( coilNo < 0 ) != status ? 0xFF00 : 0x0000 );
//
//            bbOut.flip();
//            int returnSize = bbOut.remaining(); // ответ будет такого же размера
//            while( bbOut.hasRemaining() ) socketChannel.write( bbOut.getBuffer() );
//
//            AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.BIG_ENDIAN, returnSize,
//                    new StringBuilder( "Connection lost to relay = " ).append( relayIP ).append( " : " ).append( relayPort ) );
//            //--- с ответом пока ничего не делаем
//            //...
//
//            if( socketChannel != null )
//                try {
//                    socketChannel.close();
//                }
//                catch( Throwable t ) {}
//                finally {
//                    socketChannel = null;
//                }
//        }
//    }
//
//    protected boolean readDiscreteInputs( int inputNo ) throws Throwable {
//        boolean value;
//
//        int absInputNo = Math.abs( inputNo );
//        //--- через GPIO через SysFS
//        if( relayIP == null ) {
//            if( ! hmGPIOInit.containsKey( inputNo ) ) {
//                CommonFunction.runCommand( null, VideoFunction.GPIO_IN_INIT, Integer.toString( absInputNo ) );
//                hmGPIOInit.put( inputNo, new File( tempDirName, Integer.toString( absInputNo ) ) );
//            }
//            CommonFunction.runCommand( null, VideoFunction.GPIO_IN, Integer.toString( absInputNo ),
//                                       new StringBuilder( tempDirName ).append( '/' ).append( absInputNo ).toString() );
//            String filePath = hmGPIOInit.get( inputNo ).getCanonicalPath();
////            CommonFunction.runCommand( null, "cat", new StringBuilder( relayPath ).append( "/gpio" ).append( inputNo )
////                                                                                  .append( "/value" ).toString(),
////                                       ">", filePath );
//            ArrayList<String> alFileLines = CommonFunction.loadTextFile( filePath, "Cp1251", null, true );
//            value = ( inputNo > 0 ) != alFileLines.get( 0 ).equals( "0" );
//        }
//        else {
//            SocketChannel socketChannel = SocketChannel.open();
//            socketChannel.configureBlocking( true );
//            socketChannel.connect( new InetSocketAddress( relayIP, relayPort ) );
//
//            AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 16 );
//            bbOut.putShort( 0x1973 );       // transaction ID (от балды)
//            bbOut.putShort( 0x0000 );       // ModBus protocol ID (const)
//            bbOut.putShort( 6 );            // packet len
//            bbOut.putByte( 1 );             // unit ID (const)
//            bbOut.putByte( 2 );             // function ID = Read Discrete Inputs
//            bbOut.putShort( absInputNo );   // адрес кнопки
//            bbOut.putShort( 6 );            // 6 входов в 7060
//
//            bbOut.flip();
//            while( bbOut.hasRemaining() ) socketChannel.write( bbOut.getBuffer() );
//
//            int returnSize = 10;
//            AdvancedByteBuffer bbIn = CommonFunction.readChannelToBuffer( socketChannel, ByteOrder.BIG_ENDIAN, returnSize,
//                    new StringBuilder( "Connection lost to relay = " ).append( relayIP ).append( " : " ).append( relayPort ) );
//            bbIn.getShort();    // skip transaction ID
//            bbIn.getShort();    // skip ModBus protocol ID (const)
//            bbIn.getShort();    // skip packet len (always == 4)
//            bbIn.getByte();     // skip unit ID (const)
//            bbIn.getByte();     // skip function ID
//            bbIn.getByte();     // skip byte count (always == 1)
//            int b = bbIn.getByte() & 0xFF;
//            value = ( inputNo > 0 ) != ( b == 0 );
//
//            if( socketChannel != null )
//                try {
//                    socketChannel.close();
//                }
//                catch( Throwable t ) {}
//                finally {
//                    socketChannel = null;
//                }
//        }
//
//        return value;
//    }
//
//}
