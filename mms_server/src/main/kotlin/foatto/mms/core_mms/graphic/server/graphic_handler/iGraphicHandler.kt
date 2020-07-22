package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.util.AdvancedByteBuffer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.SensorConfigA

interface iGraphicHandler {

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- опытным путём выяснено - делать усреднение по X не стОит,
    //--- т.к. в этом случае резкие переходы между уровнями получаются слишком спрямленными,
    //--- т.к. промежуточные точки "убегают/усредняются" к многочисленным соседним точкам,
    //--- а детектор заправки начинает ошибаться (незасчитывать начало заправки),
    //--- т.к. средняя точка слишком убежала направо и должного кол-ва л/час не получается
    //boolean useAverageX();

    val lineNoneColorIndex: GraphicColorIndex
    val lineNormalColorIndex: GraphicColorIndex
    val lineWarningColorIndex: GraphicColorIndex
    val lineCriticalColorIndex: GraphicColorIndex

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- будут ли использоваться/показываться статические линии предельных значений
    fun isStaticMinLimit( sca: SensorConfigA ): Boolean
    fun isStaticMaxLimit( sca: SensorConfigA ): Boolean

    fun getStaticMinLimit( sca: SensorConfigA ): Double
    fun getStaticMaxLimit( sca: SensorConfigA ): Double

    fun setStaticMinLimit(sca: SensorConfigA, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer? )
    fun setStaticMaxLimit(sca: SensorConfigA, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer? )

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- будут ли использоваться/показываться динамические линии предельных значений
    fun isDynamicMinLimit( sca: SensorConfigA ): Boolean
    fun isDynamicMaxLimit( sca: SensorConfigA ): Boolean

    fun getDynamicMinLimit(oc: ObjectConfig, sca: SensorConfigA, rawTime: Int, rawData: Double ): Double
    fun getDynamicMaxLimit(oc: ObjectConfig, sca: SensorConfigA, rawTime: Int, rawData: Double ): Double

    fun addDynamicMinLimit(rawTime: Int, dynamicMinLimit: Double, aMinLimit: GraphicDataContainer )
    fun addDynamicMaxLimit(rawTime: Int, dynamicMaxLimit: Double, aMaxLimit: GraphicDataContainer )

    //----------------------------------------------------------------------------------------------------------------------------------------

    fun getRawData( oc: ObjectConfig, sca: SensorConfigA, bb: AdvancedByteBuffer ): Double?

    fun getLineColorIndex(oc: ObjectConfig, sca: SensorConfigA, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double ): GraphicColorIndex
}
