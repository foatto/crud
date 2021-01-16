package foatto.mms.core_mms.sensor.config

open class SensorConfigAnalogue(
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
    val minView: Double,
    val maxView: Double,
    val minLimit: Double,
    val maxLimit: Double,
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
