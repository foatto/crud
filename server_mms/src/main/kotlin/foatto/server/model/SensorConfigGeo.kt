package foatto.server.model

class SensorConfigGeo {
    companion object {
        const val PORT_NUM: Int = 19

        //--- правила округления дробной скорости - в меньшую сторону, стандартно (по арифметическим/банковским правилам), в большую сторону
        const val SPEED_ROUND_RULE_LESS: Int = -1
        const val SPEED_ROUND_RULE_STANDART: Int = 0
        const val SPEED_ROUND_RULE_GREATER: Int = 1
    }
}