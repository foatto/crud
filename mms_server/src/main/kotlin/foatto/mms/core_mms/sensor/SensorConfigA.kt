package foatto.mms.core_mms.sensor

class SensorConfigA( aId: Int, aName: String, aSumGroup: String, aGroup: String, aDescr: String, aPortNum: Int, aSensorType: Int,
                     val dim: String, val minView: Double, val maxView: Double, val minLimit: Double, val maxLimit: Double,
                     val usingMinLen: Int, val isUsingCalc: Boolean,
                     val detectIncKoef: Double, val detectIncMinDiff: Double, val detectIncMinLen: Int, val incAddTimeBefore: Int, val incAddTimeAfter: Int,
                     val detectDecKoef: Double, val detectDecMinDiff: Double, val detectDecMinLen: Int, val decAddTimeBefore: Int, val decAddTimeAfter: Int,
                     val smoothMethod: Int, val smoothTime: Int,
                     val minIgnore: Int, val maxIgnore: Int,
                     val liquidName: String ) : SensorConfig( aId, aName, aSumGroup, aGroup, aDescr, aPortNum, aSensorType ) {

    //--- должно быть обязательно double, чтобы позже не нарываться на ошибки с целочисленным делением
    val alValueSensor = mutableListOf<Double>()
    val alValueData = mutableListOf<Double>()

    companion object {

        //--- алгоритмы усреднения:
        //--- медиана
        const val SMOOTH_METOD_MEDIAN = 0
        //--- среднее арифметическое
        const val SMOOTH_METOD_AVERAGE = 1
        //--- среднее квадратическое
        const val SMOOTH_METOD_AVERAGE_SQUARE = 2
        //--- среднее геометрическое
        const val SMOOTH_METOD_AVERAGE_GEOMETRIC = 3

        val hmLLErrorCodeDescr = mutableMapOf<Int, String>()

        init {
            hmLLErrorCodeDescr[ 0 ] = "Нет данных с датчика уровня"
            hmLLErrorCodeDescr[ 6500 ] = "Замыкание трубки датчика уровня"
            hmLLErrorCodeDescr[ 7500 ] = "Обрыв трубки датчика уровня"
            hmLLErrorCodeDescr[ 9998 ] = "Отсутствие данных от измерителя"
            hmLLErrorCodeDescr[ 9999 ] = "Отсутствие связи с передатчиком"
        }

        val hmLLMinSensorErrorTime = mutableMapOf<Int, Int>()

        init {
            hmLLMinSensorErrorTime[ 0 ] = 15 * 60
            hmLLMinSensorErrorTime[ 6500 ] = 0
            hmLLMinSensorErrorTime[ 7500 ] = 0
            hmLLMinSensorErrorTime[ 9998 ] = 0
            hmLLMinSensorErrorTime[ 9999 ] = 0
        }
    }
}
