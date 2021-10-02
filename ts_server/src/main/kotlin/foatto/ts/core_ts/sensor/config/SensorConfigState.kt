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
        val COLOR_UNKNOWN_FORE = 0x40_00_FF_FF.toInt()
        val COLOR_UNKNOWN_BACK = 0x40_00_80_80.toInt()

        val COLOR_RED_FORE = 0x40_FF_00_00.toInt()
        val COLOR_RED_BACK = 0x40_FF_80_80.toInt()

        val COLOR_GREEN_FORE = 0x40_00_FF_00.toInt()
        val COLOR_GREEN_BACK = 0x40_80_FF_80.toInt()

        val COLOR_BLUE_FORE = 0x40_00_00_FF.toInt()
        val COLOR_BLUE_BACK = 0x40_80_80_FF.toInt()

        val COLOR_GRAY_FORE = 0x40_60_60_60.toInt()
        val COLOR_GRAY_BACK = 0x40_A0_A0_A0.toInt()

        val COLOR_BROWN_FORE = 0x40_60_60_00.toInt()
        val COLOR_BROWN_BACK = 0x40_A0_A0_00.toInt()

        val COLOR_PURPLE_FORE = 0x40_60_00_60.toInt()
        val COLOR_PURPLE_BACK = 0x40_A0_00_A0.toInt()

        val hmStateInfo = mapOf(
            0 to StateInfo("Слепой подъём", COLOR_PURPLE_FORE, COLOR_PURPLE_BACK),
            1 to StateInfo("Непроход вверх: спуск на 1 метр", COLOR_BROWN_FORE, COLOR_BROWN_BACK),
            2 to StateInfo("Спуск", COLOR_GREEN_FORE, COLOR_GREEN_BACK),
            3 to StateInfo("Непроход вниз: пауза", COLOR_BROWN_FORE, COLOR_BROWN_BACK),
            4 to StateInfo("Непроход вниз: подъём на 1 метр", COLOR_BROWN_FORE, COLOR_BROWN_BACK),
            5 to StateInfo("Подъём", COLOR_GREEN_FORE, COLOR_GREEN_BACK),
            6 to StateInfo("Непроход вверх: спуск на 1 метр", COLOR_BROWN_FORE, COLOR_BROWN_BACK),
            7 to StateInfo("Парковка", COLOR_GREEN_FORE, COLOR_GREEN_BACK),
            8 to StateInfo("Непроход вниз: пауза", COLOR_BROWN_FORE, COLOR_BROWN_BACK),
            9 to StateInfo("Непроход вниз: подъём на 1 метр", COLOR_BROWN_FORE, COLOR_BROWN_BACK),
            10 to StateInfo("Ожидание чистки", COLOR_BLUE_FORE, COLOR_BLUE_BACK),
            11 to StateInfo("Ручной режим: остановка", COLOR_GRAY_FORE, COLOR_GRAY_BACK),
            12 to StateInfo("Ручной режим: подъём", COLOR_GRAY_FORE, COLOR_GRAY_BACK),
            13 to StateInfo("Ручной режим: спуск", COLOR_GRAY_FORE, COLOR_GRAY_BACK),
            14 to StateInfo("Ожидание включения ЭЦН", COLOR_BLUE_FORE, COLOR_BLUE_BACK),
            15 to StateInfo("Нейтраль", COLOR_BLUE_FORE, COLOR_BLUE_BACK),
            20 to StateInfo("Непроход вниз", COLOR_RED_FORE, COLOR_RED_BACK),
            21 to StateInfo("Непроход вверх", COLOR_RED_FORE, COLOR_RED_BACK),
            22 to StateInfo("Обрыв проволоки", COLOR_RED_FORE, COLOR_RED_BACK),
            23 to StateInfo("Ошибка привода", COLOR_RED_FORE, COLOR_RED_BACK),
        )
    }
}

class StateInfo(
    val descr: String,
    val foreColor: Int,
    val backColor: Int,
)
