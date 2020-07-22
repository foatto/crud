package foatto.mms.core_mms.calc

class LiquidUsingCalcData {

    var usingTotal = 0.0
    var usingMoving = 0.0
    var usingParking = 0.0

    //--- расчётный расход во время заправки/слива - хранится отдельно для справочного вывода и
    //--- уже входит в состав usingTotal и usingParking
    var usingCalc = 0.0

    //--- принципиально: общий расход жидкости задаём явным образом -
    //--- так быстрее всплывут неточности расчёта, если они есть
    fun add( aTotal: Double, aMoving: Double, aParking: Double, aCalc: Double ) {
        usingTotal += aTotal
        usingMoving += aMoving
        usingParking += aParking
        usingCalc += aCalc
    }
}
