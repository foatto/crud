package foatto.mms.core_mms.calc

import foatto.core.app.graphic.GraphicDataContainer
import foatto.mms.core_mms.sensor.config.SensorConfigCounter

class CounterCalcData(
    val scc: SensorConfigCounter,

    val value: Double,
    val alWorkOnOff: List<AbstractPeriodData>,
) {
    var density: GraphicDataContainer? = null
}