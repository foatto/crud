package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicColorIndex
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.SensorConfigA

class LiquidGraphicHandler : AnalogGraphicHandler() {

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun isStaticMinLimit( sca: SensorConfigA ) = false
    override fun isStaticMaxLimit( sca: SensorConfigA ) = false

    override fun getStaticMinLimit( sca: SensorConfigA ) = 0.0
    override fun getStaticMaxLimit( sca: SensorConfigA ) = 0.0

    override fun setStaticMinLimit(sca: SensorConfigA, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer? ) {}
    override fun setStaticMaxLimit(sca: SensorConfigA, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer? ) {}

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getLineColorIndex(oc: ObjectConfig, sca: SensorConfigA, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double ): GraphicColorIndex {
        //--- проверка на совпадение времён начальной/конечной точек, чтобы не было потом деления на ноль
        if( rawTime == prevTime ) return lineNormalColorIndex

        //--- слишком большой интервал между точками
        if( rawTime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL ) return lineNoneColorIndex

        val liquidKoef = ( rawData - prevData ) * 3600 / ( rawTime - prevTime )
        return if( liquidKoef > 0 &&  liquidKoef > sca.detectIncKoef ) lineCriticalColorIndex
          else if( liquidKoef < 0 && -liquidKoef > sca.detectDecKoef ) lineWarningColorIndex
          else lineNormalColorIndex
    }
}
