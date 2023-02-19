package foatto.ts.core_ts.ds

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.sql.SQLBatch
import foatto.ts_core.app.CommandStatusCode
import java.nio.ByteOrder
import java.time.OffsetDateTime
import java.time.ZoneOffset

class UDSHandler : TSHandler() {

    companion object {
        val UDS_RAW_PACKET_TIME_BASE = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond().toInt()
    }

    private val objectMapper = jacksonObjectMapper()

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
            loadDeviceConfig(dataWorker)

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

        //--- ищем последнюю команду на отправку, независимо от статуса
        val rs = dataWorker.conn.executeQuery(
            """
                SELECT id , send_status , command
                FROM TS_device_command_history
                WHERE device_id = ${deviceConfig!!.deviceId}
                ORDER BY create_time DESC
            """
        )
        var commandId = 0
        var sendStatus = true   // default (for 'not founded', for example, as sended already
        var command = ""
        if (rs.next()) {
            commandId = rs.getInt(1)
            sendStatus = rs.getInt(2) != 0
            command = rs.getString(3)
        }
        rs.close()
        //--- если команда ещё не отправлена - работаем дальше.
        //--- независимо от этого все предыдущие неотправленные команды игнорируем как устаревшие
        if (!sendStatus) {
            val commandData = CommandData(
                id = serialNo.toInt(),
                state = command,
            )
            val commandDataString = objectMapper.writeValueAsString(commandData)
            AdvancedLogger.debug("commandDataString = '$commandDataString'")
            //--- send command
            val bbOut = AdvancedByteBuffer(commandDataString.length, byteOrder)
            bbOut.put(commandDataString.toByteArray())
            outBuf(bbOut)
            //--- write send status & time
            dataWorker.conn.executeUpdate(
                """
                    UPDATE TS_device_command_history 
                    SET send_status = ${CommandStatusCode.SENDED} ,
                        send_time = ${getCurrentTimeInt()}
                    WHERE id = $commandId
                """
            )
            status += " Command Send;"
            //--- mark other unused/non-sended commands as 'deleted'
            dataWorker.conn.executeUpdate(
                """
                    UPDATE TS_device_command_history 
                    SET send_status = ${CommandStatusCode.DELETED} 
                    WHERE send_status = ${CommandStatusCode.NOT_SENDED}
                """
            )
        }

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

    private fun dataToByteBuffer(dataWorker: CoreNioWorker, udsDataPacket: UDSDataPacket): AdvancedByteBuffer {
        val bbData = AdvancedByteBuffer(dataWorker.conn.dialect.textFieldMaxSize / 2)

        // Код текущего состояния установки УДС
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 0, 4, udsDataPacket.state, bbData)
        // Глубина (в метрах)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 1, udsDataPacket.depth, bbData)
        // Скорость спуска (метров/час)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 2, udsDataPacket.speed, bbData)
        // Нагрузка на привод (%)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 3, udsDataPacket.load, bbData)
        // Дата и время начала следующей чистки
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 4, 4, udsDataPacket.nextCleanindDateTime, bbData)
        // Глубина чистки (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 5, udsDataPacket.cleaningDepth, bbData)
        // Период чистки (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 6, 4, udsDataPacket.cleaningPeriod, bbData)
        // Скорость чистки (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 7, udsDataPacket.cleaningSpeed, bbData)
        // Уровень сигнала сотовой связи
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 8, udsDataPacket.signalLevel, bbData)
        // Счётчик количества перезагрузок модема
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 9, 4, udsDataPacket.modemRestartCount, bbData)
        // Ограничение нагрузки на привод (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 10, udsDataPacket.driveLoadRestrict, bbData)
        // Строка с содержимым результата AT-команды запроса баланса (если баланс не запрашивался, указывается “-1”)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 11, udsDataPacket.balance, bbData)
        // Глубина парковки скребка (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 12, udsDataPacket.parkDepth, bbData)
        // Количество попыток прохода препятствия (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 13, 4, udsDataPacket.passAttempt, bbData)
        // Синхронизация с запуском ЭЦН (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 14, 1, if (udsDataPacket.ecnRunSync) 1 else 0, bbData)
        // Пауза между проходами препятствия (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 15, 4, udsDataPacket.passDelay, bbData)
        // Текущая темп. внутри станции УДС (29,3˚С) (реле №1)
        udsDataPacket.t1Current?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 16, udsDataPacket.t1Current, bbData)
        }
        // Текущая темп. на улице (23,4 ˚С) (реле №2)
        udsDataPacket.t2Current?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 17, udsDataPacket.t2Current, bbData)
        }
        // Уровень температуры внутри (параметр настройки)
        udsDataPacket.t1Setup?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 18, udsDataPacket.t1Setup, bbData)
        }
        // Уровень температуры снаружи (параметр настройки)
        udsDataPacket.t2Setup?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 19, udsDataPacket.t2Setup, bbData)
        }
        // Работает или не работает реле №1 (температура)
        udsDataPacket.t1State?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 20, 1, if (udsDataPacket.t1State) 1 else 0, bbData)
        }
        // Работает или не работает реле №2 (температура)
        udsDataPacket.t2State?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 21, 1, if (udsDataPacket.t2State) 1 else 0, bbData)
        }
        // Работает или не работает канал №1
        udsDataPacket.ch1State?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 22, 1, if (udsDataPacket.ch1State) 1 else 0, bbData)
        }
        // Работает или не работает канал №2
        udsDataPacket.ch2State?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 23, 1, if (udsDataPacket.ch2State) 1 else 0, bbData)
        }
        // Работает или не работает модуль ТРМ (общее управление температурными датчиками)
        udsDataPacket.tpmState?.let {
            CoreTelematicFunction.putSensorData(deviceConfig!!.index, 24, 1, if (udsDataPacket.tpmState) 1 else 0, bbData)
        }

        return bbData
    }

}

private class CommandData(
    val id: Int,
    val state: String,
)