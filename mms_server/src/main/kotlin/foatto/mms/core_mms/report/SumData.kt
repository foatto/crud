package foatto.mms.core_mms.report

import foatto.mms.core_mms.calc.GeoCalcData
import java.util.*

class SumData {

    var count = 0

    var gcd = GeoCalcData()
    var tmWorkCalc = TreeMap<String, Int>()
    var tmLiquidUsingTotal = TreeMap<String, Double>()
    var tmLiquidUsingCalc = TreeMap<String, Double>()
    var tmEnergoCalc = TreeMap<String, Double>()
}
