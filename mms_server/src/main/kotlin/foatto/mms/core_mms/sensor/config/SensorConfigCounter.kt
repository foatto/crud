package foatto.mms.core_mms.sensor.config

class SensorConfigCounter(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    aSmoothMethod: Int,
    aSmoothTime: Int,
    aMinIgnore: Double,
    aMaxIgnore: Double,
    val isAbsoluteCount: Boolean,
    val liquidName: String
) : SensorConfigBase(
    aId = aId,
    aName = aName,
    aGroup = aGroup,
    aDescr = aDescr,
    aPortNum = aPortNum,
    aSensorType = aSensorType,
    smoothMethod = aSmoothMethod,
    smoothTime = aSmoothTime,
    minIgnore = aMinIgnore,
    maxIgnore = aMaxIgnore,
)
