package foatto.mms.core_mms.calc

class WorkCalcData(
    val group: String,
    val alWorkOnOff: List<AbstractPeriodData>,
) {
    var onTime: Int = alWorkOnOff.filter { apd ->
        apd.getState() != 0
    }.sumOf { apd ->
        apd.endTime - apd.begTime
    }
}
