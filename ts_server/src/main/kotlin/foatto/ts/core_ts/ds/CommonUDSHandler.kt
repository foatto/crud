package foatto.ts.core_ts.ds

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import foatto.util.AdvancedByteBuffer
import foatto.util.AdvancedLogger
import foatto.util.getCurrentTimeInt
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.ts_core.app.CommandStatusCode

abstract class CommonUDSHandler : TSHandler() {

    protected val objectMapper = jacksonObjectMapper()

    protected fun dataToByteBuffer(dataWorker: CoreNioWorker, udsDataPacket: UDSDataPacket): AdvancedByteBuffer {
        val bbData = AdvancedByteBuffer(dataWorker.conn.dialect.textFieldMaxSize / 2)
        deviceConfig?.let { dc ->
            //!!! агрегации данных не производится
            val pointTime = 0
            val hmPortTime = mutableMapOf<Int, Int>()
            // Код текущего состояния установки УДС
            CoreTelematicFunction.putSensorData(dc.index, 0, 4, udsDataPacket.state, bbData, pointTime, hmPortTime)
            // Глубина (в метрах)
            CoreTelematicFunction.putSensorData(dc.index, 1, udsDataPacket.depth, bbData, pointTime, hmPortTime)
            // Скорость спуска (метров/час)
            CoreTelematicFunction.putSensorData(dc.index, 2, udsDataPacket.speed, bbData, pointTime, hmPortTime)
            // Нагрузка на привод (%)
            CoreTelematicFunction.putSensorData(dc.index, 3, udsDataPacket.load, bbData, pointTime, hmPortTime)
            // Дата и время начала следующей чистки
            CoreTelematicFunction.putSensorData(dc.index, 4, 4, udsDataPacket.nextCleanindDateTime, bbData, pointTime, hmPortTime)
            // Глубина чистки (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 5, udsDataPacket.cleaningDepth, bbData, pointTime, hmPortTime)
            // Период чистки (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 6, 4, udsDataPacket.cleaningPeriod, bbData, pointTime, hmPortTime)
            // Скорость чистки (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 7, udsDataPacket.cleaningSpeed, bbData, pointTime, hmPortTime)
            // Уровень сигнала сотовой связи
            CoreTelematicFunction.putSensorData(dc.index, 8, udsDataPacket.signalLevel, bbData, pointTime, hmPortTime)
            // Счётчик количества перезагрузок модема
            CoreTelematicFunction.putSensorData(dc.index, 9, 4, udsDataPacket.modemRestartCount, bbData, pointTime, hmPortTime)
            // Ограничение нагрузки на привод (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 10, udsDataPacket.driveLoadRestrict, bbData, pointTime, hmPortTime)
            // Строка с содержимым результата AT-команды запроса баланса (если баланс не запрашивался, указывается “-1”)
            CoreTelematicFunction.putSensorData(dc.index, 11, udsDataPacket.balance, bbData, pointTime, hmPortTime)
            // Глубина парковки скребка (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 12, udsDataPacket.parkDepth, bbData, pointTime, hmPortTime)
            // Количество попыток прохода препятствия (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 13, 4, udsDataPacket.passAttempt, bbData, pointTime, hmPortTime)
            // Синхронизация с запуском ЭЦН (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 14, 1, if (udsDataPacket.ecnRunSync) 1 else 0, bbData, pointTime, hmPortTime)
            // Пауза между проходами препятствия (параметр настройки)
            CoreTelematicFunction.putSensorData(dc.index, 15, 4, udsDataPacket.passDelay, bbData, pointTime, hmPortTime)
            // Текущая темп. внутри станции УДС (29,3˚С) (реле №1)
            udsDataPacket.t1Current?.let {
                CoreTelematicFunction.putSensorData(dc.index, 16, udsDataPacket.t1Current, bbData, pointTime, hmPortTime)
            }
            // Текущая темп. на улице (23,4 ˚С) (реле №2)
            udsDataPacket.t2Current?.let {
                CoreTelematicFunction.putSensorData(dc.index, 17, udsDataPacket.t2Current, bbData, pointTime, hmPortTime)
            }
            // Уровень температуры внутри (параметр настройки)
            udsDataPacket.t1Setup?.let {
                CoreTelematicFunction.putSensorData(dc.index, 18, udsDataPacket.t1Setup, bbData, pointTime, hmPortTime)
            }
            // Уровень температуры снаружи (параметр настройки)
            udsDataPacket.t2Setup?.let {
                CoreTelematicFunction.putSensorData(dc.index, 19, udsDataPacket.t2Setup, bbData, pointTime, hmPortTime)
            }
            // Работает или не работает реле №1 (температура)
            udsDataPacket.t1State?.let {
                CoreTelematicFunction.putSensorData(dc.index, 20, 1, if (udsDataPacket.t1State) 1 else 0, bbData, pointTime, hmPortTime)
            }
            // Работает или не работает реле №2 (температура)
            udsDataPacket.t2State?.let {
                CoreTelematicFunction.putSensorData(dc.index, 21, 1, if (udsDataPacket.t2State) 1 else 0, bbData, pointTime, hmPortTime)
            }
            // Работает или не работает канал №1
            udsDataPacket.ch1State?.let {
                CoreTelematicFunction.putSensorData(dc.index, 22, 1, if (udsDataPacket.ch1State) 1 else 0, bbData, pointTime, hmPortTime)
            }
            // Работает или не работает канал №2
            udsDataPacket.ch2State?.let {
                CoreTelematicFunction.putSensorData(dc.index, 23, 1, if (udsDataPacket.ch2State) 1 else 0, bbData, pointTime, hmPortTime)
            }
            // Работает или не работает модуль ТРМ (общее управление температурными датчиками)
            udsDataPacket.tpmState?.let {
                CoreTelematicFunction.putSensorData(dc.index, 24, 1, if (udsDataPacket.tpmState) 1 else 0, bbData, pointTime, hmPortTime)
            }
        }
        return bbData
    }

    protected fun sendCommand(dataWorker: CoreNioWorker) {
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
        var sendStatus = true   // default (for 'not founded', for example, as sended already)
        var command = ""
        if (rs.next()) {
            commandId = rs.getInt(1)
            sendStatus = rs.getInt(2) != CommandStatusCode.NOT_SENDED
            command = rs.getString(3)
        }
        rs.close()
        //--- если команда ещё не отправлена - работаем дальше.
        //--- независимо от этого все предыдущие неотправленные команды игнорируем как устаревшие
        if (!sendStatus) {
            val commandDataString = getUDSCommandDataString(command)
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
    }

    protected abstract fun getUDSCommandDataString(command: String): String

}