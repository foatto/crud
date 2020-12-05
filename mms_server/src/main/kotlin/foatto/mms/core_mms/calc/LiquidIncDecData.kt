package foatto.mms.core_mms.calc

import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel

class LiquidIncDecData( val begTime: Int, val endTime: Int, val begLevel: Double, val endLevel: Double ) {
    var objectConfig: ObjectConfig? = null
    var sca: SensorConfigLiquidLevel? = null
    var sbZoneName: StringBuilder? = null
}
