package foatto.server.model

class SensorConfigCounter {
    companion object {
        const val CALC_TYPE_IN = 0    // входящий счётчик
        const val CALC_TYPE_OUT = 1   // исходящий счётчик

        const val STATUS_UNKNOWN = 0
        const val STATUS_IDLE = 1
        const val STATUS_NORMAL = 2
        const val STATUS_OVERLOAD = 4
        const val STATUS_CHEAT = 5
        const val STATUS_REVERSE = 6
        const val STATUS_INTERVENTION = 7

//!!! временно отключим - больше мешают, чем помогают
//        val hmStatusDescr = mapOf(
//            STATUS_UNKNOWN to "(неизвестно)",
//            STATUS_IDLE to "Холостой ход",
//            STATUS_NORMAL to "Рабочий ход",
//            STATUS_OVERLOAD to "Перегрузка",
//            STATUS_CHEAT to "Накрутка",
//            STATUS_REVERSE to "Обратный ход",
//            STATUS_INTERVENTION to "Вмешательство",
//        )
    }
}