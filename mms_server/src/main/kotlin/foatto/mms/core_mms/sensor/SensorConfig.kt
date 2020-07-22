package foatto.mms.core_mms.sensor

open class SensorConfig( val id: Int, val name: String, val sumGroup: String, val group: String, val descr: String, val portNum: Int, val sensorType: Int ) {

    //--- name: внутреннее/системное имя датчика, необходимо для поиска/идентификации при программном добавлении датчиков ( например, из дпнных измерений )
    //--- sumGroup: имя группы датчиков для суммирования однотипных датчиков в пределах одного графика/отчёта
    //--- group: имя группы датчиков для логической связки разнотипных датчиков в пределах одного графика/отчёта
    //--- descr: видимое/выводимое описание датчика

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

        //--- электросчётчик

        //--- учётные величины - отчёты
        //--- Вт*ч - выводим как кВт*ч
        const val SENSOR_ENERGO_COUNT_AD = 20   // Active Direct - активная прямая электроэнергия
        const val SENSOR_ENERGO_COUNT_AR = 21   // Active Reverse - активная обратная электроэнергия
        const val SENSOR_ENERGO_COUNT_RD = 22   // Reactive Direct - реактивная прямая электроэнергия
        const val SENSOR_ENERGO_COUNT_RR = 23   // Reactive Reverse - реактивная обратная электроэнергия

        //--- контрольные величины - графики

        //--- напряжение по фазе
        const val SENSOR_ENERGO_VOLTAGE_A = 30
        const val SENSOR_ENERGO_VOLTAGE_B = 31
        const val SENSOR_ENERGO_VOLTAGE_C = 32

        //--- ток по фазе
        const val SENSOR_ENERGO_CURRENT_A = 33
        const val SENSOR_ENERGO_CURRENT_B = 34
        const val SENSOR_ENERGO_CURRENT_C = 35

        //--- коэффициент мощности по фазе
        const val SENSOR_ENERGO_POWER_KOEF_A = 36
        const val SENSOR_ENERGO_POWER_KOEF_B = 37
        const val SENSOR_ENERGO_POWER_KOEF_C = 38

        val hmSensorDescr = mutableMapOf<Int, String>()

        init {
            hmSensorDescr[ SENSOR_SIGNAL ] = "Сигнал"
            hmSensorDescr[ SENSOR_GEO ] = "Гео-данные"
            hmSensorDescr[ SENSOR_WORK ] = "Работа оборудования"
            //hmSensorDescr[ SENSOR_LIQUID_USING ] = "Расход топлива"
            hmSensorDescr[ SENSOR_LIQUID_FLOW_CALC ] = "Скорость расхода топлива (расчётная)"
            hmSensorDescr[ SENSOR_LIQUID_LEVEL ] = "Уровень топлива"
            hmSensorDescr[ SENSOR_WEIGHT ] = "Нагрузка"
            hmSensorDescr[ SENSOR_TURN ] = "Обороты"
            hmSensorDescr[ SENSOR_PRESSURE ] = "Давление"
            hmSensorDescr[ SENSOR_TEMPERATURE ] = "Температура"
            hmSensorDescr[ SENSOR_VOLTAGE ] = "Напряжение"
            hmSensorDescr[ SENSOR_POWER ] = "Мощность"
            hmSensorDescr[ SENSOR_DENSITY ] = "Плотность"
            hmSensorDescr[ SENSOR_MASS_FLOW ] = "Массовый расход"
            hmSensorDescr[ SENSOR_VOLUME_FLOW ] = "Объёмный расход"
            hmSensorDescr[ SENSOR_MASS_ACCUMULATED ] = "Накопленная масся"
            hmSensorDescr[ SENSOR_VOLUME_ACCUMULATED ] = "Накопленный объём"
            hmSensorDescr[ SENSOR_ENERGO_COUNT_AD ] = "Электроэнергия активная прямая"
            hmSensorDescr[ SENSOR_ENERGO_COUNT_AR ] = "Электроэнергия активная прямая"
            hmSensorDescr[ SENSOR_ENERGO_COUNT_RD ] = "Электроэнергия активная прямая"
            hmSensorDescr[ SENSOR_ENERGO_COUNT_RR ] = "Электроэнергия активная прямая"
            hmSensorDescr[ SENSOR_ENERGO_VOLTAGE_A ] = "Напряжение по фазе A"
            hmSensorDescr[ SENSOR_ENERGO_VOLTAGE_B ] = "Напряжение по фазе B"
            hmSensorDescr[ SENSOR_ENERGO_VOLTAGE_C ] = "Напряжение по фазе C"
            hmSensorDescr[ SENSOR_ENERGO_CURRENT_A ] = "Ток по фазе A"
            hmSensorDescr[ SENSOR_ENERGO_CURRENT_B ] = "Ток по фазе B"
            hmSensorDescr[ SENSOR_ENERGO_CURRENT_C ] = "Ток по фазе C"
            hmSensorDescr[ SENSOR_ENERGO_POWER_KOEF_A ] = "Коэффициент мощности по фазе A"
            hmSensorDescr[ SENSOR_ENERGO_POWER_KOEF_B ] = "Коэффициент мощности по фазе B"
            hmSensorDescr[ SENSOR_ENERGO_POWER_KOEF_C ] = "Коэффициент мощности по фазе C"
        }

        //--- типы датчиков, к которым не применяется сглаживание
        val hsSensorNonSmooth = mutableSetOf<Int>()

        init {
            hsSensorNonSmooth.add( SENSOR_SIGNAL )
            hsSensorNonSmooth.add( SENSOR_GEO )
            hsSensorNonSmooth.add( SENSOR_WORK )
            //hsSensorNonSmooth.add( SENSOR_LIQUID_USING )
            hsSensorNonSmooth.add( SENSOR_MASS_ACCUMULATED )
            hsSensorNonSmooth.add( SENSOR_VOLUME_ACCUMULATED )
            hsSensorNonSmooth.add( SENSOR_ENERGO_COUNT_AD )
            hsSensorNonSmooth.add( SENSOR_ENERGO_COUNT_AR )
            hsSensorNonSmooth.add( SENSOR_ENERGO_COUNT_RD )
            hsSensorNonSmooth.add( SENSOR_ENERGO_COUNT_RR )
        }

        //--- типы датчиков, у которых не бывает калибровки
        val hsSensorNonCalibration = mutableSetOf<Int>()

        init {
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

