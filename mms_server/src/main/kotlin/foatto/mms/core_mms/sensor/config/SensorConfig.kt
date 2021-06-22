package foatto.mms.core_mms.sensor.config

open class SensorConfig(
    val id: Int,
    val name: String,       // внутреннее/системное имя датчика, необходимо для поиска/идентификации при программном добавлении датчиков ( например, из дпнных измерений )
    val group: String,      // имя группы датчиков для логической связки разнотипных датчиков в пределах одного графика/отчёта
    val descr: String,      // видимое/выводимое описание датчика
    val portNum: Int,
    val sensorType: Int,
) {

    companion object {

        //--- предопределённый номер порта для гео-датика
        const val GEO_PORT_NUM = 19

        //--- предопределённый размер гео-данных (  wgsX + wgsY + speed + distance = 4 + 4 + 2 + 4 = 14  )
        const val GEO_DATA_SIZE = 14

        //--- логические входы с датчиков

        //--- особые величины - сигнал с устройства
        const val SENSOR_SIGNAL = -2   // Есть/нет

        //--- составной датчик - гео-данных ( координаты,скорость,пробег )
        const val SENSOR_GEO = -1           // координаты, км/ч, м или км

        //--- учётные величины - отчёты
        const val SENSOR_WORK = 1           // мото * час

        //--- контрольные величины - графики
        const val SENSOR_LIQUID_FLOW_CALC = 3
        const val SENSOR_LIQUID_LEVEL = 4
        const val SENSOR_WEIGHT = 5
        const val SENSOR_TURN = 6
        const val SENSOR_PRESSURE = 7
        const val SENSOR_TEMPERATURE = 8
        const val SENSOR_VOLTAGE = 9
        const val SENSOR_POWER = 10

        //--- датчики, подключенные через массомер ЭМИС

        //--- контрольные величины - графики
        const val SENSOR_DENSITY = 11
        const val SENSOR_MASS_FLOW = 12
        const val SENSOR_VOLUME_FLOW = 13

        //--- учётные величины - отчёты
        const val SENSOR_MASS_ACCUMULATED = 14
        const val SENSOR_VOLUME_ACCUMULATED = 15

        //--- учётные величины - отчёты
        const val SENSOR_LIQUID_USING = 16   // расходомер/счётчик

        //--- сигнальная величина - состояние расходомера/счётчика
        const val SENSOR_LIQUID_USING_COUNTER_STATE = 17

        //--- electro energo counters

        //--- учётные величины - отчёты
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

        init {
            hmSensorDescr[SENSOR_SIGNAL] = "Сигнал"
            hmSensorDescr[SENSOR_GEO] = "Гео-данные"
            hmSensorDescr[SENSOR_WORK] = "Работа оборудования"
            hmSensorDescr[SENSOR_LIQUID_FLOW_CALC] = "Скорость расхода топлива (расчётная)"
            hmSensorDescr[SENSOR_LIQUID_LEVEL] = "Уровень топлива"
            hmSensorDescr[SENSOR_WEIGHT] = "Вес"
            hmSensorDescr[SENSOR_TURN] = "Обороты"
            hmSensorDescr[SENSOR_PRESSURE] = "Давление"
            hmSensorDescr[SENSOR_TEMPERATURE] = "Температура"
            hmSensorDescr[SENSOR_VOLTAGE] = "Напряжение"
            hmSensorDescr[SENSOR_POWER] = "Мощность"
            hmSensorDescr[SENSOR_DENSITY] = "Плотность"
            hmSensorDescr[SENSOR_MASS_FLOW] = "Массовый расход"
            hmSensorDescr[SENSOR_VOLUME_FLOW] = "Объёмный расход"
            hmSensorDescr[SENSOR_MASS_ACCUMULATED] = "Накопленная масса"
            hmSensorDescr[SENSOR_VOLUME_ACCUMULATED] = "Накопленный объём"
            hmSensorDescr[SENSOR_LIQUID_USING] = "Расход топлива (счётчик)"
            hmSensorDescr[SENSOR_LIQUID_USING_COUNTER_STATE] = "Состояние расходомера (счётчика)"
            hmSensorDescr[SENSOR_ENERGO_COUNT_AD] = "Электроэнергия активная прямая"
            hmSensorDescr[SENSOR_ENERGO_COUNT_AR] = "Электроэнергия активная обратная"
            hmSensorDescr[SENSOR_ENERGO_COUNT_RD] = "Электроэнергия реактивная прямая"
            hmSensorDescr[SENSOR_ENERGO_COUNT_RR] = "Электроэнергия реактивная обратная"
            hmSensorDescr[SENSOR_ENERGO_VOLTAGE] = "Электрическое напряжение"
            hmSensorDescr[SENSOR_ENERGO_CURRENT] = "Электрический ток"
            hmSensorDescr[SENSOR_ENERGO_POWER_KOEF] = "Коэффициент мощности"
            hmSensorDescr[SENSOR_ENERGO_POWER_ACTIVE] = "Активная мощность"
            hmSensorDescr[SENSOR_ENERGO_POWER_REACTIVE] = "Реактивная мощность"
            hmSensorDescr[SENSOR_ENERGO_POWER_FULL] = "Полная мощность"
        }
    }
}

