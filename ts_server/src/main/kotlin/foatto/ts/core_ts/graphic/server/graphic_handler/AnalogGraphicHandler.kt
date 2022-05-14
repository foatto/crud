package foatto.ts.core_ts.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.app.graphic.graphicLineAboveColorIndexes
import foatto.core.app.graphic.graphicLineBelowColorIndexes
import foatto.core.app.graphic.graphicLineNoneColorIndexes
import foatto.core.app.graphic.graphicLineNormalColorIndexes
import foatto.core.util.AdvancedByteBuffer
import foatto.ts.core_ts.calc.AbstractObjectStateCalc
import foatto.ts.core_ts.calc.ObjectCalc
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue

class AnalogGraphicHandler : iGraphicHandler {

    override fun getLineNoneColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineNoneColorIndexes[axisIndex]
    override fun getLineNormalColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineNormalColorIndexes[axisIndex]
    override fun getLineBelowColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineBelowColorIndexes[axisIndex]
    override fun getLineAboveColorIndex(axisIndex: Int): GraphicColorIndex = graphicLineAboveColorIndexes[axisIndex]

//----------------------------------------------------------------------------------------------------------------------------------------

    //--- если заданы пределы ( максимум > минимума ), то включено
    override fun isStaticMinLimit(sca: SensorConfigAnalogue) = sca.maxGraphicLimit != sca.minGraphicLimit
    override fun isStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxGraphicLimit != sca.minGraphicLimit

    override fun getStaticMinLimit(sca: SensorConfigAnalogue) = sca.minGraphicLimit
    override fun getStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxGraphicLimit

    override fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?) {
        aMinLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(GraphicLineData(begTime, sca.minGraphicLimit, GraphicColorIndex.LINE_LIMIT))
                add(GraphicLineData(endTime, sca.minGraphicLimit, GraphicColorIndex.LINE_LIMIT))
            }.toTypedArray()
        }
    }

    override fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?) {
        aMaxLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(GraphicLineData(begTime, sca.maxGraphicLimit, GraphicColorIndex.LINE_LIMIT))
                add(GraphicLineData(endTime, sca.maxGraphicLimit, GraphicColorIndex.LINE_LIMIT))
            }.toTypedArray()
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun isDynamicMinLimit(sca: SensorConfigAnalogue) = false
    override fun isDynamicMaxLimit(sca: SensorConfigAnalogue) = false

    override fun getDynamicMinLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double) = sca.minGraphicLimit
    override fun getDynamicMaxLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double) = sca.maxGraphicLimit

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

    override fun getLineColorIndex(axisIndex: Int, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex {
        val avgDynamicMinLimit = getDynamicMinLimit(sca, rawTime, rawData)
        val avgDynamicMaxLimit = getDynamicMaxLimit(sca, rawTime, rawData)

        if (rawTime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) {
            return getLineNoneColorIndex(axisIndex)
        }

        var colorIndex = getLineNormalColorIndex(axisIndex)
        if (isStaticMinLimit(sca)) {
            colorIndex = if (rawData > avgDynamicMaxLimit) {
                getLineAboveColorIndex(axisIndex)
            } else if (rawData < avgDynamicMinLimit) {
                getLineBelowColorIndex(axisIndex)
            } else {
                getLineNormalColorIndex(axisIndex)
            }
        }
        return colorIndex
    }
/*
    override fun getLineColorIndex(axisIndex: Int, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex {
        //--- checking for coincidence of the start / end points, so that there is no division by zero later
        if (rawTime == prevTime) {
            return getLineNormalColorIndex(axisIndex)
        }

        //--- too large spacing between points
        val scll = sca as SensorConfigLiquidLevel
        val liquidKoef = (rawData - prevData) * 3600 / (rawTime - prevTime)

        return if (liquidKoef > 0 && liquidKoef > scll.detectIncKoef) {
            getLineAboveColorIndex(axisIndex)
        }
        else if (liquidKoef < 0 && -liquidKoef > scll.detectDecKoef) {
            getLineBelowColorIndex(axisIndex)
        }
        else {
            getLineNormalColorIndex(axisIndex)
        }
    }
 */
}
