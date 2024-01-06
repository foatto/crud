package foatto.core_server.ds

import foatto.util.AdvancedByteBuffer
import foatto.util.DateTime_YMDHMS
import foatto.util.getCurrentTimeInt
import foatto.util.getFileWriter
import foatto.sql.CoreAdvancedConnection
import java.io.File
import java.time.ZoneId
import java.util.*

object CoreTelematicFunction {
    //--- учитывая возможность подключения нескольких контроллеров к одному объекту,
    //--- каждому контроллеру дадим по 1000 портов
    val MAX_PORT_PER_DEVICE = 1000

    //--- ограничения по приему данных из будущего и прошлого:
    //--- не более чем за сутки из будущего и не более года из прошлого
    val MAX_FUTURE_TIME = 24 * 60 * 60
    val MAX_PAST_TIME = 365 * 24 * 60 * 60

    //----------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun putBitSensor(
        deviceIndex: Int,
        bitValue: Int,
        startPortNum: Int,
        sensorCount: Int,
        bbData: AdvancedByteBuffer,
        pointTime: Int,
        hmPortTime: MutableMap<Int, Int>,
    ) {
        for (i in 0 until sensorCount) {
            putSensorData(
                deviceIndex = deviceIndex,
                portNum = startPortNum + i,
                dataSize = 1,
                dataValue = bitValue.ushr(i) and 0x1,
                bbData = bbData,
                pointTime = pointTime,
                hmPortTime = hmPortTime,
            )
        }
    }

    fun putDigitalSensor(
        deviceIndex: Int,
        tmDigitalSensor: SortedMap<Int, Int>,
        startPortNum: Int,
        sensorDataSize: Int,
        bbData: AdvancedByteBuffer,
        pointTime: Int,
        hmPortTime: MutableMap<Int, Int>,
    ) {
        tmDigitalSensor.forEach { (index, value) ->
            putSensorData(
                deviceIndex = deviceIndex,
                portNum = startPortNum + index,
                dataSize = sensorDataSize,
                dataValue = value,
                bbData = bbData,
                pointTime = pointTime,
                hmPortTime = hmPortTime,
            )
        }
    }

    fun putDigitalSensor(
        deviceIndex: Int,
        tmDigitalSensor: SortedMap<Int, Double>,
        startPortNum: Int,
        bbData: AdvancedByteBuffer,
        pointTime: Int,
        hmPortTime: MutableMap<Int, Int>,
    ) {
        tmDigitalSensor.forEach { (index, value) ->
            putSensorData(
                deviceIndex = deviceIndex,
                portNum = startPortNum + index,
                dataValue = value,
                bbData = bbData,
                pointTime = pointTime,
                hmPortTime = hmPortTime,
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
        pointTime: Int,
        hmPortTime: MutableMap<Int, Int>,
    ) {
        putSensorPortNumAndDataSize(
            deviceIndex = deviceIndex,
            portNum = portNum,
            dataSize = dataSize,
            bbData = bbData,
            pointTime = pointTime,
            hmPortTime = hmPortTime,
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
        pointTime: Int,
        hmPortTime: MutableMap<Int, Int>,
    ) {
        //--- не будем хранить float в 4-х байтах, т.к. это будет путаться с 4-байтовым int'ом
        putSensorPortNumAndDataSize(
            deviceIndex = deviceIndex,
            portNum = portNum,
            dataSize = 8,
            bbData = bbData,
            pointTime = pointTime,
            hmPortTime = hmPortTime,
        )

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

    fun putSensorPortNumAndDataSize(
        deviceIndex: Int,
        portNum: Int,
        dataSize: Int,
        bbData: AdvancedByteBuffer,
        pointTime: Int,
        hmPortTime: MutableMap<Int, Int>,
    ) {
        val pn = deviceIndex * MAX_PORT_PER_DEVICE + portNum
        bbData.putShort(pn).putShort(dataSize - 1)
        hmPortTime[pn] = pointTime
    }

    fun updatePortTime(
        conn: CoreAdvancedConnection,
        objectId: Int,
        hmPortTime: MutableMap<Int, Int>,
    ) {
        val hmTimePorts = mutableMapOf<Int, MutableList<Int>>()
        hmPortTime.forEach { (portNum, time) ->
            val alPorts = hmTimePorts.getOrPut(time) {
                mutableListOf()
            }
            alPorts += portNum
        }
        hmTimePorts.forEach { (time, alPorts) ->
            conn.executeUpdate(
                """
                    UPDATE MMS_sensor 
                    SET raw_time = $time
                    WHERE object_id = $objectId
                    AND port_num IN ( ${alPorts.joinToString(" , ")} )
                """
            )
        }
    }

    fun writeJournal(
        dirJournalLog: File,
        zoneId: ZoneId,
        address: String,
        errorText: String,
    ) {
        //--- какое д.б. имя лог-файла для текущего дня и часа
        val logTime = DateTime_YMDHMS(zoneId, getCurrentTimeInt())
        val curLogFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

        val out = getFileWriter(File(dirJournalLog, curLogFileName), true)
        out.write("$logTime $address $errorText")
        out.newLine()
        out.flush()
        out.close()
    }
}