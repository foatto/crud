package foatto.mms.core_mms.sensor.config

class SensorConfigWork(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val boundValue: Int,
    val activeValue: Int,
    val minOnTime: Int,
    val minOffTime: Int,
    val begWorkValue: Double,
    val cmdOnId: Int,
    val cmdOffId: Int,
    val signalOn: SignalConfig,
    val signalOff: SignalConfig,
    val minIgnore: Double,
    val maxIgnore: Double,
    val liquidName: String,
    val liquidNorm: Double
) : SensorConfig(aId, aName, aGroup, aDescr, aPortNum, aSensorType)