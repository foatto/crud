package foatto.ts.core_ts.ds

import com.fasterxml.jackson.module.kotlin.readValue
import foatto.core.util.AdvancedLogger
import foatto.core.util.YMDHMS_DateTime
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeInt
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.sql.SQLBatch
import java.nio.ByteOrder

class OldUDSTechnoHandler : CommonUDSHandler() {

    private val sbData = StringBuilder()

    //--- в общем-то всё равно, т.к. принимаются/передаются передаются строки,
    //--- но при передаче упакованных данных может пригодится
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork(dataWorker: CoreNioWorker): Boolean {

        val arrByte = ByteArray(bbIn.remaining())
        bbIn.get(arrByte)
        sbData.append(String(arrByte))
//AdvancedLogger.debug("sbData = '$sbData'")

        //--- хотя бы один пакет собрался в общей строке данных?
        var packetEndPos = sbData.indexOf('}')
        if (packetEndPos < 0) {
            bbIn.compact()
            return true
        }

        val sqlBatchData = SQLBatch()
        while (true) {
            //--- последняя рабочая '}' тоже входит в рабочую строку
            packetEndPos++

            val strPacket = sbData.substring(0, packetEndPos)
            AdvancedLogger.debug("data = $strPacket")

            //--- убираем разобранный пакет из начала общей строки данных
            sbData.delete(0, packetEndPos)

            val udsRawPacket: OldUDSTechnoRawPacket = objectMapper.readValue(strPacket)
            status += " DataRead;"

            serialNo = udsRawPacket.udsId
            loadDeviceConfig(dataWorker, DEVICE_TYPE_UDS_OLD)

            val pointTime = getDateTimeInt(YMDHMS_DateTime(zoneId, udsRawPacket.datetime))

            //--- если объект прописан, то записываем точки, иначе просто пропускаем
            //--- также пропускаем точки из будущего и далёкого прошлого
            val curTime = getCurrentTimeInt()
            if (deviceConfig?.objectId != 0 && pointTime > curTime - CoreTelematicFunction.MAX_PAST_TIME && pointTime < curTime + CoreTelematicFunction.MAX_FUTURE_TIME) {
                fwVersion = udsRawPacket.version ?: ""

                val udsDataPacket = udsRawPacket.normalize(zoneId)
                val bbData = dataToByteBuffer(dataWorker, udsDataPacket)

                addPoint(dataWorker.conn, pointTime, bbData, sqlBatchData)
                dataCount++

                if (firstPointTime == 0) {
                    firstPointTime = pointTime
                }
                lastPointTime = pointTime
            }
            dataCountAll++

            //--- есть ли ещё данные в пакете
            packetEndPos = sbData.indexOf('}')
            if (packetEndPos < 0) {
                break
            }
        }
        sqlBatchData.execute(dataWorker.conn)

        sendCommand(dataWorker)

        //--- данные успешно переданы - теперь можно завершить транзакцию
        status += " Ok;"
        errorText = ""
        writeSession(dataWorker.conn, true)

        //--- для возможного режима постоянного/длительного соединения
        bbIn.compact()     // нельзя .clear(), т.к. копятся данные следующего пакета

        begTime = 0
        status = ""
        dataCount = 0
        dataCountAll = 0
        firstPointTime = 0
        lastPointTime = 0

        //--- это накопительная строка данных, очищается в процессе разбора
        //sbData.setLength( 0 );
        return true
    }

    override fun getUDSCommandDataString(command: String): String {
        val commandData = OldUDSTechnoCommandData(
            id = serialNo.toInt(),
            state = command,
        )
        return objectMapper.writeValueAsString(commandData)
    }

}

private class OldUDSTechnoCommandData(
    val id: Int,
    val pass: String = "bb9ec852de3e8f7609d3676ede4444fa",  //--- for compatibility, removed later
    val state: String,
    //--- for compatibility, not used, removed later
    val crc16L: Int = 0,
    val crc16H: Int = 0,
)
