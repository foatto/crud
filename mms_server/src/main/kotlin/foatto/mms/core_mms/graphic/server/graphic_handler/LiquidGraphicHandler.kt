package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel

class LiquidGraphicHandler : AnalogGraphicHandler() {

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun isStaticMinLimit(sca: SensorConfigAnalogue) = false
    override fun isStaticMaxLimit(sca: SensorConfigAnalogue) = false

    override fun getStaticMinLimit(sca: SensorConfigAnalogue) = 0.0
    override fun getStaticMaxLimit(sca: SensorConfigAnalogue) = 0.0

    override fun setStaticMinLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?) {}
    override fun setStaticMaxLimit(sca: SensorConfigAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?) {}

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getLineColorIndex(sca: SensorConfigAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex {
        //--- checking for coincidence of the start / end points, so that there is no division by zero later
        if (rawTime == prevTime) return lineNormalColorIndex

        //--- too large spacing between points
        if (rawTime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) return lineNoneColorIndex

        val scll = sca as SensorConfigLiquidLevel
        val liquidKoef = (rawData - prevData) * 3600 / (rawTime - prevTime)
        return if (liquidKoef > 0 && liquidKoef > scll.detectIncKoef) lineWarningColorIndex
        else if (liquidKoef < 0 && -liquidKoef > scll.detectDecKoef) lineCriticalColorIndex
        else lineNormalColorIndex
    }
}
