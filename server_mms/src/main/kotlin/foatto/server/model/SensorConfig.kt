package foatto.server.model

class SensorConfig {

    companion object {

        //--- предопределённый номер порта для гео-датика
        const val GEO_PORT_NUM = 19

        //--- предопределённый размер гео-данных (wgsX + wgsY + speed + distance = 4 + 4 + 2 + 4 = 14)
        const val GEO_DATA_SIZE = 14

        //--- логические входы с датчиков

        //--- особые величины - сигнал с устройства
        const val SENSOR_SIGNAL = -2   // Есть/нет

        //--- составной датчик - гео-данных ( координаты,скорость,пробег )
        const val SENSOR_GEO = -1           // координаты, км/ч, м или км

        //--- учётные величины - отчёты
        const val SENSOR_WORK = 1           // мото * час

        //--- контрольные величины - графики
        //const val SENSOR_LIQUID_FLOW_CALC = 3 - считается автоматически при записи данных со счётчика расхода
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

        //--- Исторически сложилось так, что это три разных датчика (трёх разных производителей и возможностей)
        //--- но используются они пока одинаково.
        //--- Возможно, когда-нибудь их удастся объединить.
        const val SENSOR_MASS_ACCUMULATED = 14
        const val SENSOR_VOLUME_ACCUMULATED = 15
        const val SENSOR_LIQUID_USING = 16

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
        const val SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT = 36 // transformation koeff by current
        const val SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE = 37  // transformation koeff by voltage

        //--- отдельный список аналоговых датчиков, чтобы не перечислять их каждый раз (и не пропустить в очередном перечислении)
        val analogueSensorTypes: Set<Int> = setOf(
            SENSOR_LIQUID_LEVEL,
            SENSOR_WEIGHT,
            SENSOR_TURN,
            SENSOR_PRESSURE,
            SENSOR_TEMPERATURE,
            SENSOR_VOLTAGE,
            SENSOR_POWER,
            SENSOR_DENSITY,
            SENSOR_MASS_FLOW,
            SENSOR_VOLUME_FLOW,
            SENSOR_ENERGO_VOLTAGE,
            SENSOR_ENERGO_CURRENT,
            SENSOR_ENERGO_POWER_KOEF,
            SENSOR_ENERGO_POWER_ACTIVE,
            SENSOR_ENERGO_POWER_REACTIVE,
            SENSOR_ENERGO_POWER_FULL,
            SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
            SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
        )

        //--- отдельный список счётных датчиков, чтобы не перечислять их каждый раз (и не пропустить в очередном перечислении)
        val counterSensorTypes: Set<Int> = setOf(
            SENSOR_MASS_ACCUMULATED,
            SENSOR_VOLUME_ACCUMULATED,
            SENSOR_LIQUID_USING,
            SENSOR_ENERGO_COUNT_AD,
            SENSOR_ENERGO_COUNT_AR,
            SENSOR_ENERGO_COUNT_RD,
            SENSOR_ENERGO_COUNT_RR,
        )

        //--- перевёрнутые графики для датчиков
        private val reversedChartSensorTypes = setOf<Int>(/* SensorConfig.SENSOR_DEPTH */)
        fun isReversedChart(sensorType: Int?): Boolean = reversedChartSensorTypes.contains(sensorType)

        //--- названия датчиков
        val hmSensorDescr: Map<Int, String> = mapOf(
            SENSOR_SIGNAL to "Сигнал",
            SENSOR_GEO to "Гео-данные",
            SENSOR_WORK to "Работа оборудования",
            SENSOR_LIQUID_LEVEL to "Уровень топлива",
            SENSOR_WEIGHT to "Вес",
            SENSOR_TURN to "Обороты",
            SENSOR_PRESSURE to "Давление",
            SENSOR_TEMPERATURE to "Температура",
            SENSOR_VOLTAGE to "Напряжение",
            SENSOR_POWER to "Мощность",
            SENSOR_DENSITY to "Плотность",
            SENSOR_MASS_FLOW to "Массовый расход",
            SENSOR_VOLUME_FLOW to "Объёмный расход",
            SENSOR_MASS_ACCUMULATED to "Накопленная масса",
            SENSOR_VOLUME_ACCUMULATED to "Накопленный объём",
            SENSOR_LIQUID_USING to "Расход топлива",
            SENSOR_LIQUID_USING_COUNTER_STATE to "Состояние расходомера",
            SENSOR_ENERGO_COUNT_AD to "Электроэнергия активная прямая",
            SENSOR_ENERGO_COUNT_AR to "Электроэнергия активная обратная",
            SENSOR_ENERGO_COUNT_RD to "Электроэнергия реактивная прямая",
            SENSOR_ENERGO_COUNT_RR to "Электроэнергия реактивная обратная",
            SENSOR_ENERGO_VOLTAGE to "Электрическое напряжение",
            SENSOR_ENERGO_CURRENT to "Электрический ток",
            SENSOR_ENERGO_POWER_KOEF to "Коэффициент мощности",
            SENSOR_ENERGO_POWER_ACTIVE to "Активная мощность",
            SENSOR_ENERGO_POWER_REACTIVE to "Реактивная мощность",
            SENSOR_ENERGO_POWER_FULL to "Полная мощность",
            SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT to "Коэффициент трансформации по току",
            SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE to "Коэффициент трансформации по напряжению",
        )
    }
}