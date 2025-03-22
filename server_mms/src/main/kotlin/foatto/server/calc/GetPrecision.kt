package foatto.server.calc

import kotlin.math.abs

fun getPrecision(value: Double): Int {
    val absValue = abs(value)
    //--- updated / simplified version of the output accuracy - more cubic meters - in whole liters, less - in hundreds of milliliters / gram
    return if (absValue >= 1000) 0
    else if (absValue >= 100) 1
    else if (absValue >= 10) 2
    else 3
}

fun getPrecision(value: Float): Int = getPrecision(value.toDouble())
