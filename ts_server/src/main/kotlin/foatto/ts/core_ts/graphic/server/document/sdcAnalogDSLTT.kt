package foatto.ts.core_ts.graphic.server.document

import foatto.core.app.graphic.GraphicElement
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.graphic.server.TSGraphicDocumentConfig
import foatto.ts.core_ts.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import java.util.*

class sdcAnalogDSLTT : sdcAbstractAnalog() {

    override fun getGraphicElements(
        sd: GraphicStartData,
        begTime: Int,
        endTime: Int,
        viewWidth: Int,
        viewHeight: Int,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        isShowBack: Boolean,
        isShowPoint: Boolean,
        isShowLine: Boolean,
        isShowText: Boolean,
        objectConfig: ObjectConfig,
        tmElement: SortedMap<String, GraphicElement>,
        tmElementVisibleConfig: SortedMap<String, String>,
    ) {
        val tsgdc = GraphicDocumentConfig.hmConfig[documentTypeName] as TSGraphicDocumentConfig
        val graphicHandler = tsgdc.graphicHandler as AnalogGraphicHandler

        val scaDepth = objectConfig.hmSensorConfig[SensorConfig.SENSOR_DEPTH]?.values?.firstOrNull() as? SensorConfigAnalogue
        val scaSpeed = objectConfig.hmSensorConfig[SensorConfig.SENSOR_SPEED]?.values?.firstOrNull() as? SensorConfigAnalogue
        val scaLoad = objectConfig.hmSensorConfig[SensorConfig.SENSOR_LOAD]?.values?.firstOrNull() as? SensorConfigAnalogue

        val scaTemperatureIn = objectConfig.hmSensorConfig[SensorConfig.SENSOR_TEMPERATURE_IN]?.values?.firstOrNull() as? SensorConfigAnalogue
        val scaTemperatureOut = objectConfig.hmSensorConfig[SensorConfig.SENSOR_TEMPERATURE_OUT]?.values?.firstOrNull() as? SensorConfigAnalogue

        getGraphicElement(
            graphicTitle = "#1 Глубина, скорость и нагрузка",
            begTime = begTime,
            endTime = endTime,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            alRawTime = alRawTime,
            alRawData = alRawData,
            isShowBack = isShowBack,
            isShowPoint = isShowPoint,
            isShowLine = isShowLine,
            isShowText = isShowText,
            objectConfig = objectConfig,
            graphicHandler = graphicHandler,
            alSca = listOf(scaDepth, scaSpeed, scaLoad).filterNotNull(),
            tmElement = tmElement,
            tmElementVisibleConfig = tmElementVisibleConfig,
        )

        getGraphicElement(
            graphicTitle = "#2 Внутренняя и внешняя температуры",
            begTime = begTime,
            endTime = endTime,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            alRawTime = alRawTime,
            alRawData = alRawData,
            isShowBack = isShowBack,
            isShowPoint = isShowPoint,
            isShowLine = isShowLine,
            isShowText = isShowText,
            objectConfig = objectConfig,
            graphicHandler = graphicHandler,
            alSca = listOf(scaTemperatureIn, scaTemperatureOut).filterNotNull(),
            tmElement = tmElement,
            tmElementVisibleConfig = tmElementVisibleConfig,
        )
    }

}