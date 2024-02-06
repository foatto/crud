package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.app.graphic.graphicLineAboveColorIndexes
import foatto.core.app.graphic.graphicLineBelowColorIndexes
import foatto.core.app.graphic.graphicLineNoneColorIndexes
import foatto.core.app.graphic.graphicLineNormalColorIndexes
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.util.AdvancedByteBuffer

open class AnalogGraphicHandler : iGraphicHandler {

    override fun getLineNoneColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineNoneColorIndexes[axisIndex]
    override fun getLineNormalColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineNormalColorIndexes[axisIndex]
    override fun getLineBelowColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineBelowColorIndexes[axisIndex]
    override fun getLineAboveColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineAboveColorIndexes[axisIndex]

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- если заданы пределы ( максимум > минимума ), то включено
    override fun isStaticMinLimit(sca: SensorConfigAnalogue) = sca.maxLimit != sca.minLimit
    override fun isStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxLimit != sca.minLimit

    override fun getStaticMinLimit(sca: SensorConfigAnalogue) = sca.minLimit
    override fun getStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxLimit

    override fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?) {
        aMinLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(GraphicLineData(begTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
                add(GraphicLineData(endTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
            }
        }
    }

    override fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?) {
        aMaxLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(GraphicLineData(begTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
                add(GraphicLineData(endTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getRawData(sca: SensorConfigAnalogue, bb: AdvancedByteBuffer): Double? {
        val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bb)?.toDouble() ?: return null
        //--- вручную игнорируем заграничные значения
        return if (ObjectCalc.isIgnoreSensorData(sca.minIgnore, sca.maxIgnore, sensorData)) {
            null
        } else {
            AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData)
        }
    }

    override fun getLineColorIndex(axisIndex: Int, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex =
        getLineNormalColorIndex(axisIndex)
}
