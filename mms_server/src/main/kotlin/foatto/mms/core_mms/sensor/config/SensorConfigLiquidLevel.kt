package foatto.mms.core_mms.sensor.config

class SensorConfigLiquidLevel(
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
    aSmoothMethod: Int,
    aSmoothTime: Int,
    aMinIgnore: Double,
    aMaxIgnore: Double,
    val containerType: Int,
    val liquidName: String,
    aMinView: Double,
    aMaxView: Double,
    aMinLimit: Double,
    aMaxLimit: Double,
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
) : SensorConfigAnalogue(
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
    smoothMethod = aSmoothMethod,
    smoothTime = aSmoothTime,
    aMinIgnore = aMinIgnore,
    aMaxIgnore = aMaxIgnore,
    minView = aMinView,
    maxView = aMaxView,
    minLimit = aMinLimit,
    maxLimit = aMaxLimit,
) {

    companion object {

        val CONTAINER_TYPE_MAIN = 0     // основная ёмкость
        val CONTAINER_TYPE_WORK = 1     // рабочая/расходная ёмкость

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
