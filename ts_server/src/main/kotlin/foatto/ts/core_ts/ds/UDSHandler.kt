package foatto.ts.core_ts.ds

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.YMDHMS_DateTime
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeInt
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.sql.SQLBatch
import java.nio.ByteOrder

class UDSHandler : TSHandler() {

    // private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
    private val objectMapper = jacksonObjectMapper()

    private val sbData = StringBuilder()

    //--- в общем-то всё равно, т.к. принимаются/передаются передаются строки,
    //--- но при передаче упакованных данных может пригодится
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork(dataWorker: CoreNioWorker): Boolean {
        //        //--- данных нет - ждём
        //        if( ! bbIn.hasRemaining() ) {
        //            bbIn.compact();
        //            return true;
        //        }

        val arrByte = ByteArray(bbIn.remaining())
        bbIn.get(arrByte)
        sbData.append(String(arrByte))

//        //--- минимальный размер пакета = 5 символов/байт
//        if( sbData.length < 5 ) {
//            bbIn.compact()
//            return true
//        }

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

            serialNo = udsRawPacket.udsId
            loadDeviceConfig(dataWorker)

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
            packetEndPos = sbData.indexOf("\r\n")
            if (packetEndPos < 0) {
                break
            }
        }
        sqlBatchData.execute(dataWorker.conn)

//        //--- ищем последнюю команду на отправку, независимо от статуса
//        val rs = dataWorker.conn.executeQuery(
//            """
//                SELECT id , send_status , command
//                FROM TS_device_command_history
//                WHERE device_id = ${deviceConfig!!.deviceId}
//                ORDER BY create_time DESC
//            """
//        )
//        var commandId = 0
//        var sendStatus = true   // default (for 'not founded', for example0 as sended already
//        var command = ""
//        if (rs.next()) {
//            commandId = rs.getInt(1)
//            sendStatus = rs.getInt(2) != 0
//            command = rs.getString(3)
//        }
//        rs.close()
//        //--- если команда ещё не отправлена - работаем дальше.
//        //--- независимо от этого все предыдущие неотправленные команды игнориуем как устаревшие
//        val commandData = CommandData(
//            id = serialNo.toInt(),
//            state = if (!sendStatus) {
//                command
//            } else {
//                ""
//            },
//        )
//        val commandDataString = objectMapper.writeValueAsString(commandData)
//AdvancedLogger.debug("commandDataString = '$commandDataString'")
//        //--- send command
//        val bbOut = AdvancedByteBuffer(commandDataString.length, byteOrder)
//        bbOut.put(commandDataString.toByteArray())
//        outBuf(bbOut)
//        //--- write send status & time
//        if(!sendStatus) {
//            dataWorker.conn.executeUpdate(
//                """
//                    UPDATE TS_device_command_history SET
//                    send_status = 1 ,
//                    send_time = ${getCurrentTimeInt()}
//                    WHERE id = $commandId
//                """
//            )
//            status += " Command Send;"
//        }

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
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 9, 4, udsDataPacket.mrc, bbData)
        // Ограничение нагрузки на привод (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 10, udsDataPacket.driveLoadRestrict, bbData)
        // Строка с содержимым результата AT-команды запроса баланса (если баланс не запрашивался, указывается “-1”)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 11, udsDataPacket.balance, bbData)
        // Глубина парковки скребка (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 12, udsDataPacket.parkDepth, bbData)
        // Количество попыток прохода препятствия (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 13, 4, udsDataPacket.passAttempt, bbData)
        // Синхронизация с запуском ЭЦН (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 14, 4, udsDataPacket.ecnStart, bbData)
        // Пауза между проходами препятствия (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 15, 4, udsDataPacket.passDelay, bbData)
        // Текущая темп. внутри станции УДС (29,3˚С) (реле №1)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 16, udsDataPacket.temp1, bbData)
        // Текущая темп. на улице (23,4 ˚С) (реле №2)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 17, udsDataPacket.temp2, bbData)
        // Уровень температуры внутри (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 18, udsDataPacket.setPoint1, bbData)
        // Уровень температуры снаружи (параметр настройки)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 19, udsDataPacket.setPoint2, bbData)
        // Работает или не работает реле №1 (температура)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 20, 1, if (udsDataPacket.relay1State) 1 else 0, bbData)
        // Работает или не работает реле №2 (температура)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 21, 1, if (udsDataPacket.relay2State) 1 else 0, bbData)
        // Работает или не работает канал №1
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 22, 1, if (udsDataPacket.channel1Error) 1 else 0, bbData)
        // Работает или не работает канал №2
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 23, 1, if (udsDataPacket.channel2Error) 1 else 0, bbData)
        // Работает или не работает модуль ТРМ (общее управление температурными датчиками)
        CoreTelematicFunction.putSensorData(deviceConfig!!.index, 24, 1, if (udsDataPacket.trmFail) 1 else 0, bbData)

        return bbData
    }

}

private class CommandData(
    val id: Int,
    val pass: String = "bb9ec852de3e8f7609d3676ede4444fa",  //--- for compatibility, removed later
    val state: String,
    //--- for compatibility, removed later
    val crc16L: Int = 0,
    val crc16H: Int = 0,
)