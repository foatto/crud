package foatto.mms.core_mms.sensor

class SensorConfigElectro(
    aId: Int,
    aName: String,
    aSumGroup: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val dim: String,
    minView: Double,
    maxView: Double,
    minLimit: Double,
    maxLimit: Double,
    smoothMethod: Int,
    smoothTime: Int,
    minIgnore: Int,
    maxIgnore: Int,
    val phase: Int
) : SensorConfigSemiAnalogue(
    aId = aId,
    aName = aName,
    aSumGroup = aSumGroup,
    aGroup = aGroup,
    aDescr = aDescr,
    aPortNum = aPortNum,
    aSensorType = aSensorType,
    minView = minView,
    maxView = maxView,
    minLimit = minLimit,
    maxLimit = maxLimit,
    smoothMethod = smoothTime,
    smoothTime = smoothTime,
    minIgnore = minIgnore,
    maxIgnore = maxIgnore
)