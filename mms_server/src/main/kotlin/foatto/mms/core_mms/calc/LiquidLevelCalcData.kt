package foatto.mms.core_mms.calc

import foatto.core.app.chart.ChartElementDTO
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel

class LiquidLevelCalcData {

    var containerType = SensorConfigLiquidLevel.CONTAINER_TYPE_WORK

    var begLevel = 0.0
    var endLevel = 0.0

    var incTotal = 0.0
    var decTotal = 0.0

    var usingTotal = 0.0
    var usingCalc = 0.0

    var aLine: ChartElementDTO? = null
    var alLSPD: List<LiquidStatePeriodData>? = null

    constructor(aContainerType: Int, aALine: ChartElementDTO, aAlLSPD: List<LiquidStatePeriodData>) {
        containerType = aContainerType
        aLine = aALine
        alLSPD = aAlLSPD

        if (aLine!!.alGLD.isNotEmpty()) {
            begLevel = aLine!!.alGLD[0].y.toDouble()
            endLevel = aLine!!.alGLD[aLine!!.alGLD.size - 1].y.toDouble()
        }
    }
}
