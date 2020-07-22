@file:JvmName("DELHandler")
package foatto.mms.core_mms.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.crc16_modbus
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.core_server.ds.DataMessage
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

class DELHandler : MMSHandler() {

    //    //--- кол-во точек, считываемое за один раз = размеру MTU сетевого пакета / размер записи точки = 1500 / 40 -> 32
    //    //--- одновременно это лимитирует размер буферной строки с SQL-выражениями добавления строчек
    //    private static final int RECORD_PAGE_SIZE = 32;
    //
    //    //--- должно сохраняться между циклами работы
    //    private PLADeviceInfo di = null;

    override var startBufSize: Int = 2048
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var packetHeader: PacketHeader? = null
    private var measureCount = 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init( aDataServer: CoreDataServer, aSelectionKey: SelectionKey ) {
        deviceType = MMSHandler.DEVICE_TYPE_DEL_PULSAR

        super.init( aDataServer, aSelectionKey )
    }

    override fun preWork() {
        super.preWork()

        send( PacketHeader.CMD_READ_ID, false )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork( dataWorker: CoreDataWorker ): Boolean {

        //--- сначала дождёмся заголовка
        if( packetHeader == null ) {
            if( bbIn.remaining() < 7 ) {
                bbIn.compact()
                return true
            }
            packetHeader = PacketHeader( bbIn )

            if( packetHeader!!.signature != PacketHeader.SIGNATURE ) {
                writeError( dataWorker.alConn, dataWorker.alStm[ 0 ], " Wrong signature = $packetHeader!!.signature for device ID = $deviceID" )
                return false
            }
            //--- проверку на packetID пропускаем, ибо не используем
            // ...
            //--- вынуждены пропускать проверку CRC, т.к. к моменту известности размера пакета заголовок уже считан и возможно потёрт
            //if( CRC.crc16_modbus( byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), 4 + packetHeader.packetSize + 2 /*CRC16*/ ) ! =0 ) ...
        }
        //--- заголовок сообщает об ошибке
        if( packetHeader!!.error != 0 ) {
            //            if( bbIn.remaining() < 1 ) {
            //                bbIn.compact();
            //                return true;
            //            }
            //            int errorCode = bbIn.getByte() & 0xFF;
            //            writeError( dataWorker.alConn, dataWorker.alStm.get( 0 ),
            //                                                    new StringBuilder( " Error code = " ).append( errorCode )
            //                                                              .append( " from device ID = " ).append( deviceID ).toString() );
            writeError( dataWorker.alConn, dataWorker.alStm[ 0 ], " Error from device ID = $deviceID" )
            return false
        }
        //--- ошибки нет, дожидаемся обещанного объёма данных + CRC16
        if( bbIn.remaining() < packetHeader!!.packetSize + 2 ) {
            bbIn.compact()
            return true
        }

        //--- данные полностью получены, можно разбираться

        //        SQLBatch sqlBatchData;
        //        PLAPoint p;
        //        AdvancedByteBuffer bbData;

        when( packetHeader!!.command ) {
            PacketHeader.CMD_READ_ID -> {
                if( packetHeader!!.packetSize != 10 ) {
                    writeError( dataWorker.alConn, dataWorker.alStm[ 0 ], " Wrong READ_ID packetSize = ${packetHeader!!.packetSize} for device ID = $deviceID" )
                    return false
                }
                deviceID = bbIn.getInt()
                /*int deviceType =*/ bbIn.getShort()/* & 0xFFFF;*/     // not used, always == 0
                fwVersion = bbIn.getShort().toInt() and 0xFFFF
                measureCount = bbIn.getShort().toInt() and 0xFFFF
                if( deviceID <= 0 ) {
                    writeError( dataWorker.alConn, dataWorker.alStm[ 0 ], " Wrong device ID = $deviceID" )
                    return false
                }
                //            if( fwVersion < 0x1053 ) {  // предположительный номер версии с поддержкой пульсара
                //                writeError( dataWorker.alConn, dataWorker.alStm.get( 0 ), new StringBuilder( " Old version = " ).append( Integer.toHexString( fwVersion ) )
                //                                                                       .append( " for device ID = " ).append( deviceID ).toString() );
                //                return false;
                //            }
                //--- общее петролайновское извращение - записываем HEX-номер версии прошивки в как бы десятичном виде
                fwVersion = Integer.parseInt( Integer.toHexString( fwVersion ), 10 )
                deviceConfig = DeviceConfig.getDeviceConfig( dataWorker.alStm[ 0 ], deviceID )
                //--- неизвестный контроллер
                if( deviceConfig == null ) {
                    writeError( dataWorker.alConn, dataWorker.alStm[ 0 ], " Unknown device ID = $deviceID" )
                    writeJournal()
                    return false
                }

                //            send( CMD_READ_CUR_POS, false );
                sbStatus.append( "ID;" )
            }
        }
        //--- поздняк уже проверять, просто дочитаем буфер
        val crc16 = bbIn.getShort().toInt() and 0xFFFF
        //dsLog.error( new StringBuilder( " Invalid CRC for device ID = " ).append( deviceID ).toString() );

        //--- для возможного режима постоянного/длительного соединения
        bbIn.clear()   // других данных быть не должно, именно .clear(), а не .compact()
        //--- этого делать нельзя, т.к. будет обнуление между циклами
        //        begTime = 0;
        //        sbStatus.setLength( 0 );
        //        dataCount = 0;
        //        dataCountAll = 0;
        //        firstPointTime = 0;
        //        lastPointTime = 0;

        packetHeader = null
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun send( cmd: Int, withWriteControl: Boolean ) {
        val bbOut = AdvancedByteBuffer( 8, byteOrder ) // хватит и 6 байт, но для кеширования сделаем 8

        //--- отправка первоначальной команды (на получения deviceID)
        //--- packetID у меня не используется, на всякий случай отправляю commandCode
        PacketHeader( cmd, 0, cmd, 0 ).write( bbOut )

        send( bbOut, withWriteControl )
    }

    private fun send( bbOut: AdvancedByteBuffer, withWriteControl: Boolean ) {

        if( withWriteControl ) outBuf( bbOut )
        else {
            bbOut.flip()
            clqOut.offer( DataMessage( byteBuffer = bbOut ) )
            dataServer.putForWrite( this )
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private class PacketHeader {

        companion object {

            val SIGNATURE = 0x05  // 8 bit: 7 bit signature + 1 bit (0 = query, 1 = answer)

            val CMD_READ_ID = 0
            val CMD_READ_MEASURE_TABLE = 41
            val CMD_READ_MEMORY = 42
        }

        var signature: Int = 0
        var owner: Int = 0       // 8 bit: зарезервировано: равно полю Owner в запросе
        var command: Int = 0     // 8 bit
        var error: Int = 0       // 8 bit: 0 = success, 1 = error
        var packetID: Int = 0    // 8 bit
        var packetSize: Int = 0  // 16 bit: размер поля параметров в пакете (исключая размер CRC и размер самого заголовка)

        internal constructor( aCommand: Int, aError: Int, aPacketID: Int, aPacketSize: Int ) {
            signature = SIGNATURE
            owner = 0
            command = aCommand
            error = aError
            packetID = aPacketID
            packetSize = aPacketSize
        }

        internal constructor( byteBuffer: AdvancedByteBuffer ) {
            signature = byteBuffer.getByte().toInt() and 0x7F
            owner = byteBuffer.getByte().toInt()
            command = byteBuffer.getByte().toInt()
            error = byteBuffer.getByte().toInt()
            packetID = byteBuffer.getByte().toInt()
            packetSize = byteBuffer.getShort().toInt()
        }

        //--- запись команды, с дополнительными данными и без CRC
        fun write( byteBuffer: AdvancedByteBuffer ) {
            byteBuffer.putByte( signature.toByte() )
            byteBuffer.putByte( owner.toByte() )
            byteBuffer.putByte( command.toByte() )
            byteBuffer.putByte( error.toByte() )
            byteBuffer.putByte( packetID.toByte() )
            byteBuffer.putShort( packetSize.toShort() )

            if( packetSize == 0 ) byteBuffer.putShort( crc16_modbus( byteBuffer.array(), byteBuffer.arrayOffset(), 7, false ) )
        }

    }

}

        //        case CMD_READ_CUR_POS:
        //            if( packetHeader.packetSize != 40 + 128 ) {
        //                writeError( dataWorker.alConn, dataWorker.alStm.get( 0 ), new StringBuilder( " Wrong CUR_POS packetSize = " ).append( packetHeader.packetSize )
        //                                                                       .append( " for device ID = " ).append( deviceID ).toString() );
        //                return false;
        //            }
        //            p = new PLAPoint( this, bbIn );
        //            di = new PLADeviceInfo( bbIn );
        //
        //            long curTime = System.currentTimeMillis();
        //            if( p.time > curTime - MAX_PAST_TIME && p.time < curTime + MAX_FUTURE_TIME ) {
        //                bbData = new AdvancedByteBuffer( dataWorker.alConn.get( 0 ).getTextFieldMaxSize() / 2 );
        //
        //                putBitSensor( p.d, 0, 8, bbData );
        //                putSensorData( 8, 2, di.systemVoltage, bbData );
        //                putSensorData( 9, 2, di.batteryVoltage, bbData );
        //                putDigitalSensor( p.a, 10, 2, bbData );
        //                putSensorData( 18, 2, di.temperature, bbData );
        //
        //                putSensorPortNumAndDataSize( SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData );
        //                bbData.putInt( p.wgsX ).putInt( p.wgsY ).putShort( p.speed ).putInt( p.dist );
        //
        //                sqlBatchData = new SQLBatch( 1 );
        //                addPoint( dataWorker.alStm.get( 0 ), p.time, bbData, sqlBatchData );
        //                for( CoreAdvancedStatement stm : dataWorker.alStm ) sqlBatchData.execute( stm );
        //                for( CoreAdvancedConnection conn : dataWorker.alConn ) conn.commit();
        //            }
        //            sbStatus.append( "CurPos;" );
        //
        //            //--- вместо recordStart используем общий dataCount
        //            dataCount = 1;
        //            dataCountAll = 1;
        //
        ////            //--- проверить смену прошивки
        //////        boolean isUpdateFirmware = deviceConfig.isUpdateFirmware;
        ////            //--- проверим смену конфига
        ////            if( deviceConfig.isUpdateConfig ) {
        ////                if( ! sendConfig( dataWorker.alConn.get( 0 ), dataWorker.alStm.get( 0 ) ) ) return false;
        ////            }
        //            //--- запросить первый набор точек, если они есть
        //            if( recordCount > 0 /*&& cc.autoID != 0*/ ) {
        //                sendReadCoords();
        //            }
        //            else {
        //                sbStatus.append( "Ok;" );
        //                errorText = null;
        //                writeSession( dataWorker.alConn, dataWorker.alStm.get( 0 ), true );
        //                return false;
        //            }
        //            break;
        //
        //        case CMD_WRITE_CONFIG:
        //            sbStatus.append( "Configured;Ok;" );
        //            errorText = null;
        //            writeSession( dataWorker.alConn, dataWorker.alStm.get( 0 ), true );
        //            //--- закрываем соединение от греха подальше :)
        //            return false;
        //            //break;
        //
        //        case CMD_READ_COORDS:
        //            if( packetHeader.packetSize % 40 != 0 ) {
        //                writeError( dataWorker.alConn, dataWorker.alStm.get( 0 ), new StringBuilder( " Wrong READ_COORDS packetSize = " ).append( packetHeader.packetSize )
        //                                                                       .append( " for device ID = " ).append( deviceID ).toString() );
        //                return false;
        //            }
        //            int pc = packetHeader.packetSize / 40;
        //
        //            sqlBatchData = new SQLBatch( pc );
        //            curTime = System.currentTimeMillis();
        //            for( int i = 0; i < pc; i++ ) {
        //                p = new PLAPoint( this, bbIn );
        //                if( p.time > curTime - MAX_PAST_TIME && p.time < curTime + MAX_FUTURE_TIME ) {
        //                    bbData = new AdvancedByteBuffer( dataWorker.alConn.get( 0 ).getTextFieldMaxSize() / 2 );
        //
        //                    putBitSensor( p.d, 0, 8, bbData );
        //                    putSensorData( 8, 2, di.systemVoltage, bbData );
        //                    putSensorData( 9, 2, di.batteryVoltage, bbData );
        //                    putDigitalSensor( p.a, 10, 2, bbData );
        //                    putSensorData( 18, 2, di.temperature, bbData );
        //
        //                    putSensorPortNumAndDataSize( SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData );
        //                    bbData.putInt( p.wgsX ).putInt( p.wgsY ).putShort( p.speed ).putInt( p.dist );
        //
        //                    addPoint( dataWorker.alStm.get( 0 ), p.time, bbData, sqlBatchData );
        //                }
        //            }
        //            for( CoreAdvancedStatement stm : dataWorker.alStm ) sqlBatchData.execute( stm );
        //            for( CoreAdvancedConnection conn : dataWorker.alConn ) conn.commit();
        //
        //            //--- есть/остались ещё точки? (используем -1, т.к. dataCount  у нас начинается с 1 - первой точкой становится CUR_COORD)
        //            if( ( dataCount - 1 ) < recordCount ) sendReadCoords();
        //            //--- точек больше нет, пора закругляться
        //            else {
        //                send( CMD_CLEAR_COORDS, true );
        //                sbStatus.append( "DataRead;" );
        //            }
        //            break;
        //
        //        case CMD_CLEAR_COORDS:
        //            sbStatus.append( "Ok;" );
        //            errorText = null;
        //            writeSession( dataWorker.alConn, dataWorker.alStm.get( 0 ), true );
        //            //--- закрываем соединение от греха подальше :)
        //            return false;
        //            //break;

    //    private void sendDelay() throws Throwable {
    //        Integer delay = chmDeviceDelay.get( deviceID );
    //        delay = delay == null ? 1 : delay + 1;
    //        chmDeviceDelay.put( deviceID, delay );
    //
    //        //--- не более чем на 540 мин (9 часов), чтобы секундами не переполнить signed short в минус
    //        int second = Math.min( delay, 540 ) * 60 + 0x04;    // в качестве причины указываем "Нет регистрации в Мск"
    //
    //        AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 6 + 2, getByteOrder() );
    //
    //        PacketHeader ph = new PacketHeader( CMD_SEND_DELAY, 0, CMD_SEND_DELAY, 2 );
    //        ph.write( bbOut );
    //        //--- дополняем данными для последующего вычисления CRC
    //        bbOut.putShort( (short) second );
    //        bbOut.putShort( (short) CRC.crc16_modbus( bbOut.array(), bbOut.arrayOffset(), 4 + ph.packetSize, false ) );
    //
    //        send( bbOut, false );
    //    }
    //
    //    private void sendReadCoords() throws Throwable {
    //        //--- вместо recordStart используем общий dataCount
    //        int recordReadCount = ( dataCount - 1 ) + RECORD_PAGE_SIZE < recordCount ? RECORD_PAGE_SIZE : recordCount - ( dataCount - 1 );
    //
    //        AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 16, getByteOrder() );    // для работы хватит 10 байт, но мы сделаем 16 для кеширования
    //
    //        PacketHeader ph = new PacketHeader( CMD_READ_COORDS, 0, CMD_READ_COORDS, 4 );
    //        ph.write( bbOut );
    //        //--- дополняем данными для последующего вычисления CRC
    //        bbOut.putShort( (short) ( dataCount - 1 ) );
    //        bbOut.putShort( (short) recordReadCount );
    //        bbOut.putShort( (short) CRC.crc16_modbus( bbOut.array(), bbOut.arrayOffset(), 4 + ph.packetSize, false ) );
    //
    //        send( bbOut, true );
    //
    //        dataCount += recordReadCount;
    //        dataCountAll += recordReadCount;
    //    }

    //    private boolean sendConfig( CoreAdvancedConnection conn, CoreAdvancedStatement stm ) throws Throwable {
    //        //--- проверка версии прошивки
    //        if( fwVersion < 400 ) {
    //            writeError( conn, stm, new StringBuilder( " Config not writed: old firmware version = " ).append( fwVersion ).append( " for device ID = " ).append( deviceID ).toString() );
    //            return false;
    //        }
    //
    //        //--- делитель показателя бортового напряжения
    ////        double dv = 100;
    ////        if( di.systemVoltage / dv < 5 ) {
    ////            writeError( conn, stm, new StringBuilder( " Config not writed: low voltage [system/battery] = " )
    ////                                  .append( StringFunction.getSplittedDouble( di.systemVoltage / dv, 2 ) ).append( " / " )
    ////                                  .append( StringFunction.getSplittedDouble( di.batteryVoltage / dv, 2 ) )
    ////                                  .append( " for device ID = " ).append( deviceID ).toString() );
    ////            return false;
    ////        }
    //
    //        //--- проверка на наличие номера телефона
    //        if( deviceConfig.cellNo == null || deviceConfig.cellNo.trim().length() != 12 ) {
    //            writeError( conn, stm, new StringBuilder( " Config not writed: wrong phone = " ).append( deviceConfig.cellNo )
    //                                  .append( " for device ID = " ).append( deviceID ).toString() );
    //            return false;
    //        }
    //
    //        //--- загрузить конфигурацию
    //        HashMap<String,String> hmConfig = new HashMap<>();
    //        CoreAdvancedResultSet rs = stm.executeQuery( new StringBuilder(
    //            " SELECT param_name , param_value FROM MMS_device_config WHERE device_id IN ( 0 , " ).append( deviceID )
    //            .append( " ) ORDER BY device_id " ).toString() );
    //        while( rs.next() ) hmConfig.put( rs.getString( 1 ).trim(), rs.getString( 2 ).trim() );
    //        rs.close();
    //
    //        //--- проверка на наличие APN для данного префикса номера телефона
    //        String phonePrefix = deviceConfig.cellNo.substring( 0, 5 );
    //        String apnURL = null, apnLogin = null, apnPwd = null;
    //        for( String key : hmConfig.keySet() ) {
    //            if( key.startsWith( "GPRSAPN" ) ) {
    //                String value = hmConfig.get( key );
    //                if( value.contains( phonePrefix ) ) {
    //                    StringTokenizer st = new StringTokenizer( value.substring( value.indexOf( ';' ) + 1 ), "," );
    //                    try {
    //                        apnURL = st.nextToken();
    //                        apnLogin = st.nextToken();
    //                        apnPwd = st.nextToken();
    //                    }
    //                    catch( NoSuchElementException nsee ) {}
    //                    break;
    //                }
    //            }
    //        }
    //        if( apnURL == null || apnLogin == null || apnPwd == null || apnURL.isEmpty() || apnLogin.isEmpty() || apnPwd.isEmpty() ) {
    //            writeError( conn, stm, new StringBuilder( " Config not writed: APN not found " )
    //                                  .append( " for device ID = " ).append( deviceID ).toString() );
    //            return false;
    //        }
    //
    //        AdvancedByteBuffer bbOut = new AdvancedByteBuffer( 2048, getByteOrder() );  // 6 + 1487, но 2048 для кеширования
    //
    //        PacketHeader ph = new PacketHeader( CMD_WRITE_CONFIG, 0, CMD_WRITE_CONFIG, 1487 );
    //        ph.write( bbOut );
    //
    //        putZeroEndedString( apnURL, 24, bbOut );
    //        putZeroEndedString( apnLogin, 16, bbOut );
    //        putZeroEndedString( apnPwd, 16, bbOut );
    //        bbOut.putByte( (byte) getIntegerValue( hmConfig, "GPRSActive", 1 ) );
    //        bbOut.putByte( (byte) getIntegerValue( hmConfig, "GPRSConnectInterval", 5 ) );
    //        bbOut.put( new byte[ 32 ] );
    //
    //        boolean isOneGoodIP = false;    // отметим наличие хотя бы одного нормального IP:port
    //        for( int i = 0; i < 4; i++ ) {
    //            String value = hmConfig.get( new StringBuilder( "GPRSIP" ).append( i + 1 ).toString() );
    //            String ip = "";
    //            String port = "";
    //            try {
    //                StringTokenizer st = new StringTokenizer( value, ":" );
    //                ip = st.nextToken();
    //                port = st.nextToken();
    //                isOneGoodIP = true;
    //            }
    //            catch( Throwable t ) {}
    //            putZeroEndedString( ip, 16, bbOut );
    //            putZeroEndedString( port, 6, bbOut );
    //        }
    //        bbOut.put( new byte[ 100 ] );
    //        //--- если ни одного нормального IP:port не было, то прерываем переконфигурацию
    //        if( ! isOneGoodIP ) {
    //            writeError( conn, stm, new StringBuilder( " Config not writed: APN not found " )
    //                                             .append( " for device ID = " ).append( deviceID ).toString() );
    //            return false;
    //        }
    //
    //        bbOut.putByte( (byte) getIntegerValue( hmConfig, "GPSTimeInterval", 10 ) );
    //        putZeroEndedString( "", 32, bbOut );    // имя объекта "Топливозаправщик" (ASCII)
    //        putZeroEndedString( "", 8, bbOut );     // гос. номер объекта "в595вх" (ASCII)
    //        bbOut.putByte( (byte) getIntegerValue( hmConfig, "GPSFilterOff", 0 ) );
    //        bbOut.putByte( (byte) getIntegerValue( hmConfig, "GPSAccelLevel", 45 ) );
    //        bbOut.put( new byte[ 38 ] );
    //        //--- аналоговые датчики
    //        for( int i = 0; i < 8; i++ ) {
    //            bbOut.putByte( (byte) ( i + 1 ) );
    //            bbOut.putByte( (byte) 1 );
    //            bbOut.put( new byte[ 56 ] );
    //        }
    //        //--- цифровые датчики
    //        for( int i = 0; i < 8; i++ ) {
    //            bbOut.putByte( (byte) ( i + 1 ) );
    //            bbOut.putByte( (byte) 1 );          // признак активности канала вкл./выкл. (0.. 1)
    //            bbOut.put( new byte[ 32 ] );    // имя датчика
    //            bbOut.putByte( (byte) 2 );          // 2 - любое изменение фронта
    //            bbOut.put( new byte[ 22 ] );
    //        }
    //        //--- номера телефонов
    //        bbOut.put( new byte[ 2 * 8 * 13 ] );
    //
    //        bbOut.putShort( (short) CRC.crc16_modbus( bbOut.array(), bbOut.arrayOffset(), 4 + ph.packetSize, false ) );
    //
    //        send( bbOut, false );
    //        return true;
    //    }

    //    private int getIntegerValue( HashMap<String,String> hmConfig, String paramName, int defaultValue ) {
    //        int value = defaultValue;
    //        try { value = Integer.parseInt( hmConfig.get( paramName ) ); }
    //        catch( NumberFormatException nfe ) {
    //            AdvancedLogger.error( new StringBuilder( "Send Config: wrong value for " ).append( paramName ).toString() );
    //        }
    //        return value;
    //    }
    //
    //    private void putZeroEndedString( String s, int maxLen, AdvancedByteBuffer bb ) throws Throwable {
    //        s = s.substring( 0, Math.min( maxLen, s.length() ) );
    //        bb.put( s.getByteBuffer() );
    //        for( int i = s.length(); i < maxLen; i++ ) bb.putByte( (byte) 0 );
    //    }

    //    private static class PLAPoint {
    //
    //        private static final int STATUS_WRONG_COORD  = 0x0001;    // Неправильные координаты
    //        private static final int STATUS_GPS_FAIL     = 0x0002;       // Отказ GPS
    //        private static final int STATUS_PARKING      = 0x0004;        // Стоянка
    //        private static final int STATUS_EW_COORD     = 0x4000;       // E/W позиция
    //        private static final int STATUS_NS_COORD     = 0x8000;       // N/S позиция
    //
    //        private static final int ANALOG_INPUT_COUNT = 8;
    //
    //        public long time = 0;
    //
    //        public int wgsX = 0;
    //        public int wgsY = 0;
    //        public short speed = 0;
    //        public int dist = 0;
    //
    //        public int d = 0;
    //        public int[] a = new int[ ANALOG_INPUT_COUNT ];
    //
    //        private PLAPoint( PLAHandler aPLAHandler, AdvancedByteBuffer byteBuffer ) throws Throwable {
    //
    //            time = byteBuffer.getInt() * 1000L; // было UNIX-time, но только в сек - переводим в мсек
    //
    //            //--- сначала долгота (X), потом широта (Y) в формате 179^44.1234'
    //            int gpsX = byteBuffer.getInt();
    //            int gpsY = byteBuffer.getInt();
    //            //--- преобразование наших ёбнутых координат, скоростей и дистанций
    //            int tmpX = gpsX / 1000_000;
    //            int tmpY = gpsY / 1000_000;
    //            wgsX = (int) ( ( tmpX + ( gpsX - tmpX * 1000_000 ) / 10_000.0 / 60.0 ) * XyProjection.WGS_KOEF_i );
    //            wgsY = (int) ( ( tmpY + ( gpsY - tmpY * 1000_000 ) / 10_000.0 / 60.0 ) * XyProjection.WGS_KOEF_i );
    //
    //            //--- скорость в формате xxx.xx [миль/час==узлы]
    //            speed = aPLAHandler.roundSpeed( ( byteBuffer.getShort() & 0xFFFF ) / 100.0 * 1.852 );
    //
    //            //if( speed < 0 ) speed = 0;
    //            //if( speed > 255 ) speed = 255;
    //            //--- дистанция - в милях, переводим в метры
    //            dist = (int) Math.round( byteBuffer.getFloat() * 1.852 * 1000 );
    //
    //            byteBuffer.getShort();  // SKIP azimuth
    //            int status = byteBuffer.getShort() & 0xFFFF;
    //            int event = byteBuffer.getByte() & 0xFF;
    //
    //            d = byteBuffer.getByte() & 0xFF;
    //            for( int i = 0; i < ANALOG_INPUT_COUNT; i++ )
    //                a[ i ] = byteBuffer.getShort() & 0xFFFF;
    //
    //            //--- дополнительная обработка данных по полю status ---
    //
    //            //--- нулевые координаты указывают на ошибку GPS
    //            if( gpsX == 0 && gpsY == 0 ) status |= STATUS_WRONG_COORD;
    //            //--- при неправильных GPS-данных всё = 0
    //            if( ( status & ( STATUS_WRONG_COORD | STATUS_GPS_FAIL ) ) != 0 ) {
    //                wgsX = wgsY = dist = 0;
    //                speed = (short) 0;
    //            }
    //            else {
    //                //--- пробег на событиях не имеет значения
    //                if( event != 0 ) dist = 0;
    //                //--- на стоянке скорость = 0
    //                if( ( status & STATUS_PARKING ) != 0 ) speed = 0;
    //                //--- установка знака координат в зависимости от статуса
    //                if( ( status & STATUS_NS_COORD ) == 0 ) wgsX = -wgsX;
    //                if( ( status & STATUS_EW_COORD ) == 0 ) wgsY = -wgsY;
    //            }
    //        }
    //    }
