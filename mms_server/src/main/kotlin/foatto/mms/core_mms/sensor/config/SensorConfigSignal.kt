package foatto.mms.core_mms.sensor.config

class SensorConfigSignal(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val boundValue: Int,
    val activeValue: Int,
    val minIgnore: Double,
    val maxIgnore: Double
) : SensorConfig(aId, aName, aGroup, aDescr, aPortNum, aSensorType)