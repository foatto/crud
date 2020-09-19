package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.util.AdvancedByteBuffer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.SensorConfigSemiAnalogue

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
    fun isStaticMinLimit(sca: SensorConfigSemiAnalogue): Boolean
    fun isStaticMaxLimit(sca: SensorConfigSemiAnalogue): Boolean

    fun getStaticMinLimit(sca: SensorConfigSemiAnalogue): Double
    fun getStaticMaxLimit(sca: SensorConfigSemiAnalogue): Double

    fun setStaticMinLimit(sca: SensorConfigSemiAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?)
    fun setStaticMaxLimit(sca: SensorConfigSemiAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?)

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- whether dynamic limit lines will be used / displayed
    fun isDynamicMinLimit(sca: SensorConfigSemiAnalogue): Boolean
    fun isDynamicMaxLimit(sca: SensorConfigSemiAnalogue): Boolean

    fun getDynamicMinLimit(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, rawTime: Int, rawData: Double): Double
    fun getDynamicMaxLimit(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, rawTime: Int, rawData: Double): Double

    fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: GraphicDataContainer)
    fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: GraphicDataContainer)

    //----------------------------------------------------------------------------------------------------------------------------------------

    fun getRawData(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, bb: AdvancedByteBuffer): Double?

    fun getLineColorIndex(oc: ObjectConfig, sca: SensorConfigSemiAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex
}
