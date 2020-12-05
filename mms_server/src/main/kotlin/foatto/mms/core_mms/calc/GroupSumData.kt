package foatto.mms.core_mms.calc

import java.util.*

class GroupSumData {

    //--- working sensors summary

    var onTime = 0

    //--- liquid using by name

    val tmLiquidUsingTotal = TreeMap<String, Double>()
    val tmLiquidUsingCalc = TreeMap<String, Double>()

    //--- energo using by type and phase

    val tmEnergoUsing = TreeMap<Int, TreeMap<Int, Double>>()

    //--- liquid level

    var begLevel = 0.0
    var endLevel = 0.0

    var incTotal = 0.0
    var decTotal = 0.0

    //--------------------------------------------------------------

    fun addWorkData(aOnTime: Int) {
        onTime += aOnTime
    }

    fun addLiquidUsing(aLiquidName: String, aTotal: Double, aCalc: Double) {
        val curTotal = tmLiquidUsingTotal[aLiquidName] ?: 0.0
        tmLiquidUsingTotal[aLiquidName] = curTotal + aTotal

        val curCalc = tmLiquidUsingCalc[aLiquidName] ?: 0.0
        tmLiquidUsingCalc[aLiquidName] = curCalc + aCalc
    }

    fun addLiquidLevel(aBegLevel: Double, aEndLevel: Double, aIncTotal: Double, aDecTotal: Double) {
        begLevel += aBegLevel
        endLevel += aEndLevel

        incTotal += aIncTotal
        decTotal += aDecTotal
    }

}