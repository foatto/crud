package foatto.mms.core_mms.sensor

open class SensorConfigSemiAnalogue(
    aId: Int,
    aName: String,
    aSumGroup: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
    val minView: Double,
    val maxView: Double,
    val minLimit: Double,
    val maxLimit: Double,
    val smoothMethod: Int,
    val smoothTime: Int,
    val minIgnore: Int,
    val maxIgnore: Int
) : SensorConfig(aId, aName, aSumGroup, aGroup, aDescr, aPortNum, aSensorType) {

    //--- должно быть обязательно double, чтобы позже не нарываться на ошибки с целочисленным делением
    val alValueSensor = mutableListOf<Double>()
    val alValueData = mutableListOf<Double>()

}
