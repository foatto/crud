package foatto.mms.core_mms.calc

import foatto.core.app.chart.ChartElementDTO
import foatto.mms.core_mms.sensor.config.SensorConfigCounter

class CounterCalcData(
    val scc: SensorConfigCounter,

    val value: Double,
) {
    var density: ChartElementDTO? = null
}