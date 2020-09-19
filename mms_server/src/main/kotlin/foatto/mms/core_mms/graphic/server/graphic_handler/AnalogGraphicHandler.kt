package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.util.AdvancedByteBuffer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.sensor.SensorConfigSemiAnalogue

open class AnalogGraphicHandler : iGraphicHandler {

    //----------------------------------------------------------------------------------------------------------------------------------------

    override val lineNoneColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_NONE_0
    override val lineNormalColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_NORMAL_0
    override val lineWarningColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_WARNING_0
    override val lineCriticalColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_CRITICAL_0

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- если заданы пределы ( максимум > минимума ), то включено
    override fun isStaticMinLimit(sca: SensorConfigSemiAnalogue) = sca.maxLimit > sca.minLimit
    override fun isStaticMaxLimit(sca: SensorConfigSemiAnalogue) = sca.maxLimit > sca.minLimit

    override fun getStaticMinLimit(sca: SensorConfigSemiAnalogue) = sca.minLimit
    override fun getStaticMaxLimit(sca: SensorConfigSemiAnalogue) = sca.maxLimit

    override fun setStaticMinLimit(sca: SensorConfigSemiAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?) {
        if (aMinLimit != null) {
            aMinLimit.alGLD.add(GraphicLineData(begTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
            aMinLimit.alGLD.add(GraphicLineData(endTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
        }
    }

    override fun setStaticMaxLimit(sca: SensorConfigSemiAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?) {
        if (aMaxLimit != null) {
            aMaxLimit.alGLD.add(GraphicLineData(begTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
            aMaxLimit.alGLD.add(GraphicLineData(endTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun isDynamicMinLimit(sca: SensorConfigSemiAnalogue) = false
    override fun isDynamicMaxLimit(sca: SensorConfigSemiAnalogue) = false

    override fun getDynamicMinLimit(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, rawTime: Int, rawData: Double) = sca.minLimit
    override fun getDynamicMaxLimit(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, rawTime: Int, rawData: Double) = sca.maxLimit

    override fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: GraphicDataContainer) {}
    override fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: GraphicDataContainer) {}

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getRawData(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, bb: AdvancedByteBuffer): Double? {
        val sensorData = AbstractObjectStateCalc.getSensorData(oc, sca.portNum, bb)?.toDouble() ?: 0.0
        //--- вручную игнорируем заграничные значения
        return if (sensorData < sca.minIgnore || sensorData > sca.maxIgnore) null
        else AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData)
    }

    override fun getLineColorIndex(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex {
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
