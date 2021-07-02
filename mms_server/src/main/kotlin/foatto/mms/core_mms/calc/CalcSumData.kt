package foatto.mms.core_mms.calc

import java.util.*

class CalcSumData {

    //--- only for calculating the average fuel consumption (to check that the equipment is the only one in the group)

    var tmWork = sortedMapOf<String, Int>()

    //--- energo using by type and phase

    val tmEnergo = sortedMapOf<Int, SortedMap<Int, Double>>()

    //--- liquid using by liquid name

    val tmLiquidUsing = sortedMapOf<String, Double>()

    //--- liquid level

    val tmLiquidIncDec = sortedMapOf<String, Pair<Double, Double>>()

    //--------------------------------------------------------------

    fun addLiquidUsing(liquidName: String, using: Double) {
        val curUsing = tmLiquidUsing[liquidName] ?: 0.0
        tmLiquidUsing[liquidName] = curUsing + using
    }

    fun addLiquidLevel(liquidName: String, inc: Double, dec: Double) {
        val (curInc, curDec) = tmLiquidIncDec[liquidName] ?: Pair(0.0, 0.0)
        tmLiquidIncDec[liquidName] = Pair(curInc + inc, curDec + dec)
    }

}