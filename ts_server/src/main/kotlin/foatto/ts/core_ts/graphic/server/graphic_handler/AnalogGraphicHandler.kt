package foatto.ts.core_ts.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.util.AdvancedByteBuffer
import foatto.ts.core_ts.calc.AbstractObjectStateCalc
import foatto.ts.core_ts.calc.ObjectCalc
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue

class AnalogGraphicHandler : iGraphicHandler {

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
        aMinLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(GraphicLineData(begTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
                add(GraphicLineData(endTime, sca.minLimit, GraphicColorIndex.LINE_LIMIT))
            }.toTypedArray()
        }
    }

    override fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?) {
        aMaxLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(GraphicLineData(begTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
                add(GraphicLineData(endTime, sca.maxLimit, GraphicColorIndex.LINE_LIMIT))
            }.toTypedArray()
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun isDynamicMinLimit(sca: SensorConfigAnalogue) = false
    override fun isDynamicMaxLimit(sca: SensorConfigAnalogue) = false

    override fun getDynamicMinLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double) = sca.minLimit
    override fun getDynamicMaxLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double) = sca.maxLimit

    override fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: GraphicDataContainer) {}
    override fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: GraphicDataContainer) {}

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getRawData(sca: SensorConfigAnalogue, bb: AdvancedByteBuffer): Double? {
        val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bb)?.toDouble() ?: return null
        return if (ObjectCalc.isIgnoreSensorData(sca, sensorData)) {
            null
        } else {
            AbstractObjectStateCalc.getSensorValue(sca.alValueSensor, sca.alValueData, sensorData)
        }
    }

    override fun getLineColorIndex(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex {
        val avgDynamicMinLimit = getDynamicMinLimit(sca, rawTime, rawData)
        val avgDynamicMaxLimit = getDynamicMaxLimit(sca, rawTime, rawData)

        var colorIndex = lineNormalColorIndex
        if (isStaticMinLimit(sca)) {
            colorIndex = if (rawData > avgDynamicMaxLimit) {
                lineCriticalColorIndex
            } else if (rawData < avgDynamicMinLimit) {
                lineWarningColorIndex
            } else {
                lineNormalColorIndex
            }
        }
        return colorIndex
    }
}
