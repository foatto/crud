package foatto.mms.core_mms.ds

import foatto.core.app.xy.XyProjection
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.crc16_modbus
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.core_server.ds.DataMessage
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.sql.SQLBatch
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.round

class PLAHandler : MMSHandler() {

    companion object {
        //    public static final int CMD_NONE                    =-1;
        val CMD_READ_ID = 0  // (empty)  => (ControllerIDData)

        //    public static final int CMD_READ_CUR_POS_SMS        = 1;  // (empty)  => (ReadCurrentPositionDataSMS)
        val CMD_READ_COORDS = 2  // (ReadCoordinatesData)  => (N)*(GPSPointData)
        val CMD_CLEAR_COORDS = 3  // (empty)  => (empty)
        val CMD_WRITE_CONFIG = 4  // (ControllerConfigData)  => (empty)

        //    public static final int CMD_WRITE_FIRMWARE          = 5;  // (FirmwareData)+(BUFFER:[BYTE*N])  => (empty)
        //    public static final int CMD_RESET_FIRMWARE          = 6;  // (empty)  => (empty)
        val CMD_SEND_DELAY = 7  // (DelaySecondsData)  => (empty)
        val CMD_READ_CUR_POS = 8  // (empty)  => (ReadCurrentPositionData)
        //    public static final int CMD_SEND_COMMAND            = 9;   // (SendCommandData)  => (empty)

        //--- кол-во точек, считываемое за один раз = размеру MTU сетевого пакета / размер записи точки = 1500 / 40 -> 32
        //--- одновременно это лимитирует размер буферной строки с SQL-выражениями добавления строчек
        private val RECORD_PAGE_SIZE = 32

        //--- кого на сколько послали в прошлый раз
        private val chmDeviceDelay = ConcurrentHashMap<String, Int>()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override var startBufSize: Int = 2048
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- должно сохраняться между циклами работы
    private var di: PLADeviceInfo? = null

    private var recordCount = 0
    private var packetHeader: PacketHeader? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aDataServer: CoreDataServer, aSelectionKey: SelectionKey) {
        deviceType = DEVICE_TYPE_PETROLINE

        super.init(aDataServer, aSelectionKey)
    }

    override fun preWork() {
        super.preWork()

        send(CMD_READ_ID, false)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork(dataWorker: CoreDataWorker): Boolean {

        //--- сначала дождёмся заголовка
        if (packetHeader == null) {
            if (bbIn.remaining() < 4) {
                bbIn.compact()
                return true
            }
            packetHeader = PacketHeader(bbIn)

            if (packetHeader!!.signature != PacketHeader.PACKET_SIGNATURE) {
                writeError(
                    conn = dataWorker.conn,
                    stm = dataWorker.stm,
                    dirSessionLog = dirSessionLog,
                    zoneId = zoneId,
                    deviceConfig = deviceConfig,
                    fwVersion = fwVersion,
                    begTime = begTime,
                    address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                    status = status,
                    errorText = "Wrong signature = ${packetHeader!!.signature} for serialNo = $serialNo",
                    dataCount = dataCount,
                    dataCountAll = dataCountAll,
                    firstPointTime = firstPointTime,
                    lastPointTime = lastPointTime,
                )
                return false
            }
            //--- проверку на packetID пропускаем, ибо не используем
            // ...
            //--- вынуждены пропускать проверку CRC, т.к. к моменту известности размера пакета заголовок уже считан и возможно потёрт
            //if( CRC.crc16_modbus( byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), 4 + packetHeader.packetSize + 2 /*CRC16*/ ) ! =0 ) ...
        }
        //--- заголовок сообщает об ошибке
        if (packetHeader!!.error != 0) {
            if (bbIn.remaining() < 1) {
                bbIn.compact()
                return true
            }
            val errorCode = bbIn.getByte().toInt() and 0xFF
            writeError(
                conn = dataWorker.conn,
                stm = dataWorker.stm,
                dirSessionLog = dirSessionLog,
                zoneId = zoneId,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                status = status,
                errorText = "Error code = $errorCode from serialNo = $serialNo",
                dataCount = dataCount,
                dataCountAll = dataCountAll,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
            )
            return false
        }
        //--- ошибки нет, дожидаемся обещанного объёма данных + CRC16
        if (bbIn.remaining() < packetHeader!!.packetSize + 2) {
            bbIn.compact()
            return true
        }

        //--- данные полностью получены, можно разбираться

        val sqlBatchData: SQLBatch
        var p: PLAPoint
        var bbData: AdvancedByteBuffer

        when (packetHeader!!.command) {
            CMD_READ_ID -> {
                if (packetHeader!!.packetSize != 7) {
                    writeError(
                        conn = dataWorker.conn,
                        stm = dataWorker.stm,
                        dirSessionLog = dirSessionLog,
                        zoneId = zoneId,
                        deviceConfig = deviceConfig,
                        fwVersion = fwVersion,
                        begTime = begTime,
                        address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                        status = status,
                        errorText = "Wrong READ_ID packetSize = $packetHeader!!.packetSize for serialNo = $serialNo",
                        dataCount = dataCount,
                        dataCountAll = dataCountAll,
                        firstPointTime = firstPointTime,
                        lastPointTime = lastPointTime,
                    )
                    return false
                }
                serialNo = (bbIn.getShort().toInt() and 0xFFFF).toString()
                fwVersion = (bbIn.getShort().toInt() and 0xFFFF).toString()
                /*recordSize =*/ bbIn.getByte()// & 0xFF; SKIP Record Size
                recordCount = bbIn.getShort().toInt() and 0xFFFF
                if (serialNo.isEmpty()) {
                    writeError(
                        conn = dataWorker.conn,
                        stm = dataWorker.stm,
                        dirSessionLog = dirSessionLog,
                        zoneId = zoneId,
                        deviceConfig = deviceConfig,
                        fwVersion = fwVersion,
                        begTime = begTime,
                        address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                        status = status,
                        errorText = "Wrong serialNo = $serialNo",
                        dataCount = dataCount,
                        dataCountAll = dataCountAll,
                        firstPointTime = firstPointTime,
                        lastPointTime = lastPointTime,
                    )
                    return false
                }
                if (fwVersion.toInt() < 0x400) {
                    writeError(
                        conn = dataWorker.conn,
                        stm = dataWorker.stm,
                        dirSessionLog = dirSessionLog,
                        zoneId = zoneId,
                        deviceConfig = deviceConfig,
                        fwVersion = fwVersion,
                        begTime = begTime,
                        address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                        status = status,
                        errorText = "Old version = $fwVersion for serialNo = $serialNo",
                        dataCount = dataCount,
                        dataCountAll = dataCountAll,
                        firstPointTime = firstPointTime,
                        lastPointTime = lastPointTime,
                    )
                    return false
                }
                //--- наше петромапское извращение - записываем HEX-номер версии прошивки в как бы десятичном виде
                deviceConfig = DeviceConfig.getDeviceConfig(dataWorker.stm, serialNo)
                //--- неизвестный контроллер
                if (deviceConfig == null) {
                    sendDelay()
                    writeError(
                        conn = dataWorker.conn,
                        stm = dataWorker.stm,
                        dirSessionLog = dirSessionLog,
                        zoneId = zoneId,
                        deviceConfig = deviceConfig,
                        fwVersion = fwVersion,
                        begTime = begTime,
                        address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                        status = status,
                        errorText = "Unknown serialNo = $serialNo",
                        dataCount = dataCount,
                        dataCountAll = dataCountAll,
                        firstPointTime = firstPointTime,
                        lastPointTime = lastPointTime,
                    )
                    writeJournal(
                        dirJournalLog = dirJournalLog,
                        zoneId = zoneId,
                        address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                        errorText = "Unknown serialNo = $serialNo",
                    )
                    return false
                }
                chmDeviceDelay.remove(serialNo)

                send(CMD_READ_CUR_POS, false)
                status += " ID;"
            }

            CMD_READ_CUR_POS -> {
                if (packetHeader!!.packetSize != 40 + 128) {
                    writeError(
                        conn = dataWorker.conn,
                        stm = dataWorker.stm,
                        dirSessionLog = dirSessionLog,
                        zoneId = zoneId,
                        deviceConfig = deviceConfig,
                        fwVersion = fwVersion,
                        begTime = begTime,
                        address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                        status = status,
                        errorText = "Wrong CUR_POS packetSize = ${packetHeader!!.packetSize} for serialNo = $serialNo",
                        dataCount = dataCount,
                        dataCountAll = dataCountAll,
                        firstPointTime = firstPointTime,
                        lastPointTime = lastPointTime,
                    )
                    return false
                }
                p = PLAPoint(this, bbIn)
                di = PLADeviceInfo(bbIn)

                val curTime = getCurrentTimeInt()
                if (p.time > curTime - MAX_PAST_TIME && p.time < curTime + MAX_FUTURE_TIME) {
                    bbData = AdvancedByteBuffer(dataWorker.conn.dialect.textFieldMaxSize / 2)

                    putBitSensor(deviceConfig!!.index, p.d, 0, 8, bbData)
                    putSensorData(deviceConfig!!.index, 8, 2, di!!.systemVoltage, bbData)
                    putSensorData(deviceConfig!!.index, 9, 2, di!!.batteryVoltage, bbData)
                    putDigitalSensor(deviceConfig!!.index, p.tmA, 10, 2, bbData)
                    putSensorData(deviceConfig!!.index, 18, 2, di!!.temperature, bbData)

                    putSensorPortNumAndDataSize(deviceConfig!!.index, SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData)
                    bbData.putInt(p.wgsX).putInt(p.wgsY).putShort(p.speed).putInt(p.dist)

                    sqlBatchData = SQLBatch()
                    addPoint(dataWorker.stm, deviceConfig!!, p.time, bbData, sqlBatchData)
                    sqlBatchData.execute(dataWorker.stm)
                    dataWorker.conn.commit()
                }
                status += " CurPos;"

                //--- вместо recordStart используем общий dataCount
                dataCount = 1
                dataCountAll = 1

                //            //--- проверить смену прошивки
                ////        boolean isUpdateFirmware = deviceConfig.isUpdateFirmware;
                //            //--- проверим смену конфига
                //            if( deviceConfig.isUpdateConfig ) {
                //                if( ! sendConfig( dataWorker.conn.get( 0 ), dataWorker.alStm.get( 0 ) ) ) return false;
                //            }
                //--- запросить первый набор точек, если они есть
                if (recordCount > 0 /*&& cc.autoID != 0*/) {
                    sendReadCoords()
                } else {
                    status += " Ok;"
                    errorText = ""
                    writeSession(
                        conn = dataWorker.conn,
                        stm = dataWorker.stm,
                        dirSessionLog = dirSessionLog,
                        zoneId = zoneId,
                        deviceConfig = deviceConfig!!,
                        fwVersion = fwVersion,
                        begTime = begTime,
                        address = (selectionKey!!.channel() as SocketChannel).toString(),
                        status = status,
                        errorText = errorText,
                        dataCount = dataCount,
                        dataCountAll = dataCountAll,
                        firstPointTime = firstPointTime,
                        lastPointTime = lastPointTime,
                        isOk = true,
                    )
                    return false
                }
            }

            CMD_WRITE_CONFIG -> {
                status += " Configured;Ok;"
                errorText = ""
                writeSession(
                    conn = dataWorker.conn,
                    stm = dataWorker.stm,
                    dirSessionLog = dirSessionLog,
                    zoneId = zoneId,
                    deviceConfig = deviceConfig!!,
                    fwVersion = fwVersion,
                    begTime = begTime,
                    address = (selectionKey!!.channel() as SocketChannel).toString(),
                    status = status,
                    errorText = errorText,
                    dataCount = dataCount,
                    dataCountAll = dataCountAll,
                    firstPointTime = firstPointTime,
                    lastPointTime = lastPointTime,
                    isOk = true,
                )
                //--- закрываем соединение от греха подальше :)
                return false
            }
            //break;

            CMD_READ_COORDS -> {
                if (packetHeader!!.packetSize % 40 != 0) {
                    writeError(
                        conn = dataWorker.conn,
                        stm = dataWorker.stm,
                        dirSessionLog = dirSessionLog,
                        zoneId = zoneId,
                        deviceConfig = deviceConfig,
                        fwVersion = fwVersion,
                        begTime = begTime,
                        address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                        status = status,
                        errorText = " Wrong READ_COORDS packetSize = ${packetHeader!!.packetSize} for serialNo = $serialNo",
                        dataCount = dataCount,
                        dataCountAll = dataCountAll,
                        firstPointTime = firstPointTime,
                        lastPointTime = lastPointTime,
                    )
                    return false
                }
                val pc = packetHeader!!.packetSize / 40

                sqlBatchData = SQLBatch()
                val curTime = getCurrentTimeInt()
                for (i in 0 until pc) {
                    p = PLAPoint(this, bbIn)
                    if (p.time > curTime - MAX_PAST_TIME && p.time < curTime + MAX_FUTURE_TIME) {
                        bbData = AdvancedByteBuffer(dataWorker.conn.dialect.textFieldMaxSize / 2)

                        putBitSensor(deviceConfig!!.index, p.d, 0, 8, bbData)
                        putSensorData(deviceConfig!!.index, 8, 2, di!!.systemVoltage, bbData)
                        putSensorData(deviceConfig!!.index, 9, 2, di!!.batteryVoltage, bbData)
                        putDigitalSensor(deviceConfig!!.index, p.tmA, 10, 2, bbData)
                        putSensorData(deviceConfig!!.index, 18, 2, di!!.temperature, bbData)

                        putSensorPortNumAndDataSize(deviceConfig!!.index, SensorConfig.GEO_PORT_NUM, SensorConfig.GEO_DATA_SIZE, bbData)
                        bbData.putInt(p.wgsX).putInt(p.wgsY).putShort(p.speed).putInt(p.dist)

                        addPoint(dataWorker.stm, deviceConfig!!, p.time, bbData, sqlBatchData)
                    }
                    if (firstPointTime == 0) firstPointTime = p.time
                    lastPointTime = p.time
                }
                sqlBatchData.execute(dataWorker.stm)
                dataWorker.conn.commit()

                //--- есть/остались ещё точки? (используем -1, т.к. dataCount  у нас начинается с 1 - первой точкой становится CUR_COORD)
                if (dataCount - 1 < recordCount) sendReadCoords()
                else {
                    send(CMD_CLEAR_COORDS, true)
                    status += " DataRead;"
                }//--- точек больше нет, пора закругляться
            }

            CMD_CLEAR_COORDS -> {
                status += " Ok;"
                errorText = ""
                writeSession(
                    conn = dataWorker.conn,
                    stm = dataWorker.stm,
                    dirSessionLog = dirSessionLog,
                    zoneId = zoneId,
                    deviceConfig = deviceConfig!!,
                    fwVersion = fwVersion,
                    begTime = begTime,
                    address = (selectionKey!!.channel() as SocketChannel).toString(),
                    status = status,
                    errorText = errorText,
                    dataCount = dataCount,
                    dataCountAll = dataCountAll,
                    firstPointTime = firstPointTime,
                    lastPointTime = lastPointTime,
                    isOk = true,
                )
                //--- закрываем соединение от греха подальше :)
                return false
            }
        }//break;
        //--- поздняк уже проверять, просто дочитаем буфер
        val crc16 = bbIn.getShort().toInt() and 0xFFFF
        //dsLog.error( new StringBuilder( " Invalid CRC for device ID = " ).append( deviceID ).toString() );

        //--- для возможного режима постоянного/длительного соединения
        bbIn.clear()   // других данных быть не должно, именно .clear(), а не .compact()
        //--- этого делать нельзя, т.к. будет обнуление между циклами
        //        begTime = 0;
        //        sbStatus.setLength( 0 );
        //        dataCount = 0;

        packetHeader = null
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun send(cmd: Int, withWriteControl: Boolean) {
        val bbOut = AdvancedByteBuffer(8, byteOrder) // хватит и 6 байт, но для кеширования сделаем 8

        //--- отправка первоначальной команды (на получения deviceID)
        //--- packetID у меня не используется, на всякий случай отправляю commandCode
        PacketHeader(cmd, 0, cmd, 0).write(bbOut)

        send(bbOut, withWriteControl)
    }

    private fun send(bbOut: AdvancedByteBuffer, withWriteControl: Boolean) {

        if (withWriteControl) outBuf(bbOut)
        else {
            bbOut.flip()
            clqOut.offer(DataMessage(byteBuffer = bbOut))
            dataServer.putForWrite(this)
        }
    }

    private fun sendDelay() {
        var delay: Int? = chmDeviceDelay[serialNo]
        delay = if (delay == null) 1 else delay + 1
        chmDeviceDelay[serialNo] = delay

        //--- не более чем на 540 мин (9 часов), чтобы секундами не переполнить signed short в минус
        val second = min(delay, 540) * 60 + 0x04    // в качестве причины указываем "Нет регистрации в Мск"

        val bbOut = AdvancedByteBuffer(6 + 2, byteOrder)

        val ph = PacketHeader(CMD_SEND_DELAY, 0, CMD_SEND_DELAY, 2)
        ph.write(bbOut)
        //--- дополняем данными для последующего вычисления CRC
        bbOut.putShort(second.toShort())
        bbOut.putShort(crc16_modbus(bbOut.array(), bbOut.arrayOffset(), 4 + ph.packetSize, false).toShort())

        send(bbOut, false)
    }

    private fun sendReadCoords() {
        //--- вместо recordStart используем общий dataCount
        val recordReadCount = if (dataCount - 1 + RECORD_PAGE_SIZE < recordCount) RECORD_PAGE_SIZE else recordCount - (dataCount - 1)

        val bbOut = AdvancedByteBuffer(16, byteOrder)    // для работы хватит 10 байт, но мы сделаем 16 для кеширования

        val ph = PacketHeader(CMD_READ_COORDS, 0, CMD_READ_COORDS, 4)
        ph.write(bbOut)
        //--- дополняем данными для последующего вычисления CRC
        bbOut.putShort((dataCount - 1).toShort())
        bbOut.putShort(recordReadCount.toShort())
        bbOut.putShort(crc16_modbus(bbOut.array(), bbOut.arrayOffset(), 4 + ph.packetSize, false).toShort())

        send(bbOut, true)

        dataCount += recordReadCount
        dataCountAll += recordReadCount
    }

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

//    private fun getIntegerValue( hmConfig: HashMap<String, String>, paramName: String, defaultValue: Int ): Int {
//        var value = defaultValue
//        try {
//            value = Integer.parseInt( hmConfig[ paramName ] )
//        }
//        catch( nfe: NumberFormatException ) {
//            AdvancedLogger.error(StringBuilder("Send Config: wrong value for ").append(paramName).toString())
//        }
//
//        return value
//    }
//
//    private fun putZeroEndedString(s: String, maxLen: Int, bb: AdvancedByteBuffer) {
//        var s = s
//        s = s.substring(0, Math.min(maxLen, s.length))
//        bb.put(s.toByteArray())
//        for(i in s.length until maxLen) bb.putByte(0.toByte())
//    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private class PacketHeader {

        companion object {
            val PACKET_SIGNATURE = 3
        }

        var command: Int = 0    // 4 bit
        var error: Int = 0      // 1 bit
        var signature: Int = 0  // 3 bit
        var packetID: Int = 0   // byte 0..255
        var packetSize: Int = 0 // short

        internal constructor(byteBuffer: AdvancedByteBuffer) {
            val b = byteBuffer.getByte().toInt()

            command = b and 15
            error = b and 16 shr 4
            signature = b and 224 shr 5

            packetID = byteBuffer.getByte().toInt()
            packetSize = byteBuffer.getShort().toInt()
        }

        internal constructor(aCommand: Int, aError: Int, aPacketID: Int, aPacketSize: Int) {
            command = aCommand
            error = aError
            signature = PACKET_SIGNATURE
            packetID = aPacketID
            packetSize = aPacketSize
        }

        //--- запись команды с дополнительными данными, но без CRC
        fun write(byteBuffer: AdvancedByteBuffer) {
            val b = signature shl 5 or (error shl 4) or command

            byteBuffer.putByte(b.toByte())
            byteBuffer.putByte(packetID.toByte())
            byteBuffer.putShort(packetSize.toShort())

            if (packetSize == 0) byteBuffer.putShort(crc16_modbus(byteBuffer.array(), byteBuffer.arrayOffset(), 4, false).toShort())
        }

    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private class PLAPoint constructor(aPLAHandler: PLAHandler, byteBuffer: AdvancedByteBuffer) {

        companion object {
            private val STATUS_WRONG_COORD = 0x0001    // Неправильные координаты
            private val STATUS_GPS_FAIL = 0x0002       // Отказ GPS
            private val STATUS_PARKING = 0x0004        // Стоянка
            private val STATUS_EW_COORD = 0x4000       // E/W позиция
            private val STATUS_NS_COORD = 0x8000       // N/S позиция

            private val ANALOG_INPUT_COUNT = 8
        }

        var time = 0

        var wgsX = 0
        var wgsY = 0
        var speed: Short = 0
        var dist = 0

        var d = 0
        var tmA = TreeMap<Int, Int>()

        init {
            time = byteBuffer.getInt()

            //--- сначала долгота (X), потом широта (Y) в формате 179^44.1234'
            val gpsX = byteBuffer.getInt()
            val gpsY = byteBuffer.getInt()
            //--- преобразование наших ёбнутых координат, скоростей и дистанций
            val tmpX = gpsX / 1000000
            val tmpY = gpsY / 1000000
            wgsX = ((tmpX + (gpsX - tmpX * 1000000).toDouble() / 10_000.0 / 60.0) * XyProjection.WGS_KOEF_i).toInt()
            wgsY = ((tmpY + (gpsY - tmpY * 1000000).toDouble() / 10_000.0 / 60.0) * XyProjection.WGS_KOEF_i).toInt()

            //--- скорость в формате xxx.xx [миль/час==узлы]
            speed = aPLAHandler.roundSpeed((byteBuffer.getShort().toInt() and 0xFFFF) / 100.0 * 1.852)

            //if( speed < 0 ) speed = 0;
            //if( speed > 255 ) speed = 255;
            //--- дистанция - в милях, переводим в метры
            dist = round(byteBuffer.getFloat().toDouble() * 1.852 * 1000.0).toInt()

            byteBuffer.getShort()  // SKIP azimuth
            var status = byteBuffer.getShort().toInt() and 0xFFFF
            val event = byteBuffer.getByte().toInt() and 0xFF

            d = byteBuffer.getByte().toInt() and 0xFF
            for (i in 0 until ANALOG_INPUT_COUNT) {
                tmA.put(i, byteBuffer.getShort().toInt() and 0xFFFF)
            }

            //--- дополнительная обработка данных по полю status ---

            //--- нулевые координаты указывают на ошибку GPS
            if (gpsX == 0 && gpsY == 0) status = status or STATUS_WRONG_COORD
            //--- при неправильных GPS-данных всё = 0
            if (status and (STATUS_WRONG_COORD or STATUS_GPS_FAIL) != 0) {
                dist = 0
                wgsY = dist
                wgsX = wgsY
                speed = 0.toShort()
            } else {
                //--- пробег на событиях не имеет значения
                if (event != 0) dist = 0
                //--- на стоянке скорость = 0
                if (status and STATUS_PARKING != 0) speed = 0
                //--- установка знака координат в зависимости от статуса
                if (status and STATUS_NS_COORD == 0) wgsX = -wgsX
                if (status and STATUS_EW_COORD == 0) wgsY = -wgsY
            }
        }
    }

}
