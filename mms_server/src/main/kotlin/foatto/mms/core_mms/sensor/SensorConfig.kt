package foatto.mms.core_mms.sensor

open class SensorConfig(
    val id: Int,
    val name: String,       // внутреннее/системное имя датчика, необходимо для поиска/идентификации при программном добавлении датчиков ( например, из дпнных измерений )
    val sumGroup: String,   // имя группы датчиков для суммирования однотипных датчиков в пределах одного графика/отчёта
    val group: String,      // имя группы датчиков для логической связки разнотипных датчиков в пределах одного графика/отчёта
    val descr: String,      // видимое/выводимое описание датчика
    val portNum: Int,
    val sensorType: Int
) {

    companion object {

        //--- предопределённый номер порта для гео-датика
        const val GEO_PORT_NUM = 19

        //--- предопределённый размер гео-данных (  wgsX + wgsY + speed + distance = 4 + 4 + 2 + 4 = 14  )
        const val GEO_DATA_SIZE = 14

        //--- логические входы с датчиков

        //--- особые величины - сигнал с утройства
        const val SENSOR_SIGNAL = -2   // Есть/нет

        //--- составной датчик - гео-данных ( координаты,скорость,пробег )
        const val SENSOR_GEO = -1           // координаты, км/ч, м или км

        //--- учётные величины - отчёты
        const val SENSOR_WORK = 1           // мото * час
        //--- вместо этого будут два датчика: SENSOR_MASS_FLOW и SENSOR_VOLUME_FLOW
        //const val SENSOR_LIQUID_USING = 2   // литр

        //--- контрольные величины - графики
        const val SENSOR_LIQUID_FLOW_CALC = 3   // литр * час - расчётный, по изменению уровня
        const val SENSOR_LIQUID_LEVEL = 4   // литр
        const val SENSOR_WEIGHT = 5         // кг
        const val SENSOR_TURN = 6           // об / мин
        const val SENSOR_PRESSURE = 7       // кПа или атм?
        const val SENSOR_TEMPERATURE = 8    // С
        const val SENSOR_VOLTAGE = 9        // В
        const val SENSOR_POWER = 10         // Вт или л.с.?

        //--- датчики, подключенные через массомер ЭМИС

        const val SENSOR_DENSITY = 11
        const val SENSOR_MASS_FLOW = 12
        const val SENSOR_VOLUME_FLOW = 13
        const val SENSOR_MASS_ACCUMULATED = 14
        const val SENSOR_VOLUME_ACCUMULATED = 15

        //--- electro energo counters

        //--- учётные величины - отчёты
        //--- Вт*ч - выводим как кВт*ч
        const val SENSOR_ENERGO_COUNT_AD = 20   // Active Direct - активная прямая электроэнергия
        const val SENSOR_ENERGO_COUNT_AR = 21   // Active Reverse - активная обратная электроэнергия
        const val SENSOR_ENERGO_COUNT_RD = 22   // Reactive Direct - реактивная прямая электроэнергия
        const val SENSOR_ENERGO_COUNT_RR = 23   // Reactive Reverse - реактивная обратная электроэнергия

        //--- контрольные величины - графики

        const val SENSOR_ENERGO_VOLTAGE = 30        // voltage by phase
        const val SENSOR_ENERGO_CURRENT = 31        // current by phase
        const val SENSOR_ENERGO_POWER_KOEF = 32     // power koeff by phase
        const val SENSOR_ENERGO_POWER_ACTIVE = 33   // active power by phase
        const val SENSOR_ENERGO_POWER_REACTIVE = 34 // reactive power by phase
        const val SENSOR_ENERGO_POWER_FULL = 35     // full/summary power by phase

        //--- алгоритмы усреднения:
        //--- медиана
        const val SMOOTH_METOD_MEDIAN = 0

        //--- среднее арифметическое
        const val SMOOTH_METOD_AVERAGE = 1

        //--- среднее квадратическое
        const val SMOOTH_METOD_AVERAGE_SQUARE = 2

        //--- среднее геометрическое
        const val SMOOTH_METOD_AVERAGE_GEOMETRIC = 3

        //--- названия датчиков
        val hmSensorDescr = mutableMapOf<Int, String>()

        //--- типы датчиков, к которым не применяется сглаживание
        val hsSensorNonSmooth = mutableSetOf<Int>()

        //--- типы датчиков, у которых не бывает калибровки
        val hsSensorNonCalibration = mutableSetOf<Int>()

        init {
            hmSensorDescr[SENSOR_SIGNAL] = "Сигнал"
            hmSensorDescr[SENSOR_GEO] = "Гео-данные"
            hmSensorDescr[SENSOR_WORK] = "Работа оборудования"
            //hmSensorDescr[ SENSOR_LIQUID_USING ] = "Расход топлива"
            hmSensorDescr[SENSOR_LIQUID_FLOW_CALC] = "Скорость расхода топлива (расчётная)"
            hmSensorDescr[SENSOR_LIQUID_LEVEL] = "Уровень топлива"
            hmSensorDescr[SENSOR_WEIGHT] = "Нагрузка"
            hmSensorDescr[SENSOR_TURN] = "Обороты"
            hmSensorDescr[SENSOR_PRESSURE] = "Давление"
            hmSensorDescr[SENSOR_TEMPERATURE] = "Температура"
            hmSensorDescr[SENSOR_VOLTAGE] = "Напряжение"
            hmSensorDescr[SENSOR_POWER] = "Мощность"
            hmSensorDescr[SENSOR_DENSITY] = "Плотность"
            hmSensorDescr[SENSOR_MASS_FLOW] = "Массовый расход"
            hmSensorDescr[SENSOR_VOLUME_FLOW] = "Объёмный расход"
            hmSensorDescr[SENSOR_MASS_ACCUMULATED] = "Накопленная масся"
            hmSensorDescr[SENSOR_VOLUME_ACCUMULATED] = "Накопленный объём"
            hmSensorDescr[SENSOR_ENERGO_COUNT_AD] = "Электроэнергия активная прямая"
            hmSensorDescr[SENSOR_ENERGO_COUNT_AR] = "Электроэнергия активная обратная"
            hmSensorDescr[SENSOR_ENERGO_COUNT_RD] = "Электроэнергия реактивная прямая"
            hmSensorDescr[SENSOR_ENERGO_COUNT_RR] = "Электроэнергия реактивная обратная"
            hmSensorDescr[SENSOR_ENERGO_VOLTAGE] = "Напряжение по фазе"
            hmSensorDescr[SENSOR_ENERGO_CURRENT] = "Ток по фазе"
            hmSensorDescr[SENSOR_ENERGO_POWER_KOEF] = "Коэффициент мощности по фазе"
            hmSensorDescr[SENSOR_ENERGO_POWER_ACTIVE] = "Активная мощность по фазе"
            hmSensorDescr[SENSOR_ENERGO_POWER_REACTIVE] = "Реактивная мощность по фазе"
            hmSensorDescr[SENSOR_ENERGO_POWER_FULL] = "Полная мощность по фазе"

            hsSensorNonSmooth.add(SENSOR_SIGNAL)
            hsSensorNonSmooth.add(SENSOR_GEO)
            hsSensorNonSmooth.add(SENSOR_WORK)
            //hsSensorNonSmooth.add( SENSOR_LIQUID_USING )
            hsSensorNonSmooth.add(SENSOR_MASS_ACCUMULATED)
            hsSensorNonSmooth.add(SENSOR_VOLUME_ACCUMULATED)
            hsSensorNonSmooth.add(SENSOR_ENERGO_COUNT_AD)
            hsSensorNonSmooth.add(SENSOR_ENERGO_COUNT_AR)
            hsSensorNonSmooth.add(SENSOR_ENERGO_COUNT_RD)
            hsSensorNonSmooth.add( SENSOR_ENERGO_COUNT_RR )

            hsSensorNonCalibration.add( SENSOR_SIGNAL )
            hsSensorNonCalibration.add( SENSOR_GEO )
            hsSensorNonCalibration.add( SENSOR_WORK )
            //hsSensorNonCalibration.add( SENSOR_LIQUID_USING )
            hsSensorNonCalibration.add( SENSOR_MASS_ACCUMULATED )
            hsSensorNonCalibration.add( SENSOR_VOLUME_ACCUMULATED )
            hsSensorNonCalibration.add( SENSOR_ENERGO_COUNT_AD )
            hsSensorNonCalibration.add( SENSOR_ENERGO_COUNT_AR )
            hsSensorNonCalibration.add( SENSOR_ENERGO_COUNT_RD )
            hsSensorNonCalibration.add( SENSOR_ENERGO_COUNT_RR )
        }
    }
}

