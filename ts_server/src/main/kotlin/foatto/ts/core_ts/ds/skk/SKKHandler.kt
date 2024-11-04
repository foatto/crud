package foatto.ts.core_ts.ds.skk

import com.fasterxml.jackson.module.kotlin.readValue
import foatto.core_server.ds.CoreTelematicFunction
import foatto.core_server.ds.nio.CoreNioWorker
import foatto.ts.core_ts.ds.TSHandler
import foatto.util.AdvancedByteBuffer
import foatto.util.AdvancedLogger
import foatto.util.getCurrentTimeInt
import java.nio.ByteOrder

class SKKHandler : TSHandler() {

    private val sbData = StringBuilder()

    //--- в общем-то всё равно, т.к. принимаются/передаются передаются строки,
    //--- но при передаче упакованных данных может пригодится
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

    private var curHeaderPacket: SKKPacket? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork(dataWorker: CoreNioWorker): Boolean {

        val arrByte = ByteArray(bbIn.remaining())
        bbIn.get(arrByte)
        sbData.append(String(arrByte))
        AdvancedLogger.debug("sbData = $sbData")

        //--- хотя бы один пакет собрался в общей строке данных?
        var packetEndPos = sbData.indexOf("\r\n")
        if (packetEndPos < 0) {
            bbIn.compact()
            return true
        }

        while (true) {
            val strPacket = sbData.substring(0, packetEndPos)
            AdvancedLogger.debug("data = $strPacket")

            //--- убираем разобранный пакет из начала общей строки данных
            sbData.delete(0, packetEndPos + 2)

            val packet: SKKPacket = objectMapper.readValue(strPacket)

            if (packet.type == 1) {
                status += " HeaderRead;"

                serialNo = packet.bu.toString()
                fwVersion = packet.vers ?: "(не задано)"
                imei = packet.modem?.imei ?: "(не задано)"

                //--- "принимаем" даже непрописанные приборы (для удобства быстрого/полевого тестирования связи с приборов)
                loadDeviceConfig(dataWorker, DEVICE_TYPE_SKK)

                curHeaderPacket = packet

                sendResponse(packet.id)
            } else {
                status += " DataRead;"

                val curTime = getCurrentTimeInt()
                deviceConfig?.let { deviceConfig ->
                    packet.data?.let { datas ->
                        for (data in datas) {
                            val pointTime = data.t

                            if (deviceConfig.objectId != 0 &&
                                pointTime > curTime - CoreTelematicFunction.MAX_PAST_TIME &&
                                pointTime < curTime + CoreTelematicFunction.MAX_FUTURE_TIME
                            ) {

                                val bbData = dataToByteBuffer(dataWorker, data)

                                addPoint(dataWorker.conn, pointTime, bbData)
                                dataCount++

                                if (firstPointTime == 0) {
                                    firstPointTime = pointTime
                                }
                                lastPointTime = pointTime
                            }
                            dataCountAll++
                        }
                    }
                }

                sendResponse(packet.id)
            }

            //--- есть ли ещё данные в пакете
            packetEndPos = sbData.indexOf("\r\n")
            if (packetEndPos < 0) {
                break
            }
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

    private fun sendResponse(packetId: Int) {
        val bbOut = AdvancedByteBuffer(128, byteOrder)
        bbOut.put("{\"id\":$packetId,\"state\":\"DONE\"}".toByteArray(Charsets.ISO_8859_1))
        outBuf(bbOut)
    }

    private fun dataToByteBuffer(dataWorker: CoreNioWorker, dataPacket: SKKDataData): AdvancedByteBuffer {
        val bbData = AdvancedByteBuffer(dataWorker.conn.dialect.textFieldMaxSize / 2)
        deviceConfig?.let { dc ->
            //--- какие-нибудь очень важные данные сюда запишем :)
            //CoreTelematicFunction.putSensorData(dc.index, 0, 4, XXX, bbData)
            // Глубина (в метрах)
            CoreTelematicFunction.putSensorData(dc.index, 1, 4, dataPacket.d, bbData)
            // Скорость спуска (метров/час)
            CoreTelematicFunction.putSensorData(dc.index, 2, 4, dataPacket.s, bbData)
            // Нагрузка на привод (%)
            CoreTelematicFunction.putSensorData(dc.index, 3, 4, dataPacket.f, bbData)

            curHeaderPacket?.let { headerData ->
                headerData.modem?.let { modemData ->
                    CoreTelematicFunction.putSensorData(dc.index, 10, 4, modemData.state, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 11, 4, modemData.csq, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 12, 4, modemData.many, bbData)
                }
                headerData.depth?.let { depthData ->
                    CoreTelematicFunction.putSensorData(dc.index, 20, 4, depthData.min, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 21, 4, depthData.max, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 22, 4, depthData.flags, bbData)
                }
                headerData.speed?.let { speedData ->
                    CoreTelematicFunction.putSensorData(dc.index, 30, 4, speedData.min, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 31, 4, speedData.max, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 32, 4, speedData.flags, bbData)
                }
                headerData.force?.let { forceData ->
                    CoreTelematicFunction.putSensorData(dc.index, 40, 4, forceData.min, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 41, 4, forceData.max, bbData)
                    CoreTelematicFunction.putSensorData(dc.index, 42, 4, forceData.flags, bbData)
                }
                headerData.wire?.let { wire ->
                    CoreTelematicFunction.putSensorData(dc.index, 50, 4, wire, bbData)
                }
                headerData.encoder?.let { encoder ->
                    CoreTelematicFunction.putSensorData(dc.index, 51, 4, encoder, bbData)
                }
                headerData.roller?.let { roller ->
                    CoreTelematicFunction.putSensorData(dc.index, 52, 4, roller, bbData)
                }
                headerData.area?.let { area ->
                    CoreTelematicFunction.putStringSensorData(dc.index, 60, area, bbData)
                }
                headerData.bush?.let { bush ->
                    CoreTelematicFunction.putStringSensorData(dc.index, 61, bush, bbData)
                }
                headerData.well?.let { well ->
                    CoreTelematicFunction.putStringSensorData(dc.index, 62, well, bbData)
                }
                headerData.shop?.let { shop ->
                    CoreTelematicFunction.putStringSensorData(dc.index, 63, shop, bbData)
                }
                headerData.crew?.let { crew ->
                    CoreTelematicFunction.putStringSensorData(dc.index, 64, crew, bbData)
                }
                headerData.auto?.let { auto ->
                    CoreTelematicFunction.putStringSensorData(dc.index, 65, auto, bbData)
                }
                headerData.people?.let { people ->
                    CoreTelematicFunction.putStringSensorData(dc.index, 66, people, bbData)
                }
            }
        }
        return bbData
    }

}

