package foatto.ts.core_ts.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.util.AdvancedByteBuffer
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue

interface iGraphicHandler {

    val lineNoneColorIndex: GraphicColorIndex
    val lineNormalColorIndex: GraphicColorIndex
    val lineWarningColorIndex: GraphicColorIndex
    val lineCriticalColorIndex: GraphicColorIndex

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- whether static limit lines will be used / displayed
    fun isStaticMinLimit(sca: SensorConfigAnalogue): Boolean
    fun isStaticMaxLimit(sca: SensorConfigAnalogue): Boolean

    fun getStaticMinLimit(sca: SensorConfigAnalogue): Double
    fun getStaticMaxLimit(sca: SensorConfigAnalogue): Double

    fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?)
    fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?)

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- whether dynamic limit lines will be used / displayed
    fun isDynamicMinLimit(sca: SensorConfigAnalogue): Boolean
    fun isDynamicMaxLimit(sca: SensorConfigAnalogue): Boolean

    fun getDynamicMinLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double): Double
    fun getDynamicMaxLimit(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double): Double

    fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: GraphicDataContainer)
    fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: GraphicDataContainer)

    //----------------------------------------------------------------------------------------------------------------------------------------

    fun getRawData(sca: SensorConfigAnalogue, bb: AdvancedByteBuffer): Double?

    fun getLineColorIndex(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex
}
