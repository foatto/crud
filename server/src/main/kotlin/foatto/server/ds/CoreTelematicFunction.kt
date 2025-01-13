package foatto.server.ds

import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeYMDHMSString
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.getFileWriter
import kotlinx.datetime.TimeZone
import java.io.File
import java.util.*
import kotlin.math.min

object CoreTelematicFunction {
    //--- учитывая возможность подключения нескольких контроллеров к одному объекту,
    //--- каждому контроллеру дадим по 1000 портов
    val MAX_PORT_PER_DEVICE: Int = 1000

    //--- ограничения по приему данных из будущего и прошлого:
    //--- не более чем за сутки из будущего и не более года из прошлого
    val MAX_FUTURE_TIME: Int = 24 * 60 * 60
    val MAX_PAST_TIME: Int = 365 * 24 * 60 * 60

    //----------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun putBitSensorData(
        deviceIndex: Int,
        bitValue: Int,
        startPortNum: Int,
        sensorCount: Int,
        bbData: AdvancedByteBuffer,
    ) {
        for (i in 0 until sensorCount) {
            putSensorData(
                deviceIndex = deviceIndex,
                portNum = startPortNum + i,
                dataSize = 1,
                dataValue = bitValue.ushr(i) and 0x1,
                bbData = bbData,
            )
        }
    }

    fun putDigitalSensors(
        deviceIndex: Int,
        tmDigitalSensor: SortedMap<Int, Int>,
        startPortNum: Int,
        sensorDataSize: Int,
        bbData: AdvancedByteBuffer,
    ) {
        tmDigitalSensor.forEach { (index, value) ->
            putSensorData(
                deviceIndex = deviceIndex,
                portNum = startPortNum + index,
                dataSize = sensorDataSize,
                dataValue = value,
                bbData = bbData,
            )
        }
    }

    fun putDigitalSensors(
        deviceIndex: Int,
        tmDigitalSensor: SortedMap<Int, Double>,
        startPortNum: Int,
        bbData: AdvancedByteBuffer,
    ) {
        tmDigitalSensor.forEach { (index, value) ->
            putSensorData(
                deviceIndex = deviceIndex,
                portNum = startPortNum + index,
                dataValue = value,
                bbData = bbData,
            )
        }
    }

    //!!! ещё не мешало бы проверить корректность записи данных
    //    protected void putDigitalSensor( int[][] arrDigitalSensor, int startPortNum, int sensorDataSize, AdvancedByteBuffer bbData ) throws Throwable {
    //        for( int i = 0; i < arrDigitalSensor.length; i++ )
    //            putSensorData( startPortNum + i, sensorDataSize, arrDigitalSensor[ i ], bbData );
    //    }

    fun putSensorData(
        deviceIndex: Int,
        portNum: Int,
        dataSize: Int,
        dataValue: Int,
        bbData: AdvancedByteBuffer,
    ) {
        putSensorPortNumAndDataSize(
            deviceIndex = deviceIndex,
            portNum = portNum,
            dataSize = dataSize,
            bbData = bbData,
        )

        when (dataSize) {
            1 -> bbData.putByte(dataValue)
            2 -> bbData.putShort(dataValue)
            3 -> bbData.putInt3(dataValue)
            4 -> bbData.putInt(dataValue)
            else -> throw Exception("AbstractTelematicHandler.putSensorData: wrong data size = $dataSize")
        }
    }

    fun putSensorData(
        deviceIndex: Int,
        portNum: Int,
        dataValue: Double,
        bbData: AdvancedByteBuffer,
    ) {
        //--- не будем хранить float в 4-х байтах, т.к. это будет путаться с 4-байтовым int'ом
        putSensorPortNumAndDataSize(
            deviceIndex = deviceIndex,
            portNum = portNum,
            dataSize = 8,
            bbData = bbData,
        )

        bbData.putDouble(dataValue)
    }

    fun putStringSensorData(
        deviceIndex: Int,
        portNum: Int,
        stringValue: String,
        bbData: AdvancedByteBuffer,
    ) {
        //--- чтобы не путать короткие строки с 1-2-3-4-8-байтовыми числами,
        //--- запишем длину строки в виде отрицательного числа (т.е. как флаг строковых данных)
        val len = min(stringValue.length, 32_000)   // dataSize as Short
        val dataSize = 2 + len * 2

        putSensorPortNumAndDataSize(
            deviceIndex = deviceIndex,
            portNum = portNum,
            dataSize = -dataSize,
            bbData = bbData,
        )

        bbData.putShortString(stringValue.subSequence(0, len))
    }

    fun putSensorPortNumAndDataSize(
        deviceIndex: Int,
        portNum: Int,
        dataSize: Int,
        bbData: AdvancedByteBuffer,
    ) {
        val pn = deviceIndex * MAX_PORT_PER_DEVICE + portNum
        bbData.putShort(pn).putShort(dataSize - 1)
    }

    fun writeJournal(
        dirJournalLog: File,
        timeZone: TimeZone,
        address: String,
        errorText: String,
    ) {
        //--- какое д.б. имя лог-файла для текущего дня и часа
        val logTime = getDateTimeYMDHMSString(timeZone, getCurrentTimeInt())
        val curLogFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

        val out = getFileWriter(File(dirJournalLog, curLogFileName), true)
        out.write("$logTime $address $errorText")
        out.newLine()
        out.flush()
        out.close()
    }
}