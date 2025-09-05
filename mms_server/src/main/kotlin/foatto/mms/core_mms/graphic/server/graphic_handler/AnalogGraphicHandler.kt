package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.chart.ChartColorIndex
import foatto.core.app.chart.ChartElementDTO
import foatto.core.app.chart.ChartElementLineDTO
import foatto.core.app.chart.chartLineAboveColorIndexes
import foatto.core.app.chart.chartLineBelowColorIndexes
import foatto.core.app.chart.chartLineNoneColorIndexes
import foatto.core.app.chart.chartLineNormalColorIndexes
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.util.AdvancedByteBuffer

open class AnalogGraphicHandler : iGraphicHandler {

    override fun getLineNoneColorIndex(axisIndex: Int): ChartColorIndex = chartLineNoneColorIndexes[axisIndex]
    override fun getLineNormalColorIndex(axisIndex: Int): ChartColorIndex = chartLineNormalColorIndexes[axisIndex]
    override fun getLineBelowColorIndex(axisIndex: Int): ChartColorIndex = chartLineBelowColorIndexes[axisIndex]
    override fun getLineAboveColorIndex(axisIndex: Int): ChartColorIndex = chartLineAboveColorIndexes[axisIndex]

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- если заданы пределы ( максимум > минимума ), то включено
    override fun isStaticMinLimit(sca: SensorConfigAnalogue) = sca.maxLimit != sca.minLimit
    override fun isStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxLimit != sca.minLimit

    override fun getStaticMinLimit(sca: SensorConfigAnalogue) = sca.minLimit
    override fun getStaticMaxLimit(sca: SensorConfigAnalogue) = sca.maxLimit

    override fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: ChartElementDTO?) {
        aMinLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(ChartElementLineDTO(begTime, sca.minLimit.toFloat(), ChartColorIndex.LINE_LIMIT))
                add(ChartElementLineDTO(endTime, sca.minLimit.toFloat(), ChartColorIndex.LINE_LIMIT))
            }
        }
    }

    override fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: ChartElementDTO?) {
        aMaxLimit?.apply {
            alGLD = alGLD.toMutableList().apply {
                add(ChartElementLineDTO(begTime, sca.maxLimit.toFloat(), ChartColorIndex.LINE_LIMIT))
                add(ChartElementLineDTO(endTime, sca.maxLimit.toFloat(), ChartColorIndex.LINE_LIMIT))
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

    override fun getLineColorIndex(axisIndex: Int, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): ChartColorIndex =
        if (rawTime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) {
            getLineNoneColorIndex(axisIndex)
        } else if (sca.minLimit == sca.maxLimit) {
            getLineNormalColorIndex(axisIndex)
        } else if (rawData > sca.maxLimit) {
            getLineAboveColorIndex(axisIndex)
        } else if (rawData < sca.minLimit) {
            getLineBelowColorIndex(axisIndex)
        } else {
            getLineNormalColorIndex(axisIndex)
        }

}
