package foatto.mms.core_mms.calc

class WorkCalcData {

    var onTime = 0
    var offTime = 0
    var alWorkOnOff: List<AbstractPeriodData>? = null

    //--- для SumCollector
    constructor()

    constructor(aAlWorkOnOff: List<AbstractPeriodData>?) {
        alWorkOnOff = aAlWorkOnOff

        if (alWorkOnOff != null)
            for (apd in alWorkOnOff!!) {
                if (apd.getState() != 0) onTime += apd.endTime - apd.begTime
                else offTime += apd.endTime - apd.begTime
            }
    }
}
