package foatto.mms.core_mms.graphic.server.graphic_handler

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.SensorConfigSemiAnalogue

class LiquidGraphicHandler : AnalogGraphicHandler() {

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun isStaticMinLimit(sca: SensorConfigSemiAnalogue) = false
    override fun isStaticMaxLimit(sca: SensorConfigSemiAnalogue) = false

    override fun getStaticMinLimit(sca: SensorConfigSemiAnalogue) = 0.0
    override fun getStaticMaxLimit(sca: SensorConfigSemiAnalogue) = 0.0

    override fun setStaticMinLimit(sca: SensorConfigSemiAnalogue, begTime: Int, endTime: Int, aMinLimit: GraphicDataContainer?) {}
    override fun setStaticMaxLimit(sca: SensorConfigSemiAnalogue, begTime: Int, endTime: Int, aMaxLimit: GraphicDataContainer?) {}

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun getLineColorIndex(oc: ObjectConfig, scsa: SensorConfigSemiAnalogue, rawTime: Int, rawData: Double, prevTime: Int, prevData: Double): GraphicColorIndex {
        //--- checking for coincidence of the start / end points, so that there is no division by zero later
        if (rawTime == prevTime) return lineNormalColorIndex

        //--- too large spacing between points
        if (rawTime - prevTime > ObjectCalc.MAX_WORK_TIME_INTERVAL) return lineNoneColorIndex

        val sca = scsa as SensorConfigAnalogue
        val liquidKoef = (rawData - prevData) * 3600 / (rawTime - prevTime)
        return if (liquidKoef > 0 && liquidKoef > sca.detectIncKoef) lineCriticalColorIndex
        else if (liquidKoef < 0 && -liquidKoef > sca.detectDecKoef) lineWarningColorIndex
        else lineNormalColorIndex
    }
}
