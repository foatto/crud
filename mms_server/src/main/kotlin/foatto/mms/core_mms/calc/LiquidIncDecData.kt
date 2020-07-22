package foatto.mms.core_mms.calc

import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.SensorConfigA

class LiquidIncDecData( val begTime: Int, val endTime: Int, val begLevel: Double, val endLevel: Double ) {
    var objectConfig: ObjectConfig? = null
    var sca: SensorConfigA? = null
    var sbZoneName: StringBuilder? = null
}
