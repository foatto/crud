package foatto.mms.core_mms.report

import foatto.mms.core_mms.calc.GeoCalcData
import foatto.mms.core_mms.calc.LiquidUsingCalcData
import foatto.mms.core_mms.calc.WorkCalcData
import java.util.*

class SumData {

    var count = 0

    var gcd = GeoCalcData()
    var tmWorkCalc = TreeMap<String, WorkCalcData>()
    var tmLiquidUsingCalc = TreeMap<String, LiquidUsingCalcData>()
    var tmEnergoCalc = TreeMap<String, Int>()
}
