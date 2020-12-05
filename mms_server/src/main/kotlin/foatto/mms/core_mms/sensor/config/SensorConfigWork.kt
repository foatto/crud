package foatto.mms.core_mms.sensor.config

class SensorConfigWork(
    aId: Int,
    aName: String,
    aSumGroup: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val boundValue: Int,
    val activeValue: Int,
    val minOnTime: Int,
    val minOffTime: Int,
    val begWorkValue: Double,
    val cmdOnID: Int,
    val cmdOffID: Int,
    aSignalOn: String,
    aSignalOff: String,
    val minIgnore: Double,
    val maxIgnore: Double,
    val liquidName: String,
    val liquidNorm: Double
) : SensorConfig(aId, aName, aSumGroup, aGroup, aDescr, aPortNum, aSensorType) {

    val signalOn = SignalConfig(aSignalOn)
    val signalOff = SignalConfig(aSignalOff)
}
