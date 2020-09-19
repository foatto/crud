package foatto.mms.core_mms.sensor

class SensorConfigSignal(
    aId: Int,
    aName: String,
    aSumGroup: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val boundValue: Int,
    val activeValue: Int,
    val minIgnore: Int,
    val maxIgnore: Int
) : SensorConfig(aId, aName, aSumGroup, aGroup, aDescr, aPortNum, aSensorType)