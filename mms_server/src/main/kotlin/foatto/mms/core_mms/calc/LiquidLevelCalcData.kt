package foatto.mms.core_mms.calc

import foatto.core.app.graphic.GraphicDataContainer
import java.util.*

class LiquidLevelCalcData {

    var sumGroup: String? = null

    var begLevel = 0.0
    var endLevel = 0.0

    var incTotal = 0.0
    var decTotal = 0.0

    var usingTotal = 0.0
    var usingMoving = 0.0
    var usingParking = 0.0

    //--- расчётный расход во время заправки/слива - хранится отдельно для справочного вывода и
    //--- уже входит в состав usingTotal и usingParking
    //--- (используется объектный Double для простого детектирования используемости usingCalc на уровне отчётов)
    var usingCalc: Double? = null

    var aLine: GraphicDataContainer? = null
    var alLSPD: List<LiquidStatePeriodData>? = null

    var gcd: GeoCalcData? = null
    var tmWorkCalc = TreeMap<String, WorkCalcData>()
    var tmEnergoCalc = TreeMap<String, Int>()

    constructor( aSumGroup: String ) {
        sumGroup = aSumGroup
    }

    constructor( aALine: GraphicDataContainer, aAlLSPD: List<LiquidStatePeriodData> ) {
        aLine = aALine
        alLSPD = aAlLSPD

        if( !aLine!!.alGLD.isEmpty() ) {
            begLevel = aLine!!.alGLD[ 0 ].y
            endLevel = aLine!!.alGLD[ aLine!!.alGLD.size - 1 ].y
        }
    }
}
