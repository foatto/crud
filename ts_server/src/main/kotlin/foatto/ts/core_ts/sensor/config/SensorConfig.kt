package foatto.ts.core_ts.sensor.config

open class SensorConfig(
    val id: Int,
    val name: String,       // внутреннее/системное имя датчика, необходимо для поиска/идентификации при программном добавлении датчиков ( например, из дпнных измерений )
    val group: String,      // имя группы датчиков для логической связки разнотипных датчиков в пределах одного графика/отчёта
    val descr: String,      // видимое/выводимое описание датчика
    val portNum: Int,
    val sensorType: Int,
) {

    companion object {

        const val SENSOR_STATE = 1

        const val SENSOR_DEPTH = 2
        const val SENSOR_SPEED = 3
        const val SENSOR_LOAD = 4

        const val SENSOR_TEMPERATURE_IN = 5
        const val SENSOR_TEMPERATURE_OUT = 6

        //--- алгоритмы усреднения:
        //--- медиана
        const val SMOOTH_METOD_MEDIAN = 0

        //--- среднее арифметическое
        const val SMOOTH_METOD_AVERAGE = 1

        //--- названия датчиков
        val hmSensorDescr = mutableMapOf<Int, String>()

        init {
            hmSensorDescr[SENSOR_STATE] = "Код текущего состояния"
            hmSensorDescr[SENSOR_DEPTH] = "Глубина [м]"
            hmSensorDescr[SENSOR_SPEED] = "Скорость спуска [м/ч]"
            hmSensorDescr[SENSOR_LOAD] = "Нагрузка на привод [%]"
            hmSensorDescr[SENSOR_TEMPERATURE_IN] = "Температура внутри [C]"
            hmSensorDescr[SENSOR_TEMPERATURE_OUT] = "Температура снаружи [C]"
        }
    }
}

