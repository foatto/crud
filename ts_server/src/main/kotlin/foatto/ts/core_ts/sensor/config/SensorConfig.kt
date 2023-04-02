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

        const val SENSOR_SETUP = 7
        const val SENSOR_SIGNAL_LEVEL = 8

        const val SENSOR_NEXT_CLEAN_DATETIME = 9

        //--- алгоритмы усреднения:
        //--- медиана
        const val SMOOTH_METOD_MEDIAN = 0
        //--- среднее арифметическое
        const val SMOOTH_METOD_AVERAGE = 1

        //--- названия датчиков
        val hmSensorDescr = mutableMapOf(
            SENSOR_STATE to "Код текущего состояния",
            SENSOR_DEPTH to "Глубина [м]",
            SENSOR_SPEED to "Скорость спуска [м/ч]",
            SENSOR_LOAD to "Нагрузка на привод [%]",
            SENSOR_TEMPERATURE_IN to "Температура внутри [C]",
            SENSOR_TEMPERATURE_OUT to "Температура снаружи [C]",
            SENSOR_SETUP to "Параметр настройки",
            SENSOR_SIGNAL_LEVEL to "Уровень сигнала [%]",
            SENSOR_NEXT_CLEAN_DATETIME to "Дата/время следующей чистки",
        )
    }
}

