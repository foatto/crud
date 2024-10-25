package foatto.server.ds

import kotlin.system.exitProcess

class DataServer(aConfigFileName: String) : CoreNioServer(aConfigFileName) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0
            try {
                if (args.size == 1) {
                    DataServer(args[0]).run()
                    //--- выход для перезапуска
                    exitCode = 1
                }
                //--- обычный выход с прерыванием цикла запусков
                else println("Usage: DataServer <ini-file-name>")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            exitProcess(exitCode)
        }
    }

    //--- запуск своей специфичной для платформы версии обработчика
    override fun runNewDataWorker() {
        DataWorker(this).start()
    }

}

//        int signature = 3;
//        int error = 0;
//        int command = 0;
//        int packetID = 200;
////        int packetSize = 0;
//        int packetSize = 60000;
//
//        int b = signature << 5 | error << 4 | command;
//
//        byte[] arrByte = { (byte) b, (byte) packetID, (byte) ( packetSize & 0xFF ), (byte) ( ( packetSize >> 8 ) & 0xFF ) };
//
//        System.out.println( Integer.toHexString( b ) );
//        System.out.println( Integer.toHexString( packetID ) );
//        System.out.println( Integer.toHexString( packetSize & 0xFF ) );
//        System.out.println( Integer.toHexString( ( packetSize >> 8 ) & 0xFF ) );
//
//        System.out.println( "crc16 = " + Integer.toHexString( CRC.crc16( arrByte ) ) );
//        System.out.println( "crc16 pm = " + Integer.toHexString( CRC.crc16_modbus( arrByte ) ) );
//        System.out.println( "crc16 ansi = " + Integer.toHexString( CRC.crc16_ansi( arrByte ) ) );
//        System.out.println( "crc16 ccitt_1 = " + Integer.toHexString( CRC.crc16_ccitt_1( arrByte ) ) );
//        System.out.println( "crc16 ccitt_2 = " + Integer.toHexString( CRC.crc16_ccitt_2( arrByte ) ) );
//        System.out.println( "crc16 hdlc = " + Integer.toHexString( CRC.crc16_hdlc( arrByte ) ) );
//        System.out.println( "crc 1-wire = " + Integer.toHexString( CRC.crc_1wire( arrByte ) ) );
