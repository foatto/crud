package foatto.mms.core_mms.calc

import foatto.core.app.graphic.GraphicDataContainer

class LiquidLevelCalcData {

    var begLevel = 0.0
    var endLevel = 0.0

    var incTotal = 0.0
    var decTotal = 0.0

    var usingTotal = 0.0
    var usingCalc = 0.0

    var aLine: GraphicDataContainer? = null
    var alLSPD: List<LiquidStatePeriodData>? = null

    constructor(aALine: GraphicDataContainer, aAlLSPD: List<LiquidStatePeriodData>) {
        aLine = aALine
        alLSPD = aAlLSPD

        if (aLine!!.alGLD.isNotEmpty()) {
            begLevel = aLine!!.alGLD[0].y
            endLevel = aLine!!.alGLD[aLine!!.alGLD.size - 1].y
        }
    }
}
