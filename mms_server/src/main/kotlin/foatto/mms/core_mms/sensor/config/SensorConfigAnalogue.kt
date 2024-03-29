package foatto.mms.core_mms.sensor.config

open class SensorConfigAnalogue(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    aSerialNo: String,
    aBegYe: Int,
    aBegMo: Int,
    aBegDa: Int,
    val smoothMethod: Int,
    val smoothTime: Int,
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
    aSerialNo = aSerialNo,
    aBegYe = aBegYe,
    aBegMo = aBegMo,
    aBegDa = aBegDa,
    minIgnore = aMinIgnore,
    maxIgnore = aMaxIgnore,
)
