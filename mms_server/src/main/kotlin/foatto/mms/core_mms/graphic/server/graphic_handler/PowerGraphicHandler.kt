package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.util.AdvancedByteBuffer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.sensor.SensorConfigA

class PowerGraphicHandler : AnalogGraphicHandler() {

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- график мощности - дополнительный/третий график на графике уровня жидкости/топлива
    override val lineNormalColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_NORMAL_2
    override val lineWarningColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_WARNING_2
    override val lineCriticalColorIndex: GraphicColorIndex = GraphicColorIndex.LINE_CRITICAL_2

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getRawData( oc: ObjectConfig, scaep: SensorConfigA, bb: AdvancedByteBuffer ): Double? {
        val energoCountActiveDirect = AbstractObjectStateCalc.getSensorData( oc, scaep.portNum, bb )?.toInt() ?: 0
//--- вручную игнорируем заграничные значения
        return if( energoCountActiveDirect < scaep.minIgnore ||
                   energoCountActiveDirect > scaep.maxIgnore ) null
               else AbstractObjectStateCalc.getSensorValue( scaep.alValueSensor, scaep.alValueData, energoCountActiveDirect.toDouble() )
    }
}
