package foatto.core_server.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getFileWriter
import java.io.File
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.time.ZoneId
import java.util.*

abstract class AbstractTelematicHandler : AbstractHandler() {

    //--- учитывая возможность подключения нескольких контроллеров к одному объекту,
    //--- каждому контроллеру дадим по 1000 портов
    companion object {
        val MAX_PORT_PER_DEVICE = 1000
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- ограничения по приему данных из будущего и прошлого:
    //--- не более чем за сутки из будущего и не более года из прошлого
    val MAX_FUTURE_TIME = 24 * 60 * 60
    val MAX_PAST_TIME = 365 * 24 * 60 * 60

    //--- 1000 секунд = примерно 16-17 мин
    protected val DEVICE_CONFIG_OUT_PERIOD = 1_000

    protected var zoneId = ZoneId.systemDefault()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override var startBufSize: Int = 1024

    protected abstract val configSessionLogPath: String
    protected abstract val configJournalLogPath: String

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected var lastDeviceConfigOutTime = getCurrentTimeInt()

    protected lateinit var dirSessionLog: File
    protected lateinit var dirJournalLog: File

    //--- тип прибора - должен переопределяться в наследниках
    protected var deviceType = -1

    //--- время начала сессии
    protected var begTime = 0

    //--- запись состояния сессии
    protected var sbStatus = StringBuilder()

    //--- текст ошибки
    protected var errorText: String? = null

    //--- количество записанных блоков данных (например, точек)
    protected var dataCount = 0

    //--- количество считанных блоков данных (например, точек)
    protected var dataCountAll = 0

    //--- время первого и последнего блока данных (например, точки)
    protected var firstPointTime = 0
    protected var lastPointTime = 0

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aDataServer: CoreDataServer, aSelectionKey: SelectionKey) {
        super.init(aDataServer, aSelectionKey)

        dirSessionLog = File(dataServer.hmConfig[configSessionLogPath]!!)
        dirJournalLog = File(dataServer.hmConfig[configJournalLogPath]!!)

        begTime = getCurrentTimeInt()
        sbStatus.append("Init;")
    }

    override fun work(dataWorker: CoreDataWorker): Boolean {
        if (begTime == 0) {
            begTime = getCurrentTimeInt()
        }

        return super.work(dataWorker)
    }

    override fun preWork() {
        sbStatus.append("Start;")
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun writeJournal() {
        //--- какое д.б. имя лог-файла для текущего дня и часа
        val logTime = DateTime_YMDHMS(zoneId, getCurrentTimeInt())
        val curLogFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

        val out = getFileWriter(File(dirJournalLog, curLogFileName), true)
        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
        //--- поэтому используем более общий метод SocketChannel.socket().getLocalAddress()
        out.write("$logTime ${(selectionKey!!.channel() as SocketChannel).socket().localAddress} $errorText")
        out.newLine()
        out.flush()
        out.close()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

     protected fun putBitSensor(bitValue: Int, startPortNum: Int, sensorCount: Int, bbData: AdvancedByteBuffer) {
        for (i in 0 until sensorCount) {
            putSensorData(startPortNum + i, 1, bitValue.ushr(i) and 0x1, bbData)
        }
    }

    protected fun putDigitalSensor(tmDigitalSensor: SortedMap<Int, Int>, startPortNum: Int, sensorDataSize: Int, bbData: AdvancedByteBuffer) {
        tmDigitalSensor.forEach { (index, value) ->
            putSensorData(startPortNum + index, sensorDataSize, value, bbData)
        }
    }

    protected fun putDigitalSensor(tmDigitalSensor: SortedMap<Int, Double>, startPortNum: Int, bbData: AdvancedByteBuffer) {
        tmDigitalSensor.forEach { (index, value) ->
            putSensorData(startPortNum + index, value, bbData)
        }
    }

    //!!! ещё не мешало бы проверить корректность записи данных
    //    protected void putDigitalSensor( int[][] arrDigitalSensor, int startPortNum, int sensorDataSize, AdvancedByteBuffer bbData ) throws Throwable {
    //        for( int i = 0; i < arrDigitalSensor.length; i++ )
    //            putSensorData( startPortNum + i, sensorDataSize, arrDigitalSensor[ i ], bbData );
    //    }

    protected fun putSensorData(portNum: Int, dataSize: Int, dataValue: Int, bbData: AdvancedByteBuffer) {
        putSensorPortNumAndDataSize(portNum, dataSize, bbData)

        when(dataSize) {
            1 -> bbData.putByte(dataValue)
            2 -> bbData.putShort(dataValue)
            3 -> bbData.putInt3(dataValue)
            4 -> bbData.putInt(dataValue)
            else -> throw Exception("AbstractTelematicHandler.putSensorData: wrong data size = $dataSize")
        }
    }

    protected fun putSensorData(portNum: Int, dataValue: Double, bbData: AdvancedByteBuffer) {
        //--- не будем хранить float в 4-х байтах, т.к. это будет путаться с 4-байтовым int'ом
        putSensorPortNumAndDataSize(portNum, 8, bbData)

        bbData.putDouble(dataValue)
    }
    //!!! ещё не мешало бы проверить корректность записи данных
    //    protected void putSensorData( int portNum, int dataSize, int[] arrDataValue, AdvancedByteBuffer bbData ) throws Throwable {
    //        putSensorPortNumAndDataSize( portNum, dataSize, bbData );
    //
    //        for( int i = 0; i < arrDataValue.length; i++ )
    //                 if( dataSize == 1 ) bbData.putByte( arrDataValue[ i ] );
    //            else if( dataSize == 2 ) bbData.putShort( arrDataValue[ i ] );
    //            else if( dataSize == 3 ) bbData.putInt3( arrDataValue[ i ] );
    //            else if( dataSize == 4 ) bbData.putInt( arrDataValue[ i ] );
    //    }

    protected abstract fun putSensorPortNumAndDataSize(portNum: Int, dataSize: Int, bbData: AdvancedByteBuffer)
}