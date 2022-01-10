package foatto.ts.core_ts.graphic.server.document

import foatto.core.app.graphic.GraphicElement
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.graphic.server.TSGraphicDocumentConfig
import foatto.ts.core_ts.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import java.util.*

class sdcAnalog : sdcAbstractAnalog() {

    companion object {
        //const val MIN_CONNECT_OFF_TIME = 15 * 60
//        private const val MIN_NO_DATA_TIME = 5 * 60

//        //--- ловля основных/системных нештатных ситуаций, показываемых только на первом/верхнем графике:
//        //--- нет связи, нет данных и резервное питание
//        fun checkCommonTrouble(
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            oc: ObjectConfig,
//            begTime: Int,
//            endTime: Int,
//            aText: GraphicDataContainer
//        ) {
//            val alGTD = aText.alGTD.toMutableList()
//
//            //--- поиск значительных промежутков отсутствия данных ---
//
//            var lastDataTime = begTime
//            for (rawTime in alRawTime) {
//                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
//                if (rawTime < begTime) continue
//                if (rawTime > endTime) break
//
//                if (rawTime - lastDataTime > MIN_NO_DATA_TIME) {
//                    alGTD += GraphicTextData(
//                        textX1 = lastDataTime,
//                        textX2 = rawTime,
//                        fillColorIndex = GraphicColorIndex.FILL_CRITICAL,
//                        borderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
//                        textColorIndex = GraphicColorIndex.TEXT_CRITICAL,
//                        text = "Нет данных от прибора",
//                        toolTip = "Нет данных от прибора"
//                    )
//                }
//                lastDataTime = rawTime
//            }
//            if (min(lastDataTime, endTime) - lastDataTime > MIN_NO_DATA_TIME) {
//                alGTD += GraphicTextData(
//                    textX1 = lastDataTime,
//                    textX2 = min(lastDataTime, endTime),
//                    fillColorIndex = GraphicColorIndex.FILL_CRITICAL,
//                    borderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
//                    textColorIndex = GraphicColorIndex.TEXT_CRITICAL,
//                    text = "Нет данных от прибора",
//                    toolTip = "Нет данных от прибора"
//                )
//            }
//
//            aText.alGTD = alGTD.toTypedArray()
//        }
//
//        fun checkSensorError(
//            alRawTime: List<Int>,
//            alRawData: List<AdvancedByteBuffer>,
//            portNum: Int,
//            sensorDescr: String,
//            begTime: Int,
//            endTime: Int,
//            aFillColorIndex: GraphicColorIndex,
//            aBorderColorIndex: GraphicColorIndex,
//            aTextColorIndex: GraphicColorIndex,
//            troubleCode: Int,
//            troubleDescr: String,
//            minTime: Int,
//            alGTD: MutableList<GraphicTextData>
//        ) {
//
//            //--- в основном тексте пишем только текст ошибки, а в tooltips'e напишем вместе с описанием датчика
//            val fullTroubleDescr = StringBuilder(sensorDescr).append(": ").append(troubleDescr).toString()
//            var troubleBegTime = 0
//            var sensorData: Int
//
//            for (pos in alRawTime.indices) {
//                val rawTime = alRawTime[pos]
//                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
//                if (rawTime < begTime) continue
//                if (rawTime > endTime) break
//
//                sensorData = AbstractObjectStateCalc.getSensorData(portNum, alRawData[pos])?.toInt() ?: continue
//                if (sensorData == troubleCode) {
//                    if (troubleBegTime == 0) {
//                        troubleBegTime = rawTime
//                    }
//                } else if (troubleBegTime != 0) {
//                    if (rawTime - troubleBegTime > minTime) {
//                        alGTD += GraphicTextData(
//                            textX1 = troubleBegTime,
//                            textX2 = rawTime,
//                            fillColorIndex = aFillColorIndex,
//                            borderColorIndex = aBorderColorIndex,
//                            textColorIndex = aTextColorIndex,
//                            text = troubleDescr,
//                            toolTip = fullTroubleDescr
//                        )
//                    }
//                    troubleBegTime = 0
//                }
//            }
//            //--- запись последней незакрытой проблемы
//            if (troubleBegTime != 0 && min(getCurrentTimeInt(), endTime) - troubleBegTime > minTime) {
//                alGTD += GraphicTextData(
//                    textX1 = troubleBegTime,
//                    textX2 = min(getCurrentTimeInt(), endTime),
//                    fillColorIndex = aFillColorIndex,
//                    borderColorIndex = aBorderColorIndex,
//                    textColorIndex = aTextColorIndex,
//                    text = troubleDescr,
//                    toolTip = fullTroubleDescr
//                )
//            }
//        }
    }

    override fun getGraphicElements(
        sd: GraphicStartData,
        begTime: Int,
        endTime: Int,
        viewWidth: Int,
        viewHeight: Int,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        objectConfig: ObjectConfig,
        tmElement: SortedMap<String, GraphicElement>,
        tmElementVisibleConfig: SortedMap<String, Triple<String, String, Boolean>>,
    ) {
        val tsgdc = GraphicDocumentConfig.hmConfig[documentTypeName] as TSGraphicDocumentConfig
        val sensorType = tsgdc.sensorType
        val graphicHandler = tsgdc.graphicHandler as AnalogGraphicHandler

        objectConfig.hmSensorConfig[sensorType]?.let { hmSensorConfig ->
            hmSensorConfig.values.forEach { sc ->
                val sca = sc as SensorConfigAnalogue

                getGraphicElement(
                    graphicTitle = sca.descr,
                    begTime = begTime,
                    endTime = endTime,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    objectConfig = objectConfig,
                    graphicHandler = graphicHandler,
                    alSca = listOf(sca),
                    tmElement = tmElement,
                    tmElementVisibleConfig = tmElementVisibleConfig,
                )
            }
        }
    }
}
