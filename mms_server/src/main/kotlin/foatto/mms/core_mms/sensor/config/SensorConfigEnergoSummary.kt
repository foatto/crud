package foatto.mms.core_mms.sensor.config

class SensorConfigEnergoSummary(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    aMinIgnore: Double,
    aMaxIgnore: Double,
) : SensorConfigBase(
    aId = aId,
    aName = aName,
    aGroup = aGroup,
    aDescr = aDescr,
    aPortNum = aPortNum,
    aSensorType = aSensorType,
    minIgnore = aMinIgnore,
    maxIgnore = aMaxIgnore,
)