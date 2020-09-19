package foatto.mms.core_mms.sensor

class SensorConfigAnalogue(
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
    val usingMinLen: Int,
    val isUsingCalc: Boolean,
    val detectIncKoef: Double,
    val detectIncMinDiff: Double,
    val detectIncMinLen: Int,
    val incAddTimeBefore: Int,
    val incAddTimeAfter: Int,
    val detectDecKoef: Double,
    val detectDecMinDiff: Double,
    val detectDecMinLen: Int,
    val decAddTimeBefore: Int,
    val decAddTimeAfter: Int,
    smoothMethod: Int,
    smoothTime: Int,
    minIgnore: Int,
    maxIgnore: Int,
    val liquidName: String
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
    smoothMethod = smoothMethod,
    smoothTime = smoothTime,
    minIgnore = minIgnore,
    maxIgnore = maxIgnore
) {

    companion object {

        val hmLLErrorCodeDescr = mutableMapOf<Int, String>()

        init {
            hmLLErrorCodeDescr[0] = "Нет данных с датчика уровня"             // No data from the (liquid/fuel) level sensor
            hmLLErrorCodeDescr[6500] = "Замыкание трубки датчика уровня"      // Short circuit of the level sensor tube
            hmLLErrorCodeDescr[7500] = "Обрыв трубки датчика уровня"          // Breakage of the level sensor tube
            hmLLErrorCodeDescr[9998] = "Отсутствие данных от измерителя"      // Lack of data from the meter
            hmLLErrorCodeDescr[9999] = "Отсутствие связи с передатчиком"      // Lack of communication with the transmitter
        }

        val hmLLMinSensorErrorTime = mutableMapOf<Int, Int>()

        init {
            hmLLMinSensorErrorTime[0] = 15 * 60
            hmLLMinSensorErrorTime[6500] = 0
            hmLLMinSensorErrorTime[7500] = 0
            hmLLMinSensorErrorTime[9998] = 0
            hmLLMinSensorErrorTime[9999] = 0
        }
    }
}
