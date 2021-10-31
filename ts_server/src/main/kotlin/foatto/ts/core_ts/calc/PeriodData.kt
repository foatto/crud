package foatto.ts.core_ts.calc

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

abstract class AbstractPeriodData(var begTime: Int, var endTime: Int) {

    abstract fun getState(): Int
    abstract fun setState(aState: Int)
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class StatePeriodData(aBegTime: Int, aEndTime: Int, private var workState: Int) : AbstractPeriodData(aBegTime, aEndTime) {

    override fun getState(): Int = workState

    override fun setState(aState: Int) {
        workState = aState
    }
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

