package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.util.AdvancedByteBuffer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue

open class AnalogGraphicHandler : iGraphicHandler {

    //----------------------------------------------------------------------------------------------------------------------------------------

    override val lineNoneColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_NONE_0
    override val lineNormalColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_NORMAL_0
    override val lineWarningColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_BELOW_0
    override val lineCriticalColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_ABOVE_0

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- если заданы пределы ( максимум > минимума ), то включено
    override fun isStaticMinLimit(sca: SensorConfigAnalogue) = sca.maxLimit > sca.minLimit
    override fun isStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxLimit > sca.minLimit

    override fun getStaticMinLimit(sca: SensorConfigAnalogue) = sca.minLimit
    override fun getStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxLimit

    override fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?) {
        if (aMinLimit != null) {
            aMinLimit.alGLD.add(GraphicLineData(begTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
            aMinLimit.alGLD.add(GraphicLineData(endTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
        }
    }

    override fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?) {
        if (aMaxLimit != null) {
            aMaxLimit.alGLD.add(GraphicLineData(begTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
            aMaxLimit.alGLD.add(GraphicLineData(endTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun isDynamicMinLimit(sca: SensorConfigAnalogue) = false
    override fun isDynamicMaxLimit(sca: SensorConfigAnalogue) = false

    override fun getDynamicMinLimit(oc: ObjectConfig, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double) = sca.minLimit
    override fun getDynamicMaxLimit(oc: ObjectConfig, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double) = sca.maxLimit

    override fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: GraphicDataContainer) {}
    override fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: GraphicDataContainer) {}

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getRawData(oc: ObjectConfig, sca: SensorConfigAnalogue, bb: AdvancedByteBuffer): Double? {
        val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bb)?.toDouble() ?: return null
        //--- вручную игнорируем заграничные значения
        return if (ObjectCalc.isIgnoreSensorData(sca, sensorData)) null
        else AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData)
    }

    override fun getLineColorIndex(oc: ObjectConfig, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex {
        val avgDynamicMinLimit = getDynamicMinLimit(oc, sca, rawTime, rawData)
        val avgDynamicMaxLimit = getDynamicMaxLimit(oc, sca, rawTime, rawData)

        var colorIndex = lineNormalColorIndex
        if (isStaticMinLimit(sca)) {
            colorIndex = if (rawData > avgDynamicMaxLimit) lineCriticalColorIndex
            else if (rawData < avgDynamicMinLimit) lineWarningColorIndex
            else lineNormalColorIndex
        }
        return colorIndex
    }
}
