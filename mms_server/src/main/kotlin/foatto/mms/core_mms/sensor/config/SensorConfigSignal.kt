package foatto.mms.core_mms.sensor.config

class SensorConfigSignal(
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
    val boundValue: Int,
    val activeValue: Int,
    val minIgnore: Double,
    val maxIgnore: Double
) : SensorConfig(
    id = aId,
    name = aName,
    group = aGroup,
    descr = aDescr,
    portNum = aPortNum,
    sensorType = aSensorType,
    serialNo = aSerialNo,
    begYe = aBegYe,
    begMo = aBegMo,
    begDa = aBegDa,
)