package foatto.ts.core_ts.sensor.config

class SensorConfigState(
    aId: Int,
    aName: String,
    aGroup: String,
    aDescr: String,
    aPortNum: Int,
    aSensorType: Int,
) : SensorConfig(aId, aName, aGroup, aDescr, aPortNum, aSensorType) {

    companion object {
        val STATE_UNPASS_UP_1_METER_DOWN = 1
        val STATE_DOWN = 2
        val STATE_UNPASS_DOWN_PAUSE = 3
        val STATE_UNPASS_DOWN_1_METER_UP = 4
        val STATE_UP = 5
        val STATE_UNPASS_UP_1_METER_DOWN_ = 6
        val STATE_PARKING = 7
        val STATE_UNPASS_DOWN_PAUSE_ = 8
        val STATE_UNPASS_DOWN_1_METER_UP_ = 9
        val STATE_WAIT_CLEAN = 10
        val STATE_WAIT_ECN = 14
        val STATE_UNPASS_DOWN = 20
        val STATE_UNPASS_UP = 21
        val STATE_WIRE_RUNOUT = 22
        val STATE_DRIVE_PROTECT = 23
        val STATE_STOPPED_BY_SERVER = 24
        val STATE_BLOCKED_BY_SERVER = 25

        val COLOR_UNKNOWN_BRIGHT = 0xFF_FF_00_00.toInt()
        val COLOR_UNKNOWN_DARK = 0xFF_80_00_00.toInt()

        val COLOR_RED_BRIGHT = 0xFF_E8_A8_A8.toInt()
        val COLOR_RED_DARK = 0x40_D0_50_50.toInt()

        val COLOR_GREEN_BRIGHT = 0xFF_C0_DD_C0.toInt()
        val COLOR_GREEN_DARK = 0x40_88_BB_88.toInt()

        val COLOR_BLUE_BRIGHT = 0xFF_B0_CA_FF.toInt()
        val COLOR_BLUE_DARK = 0x40_64_96_ED.toInt()

        val COLOR_GRAY_BRIGHT = 0xFF_B0_C0_C0.toInt()
        val COLOR_GRAY_DARK = 0x40_60_70_70.toInt()

        val COLOR_ORANGE_BRIGHT = 0xFF_FF_CC_99.toInt()
        val COLOR_ORANGE_DARK = 0x40_FF_99_33.toInt()

        val COLOR_PURPLE_BRIGHT = 0xFF_B0_A0_C0.toInt()
        val COLOR_PURPLE_DARK = 0x40_60_48_7A.toInt()

        val hmStateInfo = mapOf(
            0 to StateInfo("Слепой подъём", COLOR_PURPLE_BRIGHT, COLOR_PURPLE_DARK),
            STATE_UNPASS_UP_1_METER_DOWN to StateInfo("Непроход вверх: спуск на 1 метр", COLOR_ORANGE_BRIGHT, COLOR_ORANGE_DARK),
            STATE_DOWN to StateInfo("Спуск", COLOR_GREEN_BRIGHT, COLOR_GREEN_DARK),
            STATE_UNPASS_DOWN_PAUSE to StateInfo("Непроход вниз: пауза", COLOR_ORANGE_BRIGHT, COLOR_ORANGE_DARK),
            STATE_UNPASS_DOWN_1_METER_UP to StateInfo("Непроход вниз: подъём на 1 метр", COLOR_ORANGE_BRIGHT, COLOR_ORANGE_DARK),
            STATE_UP to StateInfo("Подъём", COLOR_GREEN_BRIGHT, COLOR_GREEN_DARK),
            STATE_UNPASS_UP_1_METER_DOWN_ to StateInfo("Непроход вверх: спуск на 1 метр", COLOR_ORANGE_BRIGHT, COLOR_ORANGE_DARK),
            STATE_PARKING to StateInfo("Парковка", COLOR_GREEN_BRIGHT, COLOR_GREEN_DARK),
            STATE_UNPASS_DOWN_PAUSE_ to StateInfo("Непроход вниз: пауза", COLOR_ORANGE_BRIGHT, COLOR_ORANGE_DARK),
            STATE_UNPASS_DOWN_1_METER_UP_ to StateInfo("Непроход вниз: подъём на 1 метр", COLOR_ORANGE_BRIGHT, COLOR_ORANGE_DARK),
            STATE_WAIT_CLEAN to StateInfo("Ожидание чистки", COLOR_BLUE_BRIGHT, COLOR_BLUE_DARK),
            11 to StateInfo("Ручной режим: остановка", COLOR_GRAY_BRIGHT, COLOR_GRAY_DARK),
            12 to StateInfo("Ручной режим: подъём", COLOR_GRAY_BRIGHT, COLOR_GRAY_DARK),
            13 to StateInfo("Ручной режим: спуск", COLOR_GRAY_BRIGHT, COLOR_GRAY_DARK),
            STATE_WAIT_ECN to StateInfo("Ожидание включения ЭЦН", COLOR_BLUE_BRIGHT, COLOR_BLUE_DARK),
            15 to StateInfo("Нейтраль", COLOR_BLUE_BRIGHT, COLOR_BLUE_DARK),
            STATE_UNPASS_DOWN to StateInfo("Непроход вниз", COLOR_RED_BRIGHT, COLOR_RED_DARK),
            STATE_UNPASS_UP to StateInfo("Непроход вверх", COLOR_RED_BRIGHT, COLOR_RED_DARK),
            STATE_WIRE_RUNOUT to StateInfo("Выбег проволоки", COLOR_RED_BRIGHT, COLOR_RED_DARK),
            STATE_DRIVE_PROTECT to StateInfo("Защита привода", COLOR_RED_BRIGHT, COLOR_RED_DARK),
            STATE_STOPPED_BY_SERVER to StateInfo("Остановка с сервера", COLOR_RED_BRIGHT, COLOR_RED_DARK),
            STATE_BLOCKED_BY_SERVER to StateInfo("Блокировка с сервера", COLOR_RED_BRIGHT, COLOR_RED_DARK),
        )

        val alStateLegend = listOf(
            COLOR_GREEN_BRIGHT to "Работа",
            COLOR_PURPLE_BRIGHT to "Слепой подъём",
            COLOR_BLUE_BRIGHT to "Ожидание",
            COLOR_GRAY_BRIGHT to "Ручной режим",
            COLOR_ORANGE_BRIGHT to "Непроходы",
            COLOR_RED_BRIGHT to "Ошибка",
        )
    }
}

class StateInfo(
    val descr: String,
    val brightColor: Int,
    val darkColor: Int,
)
