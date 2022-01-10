package foatto.mms.core_mms.graphic.server.document

import foatto.core.app.graphic.GraphicElement
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.graphic.server.MMSGraphicDocumentConfig
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import java.util.*

open class sdcAnalog : sdcAbstractAnalog() {

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
        val mmsgdc = GraphicDocumentConfig.hmConfig[documentTypeName] as MMSGraphicDocumentConfig
        val sensorType = mmsgdc.sensorType
        val graphicHandler = mmsgdc.graphicHandler as AnalogGraphicHandler

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
                    alSca = listOf(sca),
                    alGraphicHandler = listOf(graphicHandler),
                    tmElement = tmElement,
                    tmElementVisibleConfig = tmElementVisibleConfig,
                )
            }
        }
    }

}
