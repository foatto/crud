package foatto.mms.core_mms.calc

class WorkCalcData(
    val group: String,
    val alWorkOnOff: List<AbstractPeriodData>,
) {
    var onTime = alWorkOnOff.filter {
        it.getState() != 0
    }.sumOf {
        it.endTime - it.begTime
    }
}
