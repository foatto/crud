package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.util.AdvancedByteBuffer
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue

interface iGraphicHandler {

    fun getLineNoneColorIndex(axisIndex: Int): GraphicColorIndex
    fun getLineNormalColorIndex(axisIndex: Int): GraphicColorIndex
    fun getLineBelowColorIndex(axisIndex: Int): GraphicColorIndex
    fun getLineAboveColorIndex(axisIndex: Int): GraphicColorIndex

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- whether static limit lines will be used / displayed
    fun isStaticMinLimit(sca: SensorConfigAnalogue): Boolean
    fun isStaticMaxLimit(sca: SensorConfigAnalogue): Boolean

    fun getStaticMinLimit(sca: SensorConfigAnalogue): Double
    fun getStaticMaxLimit(sca: SensorConfigAnalogue): Double

    fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?)
    fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?)

    //----------------------------------------------------------------------------------------------------------------------------------------

    fun getRawData(sca: SensorConfigAnalogue, bb: AdvancedByteBuffer): Double?

    fun getLineColorIndex(axisIndex: Int, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex
}
