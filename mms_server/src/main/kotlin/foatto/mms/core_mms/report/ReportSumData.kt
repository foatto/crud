package foatto.mms.core_mms.report

import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import java.util.*

class ReportSumData {

    var scg: SensorConfigGeo? = null
    var run = 0.0
    var movingTime = 0
    var parkingTime = 0
    var parkingCount = 0

    val tmWork = TreeMap<String, Int>()

    val tmEnergo = TreeMap<Int, TreeMap<Int, Double>>()
    val tmLiquidUsing = TreeMap<String, Double>()
    val tmLiquidIncDec = TreeMap<String, Pair<Double, Double>>()
}
