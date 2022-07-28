package foatto.mms.core_mms.sensor.config

class SensorConfigGeo(
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
    val minMovingTime: Int,
    val minParkingTime: Int,
    val minOverSpeedTime: Int,
    val isAbsoluteRun: Boolean,
    val speedRoundRule: Int,
    val runKoef: Double,
    val isUsePos: Boolean,
    val isUseSpeed: Boolean,
    val isUseRun: Boolean,
    val liquidName: String,
    val liquidNorm: Double,
    val maxSpeedLimit: Int,
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
) {

    companion object {

        //--- правила округления дробной скорости - в меньшую сторону, стандартно (по арифметическим/банковским правилам), в большую сторону
        val SPEED_ROUND_RULE_LESS = -1
        val SPEED_ROUND_RULE_STANDART = 0
        val SPEED_ROUND_RULE_GREATER = 1
    }
}