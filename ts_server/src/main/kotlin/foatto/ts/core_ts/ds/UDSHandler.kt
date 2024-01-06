package foatto.ts.core_ts.ds

import com.fasterxml.jackson.module.kotlin.readValue
import foatto.util.AdvancedLogger
import foatto.util.getCurrentTimeInt
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.sql.SQLBatch
import java.nio.ByteOrder
import java.time.OffsetDateTime
import java.time.ZoneOffset

class UDSHandler : CommonUDSHandler() {

    companion object {
        val UDS_RAW_PACKET_TIME_BASE = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond().toInt()
    }

    private val sbData = StringBuilder()

    //--- в общем-то всё равно, т.к. принимаются/передаются передаются строки,
    //--- но при передаче упакованных данных может пригодится
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork(dataWorker: CoreNioWorker): Boolean {

        val arrByte = ByteArray(bbIn.remaining())
        bbIn.get(arrByte)
        sbData.append(String(arrByte))
//AdvancedLogger.debug("sbData = $sbData")

        //--- хотя бы один пакет собрался в общей строке данных?
        var packetEndPos = sbData.indexOf("\r\n")
        if (packetEndPos < 0) {
            bbIn.compact()
            return true
        }

        val sqlBatchData = SQLBatch()
        while (true) {
            val strPacket = sbData.substring(0, packetEndPos)
            AdvancedLogger.debug("data = $strPacket")

            //--- убираем разобранный пакет из начала общей строки данных
            sbData.delete(0, packetEndPos + 2)

            val udsRawPacket: UDSRawPacket = objectMapper.readValue(strPacket)
            status += " DataRead;"

            serialNo = udsRawPacket.id.toString()
            loadDeviceConfig(dataWorker, DEVICE_TYPE_UDS)

            val pointTime = UDS_RAW_PACKET_TIME_BASE + udsRawPacket.timeSys

            //--- если объект прописан, то записываем точки, иначе просто пропускаем
            //--- также пропускаем точки из будущего и далёкого прошлого
            val curTime = getCurrentTimeInt()
            if (deviceConfig?.objectId != 0 && pointTime > curTime - CoreTelematicFunction.MAX_PAST_TIME && pointTime < curTime + CoreTelematicFunction.MAX_FUTURE_TIME) {
                fwVersion = udsRawPacket.vers ?: ""
                imei = udsRawPacket.imei ?: ""

                val udsDataPacket = udsRawPacket.normalize()
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
            packetEndPos = sbData.indexOf("\r\n")
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
        val commandData = UDSCommandData(
            id = serialNo.toInt(),
            state = command,
        )
        return objectMapper.writeValueAsString(commandData)
    }
}

private class UDSCommandData(
    val id: Int,
    val state: String,
)