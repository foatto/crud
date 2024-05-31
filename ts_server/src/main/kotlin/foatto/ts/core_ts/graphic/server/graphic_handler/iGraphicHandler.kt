package foatto.ts.core_ts.graphic.server.graphic_handler

import foatto.core.app.chart.ChartColorIndex
import foatto.core.app.chart.ChartElementDTO
import foatto.util.AdvancedByteBuffer
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue

interface iGraphicHandler {

    fun getLineNoneColorIndex(axisIndex: Int): ChartColorIndex
    fun getLineNormalColorIndex(axisIndex: Int): ChartColorIndex
    fun getLineBelowColorIndex(axisIndex: Int): ChartColorIndex
    fun getLineAboveColorIndex(axisIndex: Int): ChartColorIndex

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- whether static limit lines will be used / displayed
    fun isStaticMinLimit(sca: SensorConfigAnalogue): Boolean
    fun isStaticMaxLimit(sca: SensorConfigAnalogue): Boolean

    fun getStaticMinLimit(sca: SensorConfigAnalogue): Double
    fun getStaticMaxLimit(sca: SensorConfigAnalogue): Double

    fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: ChartElementDTO?)
    fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: ChartElementDTO?)

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- whether dynamic limit lines will be used / displayed
    fun isDynamicMinLimit(sca: SensorConfigAnalogue): Boolean
    fun isDynamicMaxLimit(sca: SensorConfigAnalogue): Boolean

    fun getDynamicMinLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double): Double
    fun getDynamicMaxLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double): Double

    fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: ChartElementDTO)
    fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: ChartElementDTO)

    //----------------------------------------------------------------------------------------------------------------------------------------

    fun getRawData(sca: SensorConfigAnalogue, bb: AdvancedByteBuffer): Double?

    fun getLineColorIndex(axisIndex: Int, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): ChartColorIndex
}
