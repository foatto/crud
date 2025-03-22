package foatto.server.calc

class WorkCalcData(
    val group: String,
    val states: List<WorkPeriodData>,
    val onTime: Int,
    val liquidName: String?,
    val liquidCalc: Double?,
)