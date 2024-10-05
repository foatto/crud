package foatto.server.model

class SensorConfigGeo {
    companion object {
        //--- правила округления дробной скорости - в меньшую сторону, стандартно (по арифметическим/банковским правилам), в большую сторону
        val SPEED_ROUND_RULE_LESS = -1
        val SPEED_ROUND_RULE_STANDART = 0
        val SPEED_ROUND_RULE_GREATER = 1
    }
}