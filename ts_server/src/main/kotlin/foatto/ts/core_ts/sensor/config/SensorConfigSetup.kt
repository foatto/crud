package foatto.ts.core_ts.sensor.config

class SensorConfigSetup(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val showPos: Int,
    val valueType: Int,
    val prec: Int,
) : SensorConfig(
    id = aId,
    name = aName,
    group = aGroup,
    descr = aDescr,
    portNum = aPortNum,
    sensorType = aSensorType
) {

    companion object {
        val VALUE_TYPE_NUMBER = 1
        val VALUE_TYPE_BOOLEAN = 2
    }
}
