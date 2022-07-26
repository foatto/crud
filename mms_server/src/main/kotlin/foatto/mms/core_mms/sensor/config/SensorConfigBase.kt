package foatto.mms.core_mms.sensor.config

abstract class SensorConfigBase(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val smoothMethod: Int,
    val smoothTime: Int,
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

    companion object {
        const val CALC_TYPE_IN = 0    // входящий счётчик
        const val CALC_TYPE_OUT = 1   // исходящий счётчик
    }

    //--- must be double, so as not to run into errors with integer division later
    val alValueSensor = mutableListOf<Double>()
    val alValueData = mutableListOf<Double>()
}
