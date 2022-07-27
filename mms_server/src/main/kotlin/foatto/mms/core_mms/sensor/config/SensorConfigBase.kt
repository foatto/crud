package foatto.mms.core_mms.sensor.config

abstract class SensorConfigBase(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val minIgnore: Double,
    val maxIgnore: Double,
) : SensorConfig(
    id = aId,
    name = aName,
    group = aGroup,
    descr = aDescr,
    portNum = aPortNum,
    sensorType = aSensorType
) {

    //--- must be double, so as not to run into errors with integer division later
    val alValueSensor = mutableListOf<Double>()
    val alValueData = mutableListOf<Double>()
}
