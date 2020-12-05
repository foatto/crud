package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.util.AdvancedByteBuffer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue

interface iGraphicHandler {

    //--- empirically found out - it is not worth averaging over X,
    //--- since in this case, sharp transitions between levels are too straightened,
    //--- since run / average intermediate points to multiple neighboring points,
    //--- and the refueling detector starts to make mistakes (not counting the start of refueling),
    //--- since the midpoint ran too far to the right and the proper amount of l / h does not work
    //boolean useAverageX();

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

    fun getDynamicMinLimit(oc: ObjectConfig, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double): Double
    fun getDynamicMaxLimit(oc: ObjectConfig, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double): Double

    fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: GraphicDataContainer)
    fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: GraphicDataContainer)

    //----------------------------------------------------------------------------------------------------------------------------------------

    fun getRawData(oc: ObjectConfig, sca: SensorConfigAnalogue, bb: AdvancedByteBuffer): Double?

    fun getLineColorIndex(oc: ObjectConfig, sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex
}
