package foatto.mms.core_mms.calc

class WorkCalcData {

    var onTime = 0
    var offTime = 0
    var alWorkOnOff: List<AbstractPeriodData>? = null

    var onMovingTime = 0       // работа оборудования в движении
    var offMovingTime = 0      // простой оборудования в движении
    var onParkingTime = 0      // работа оборудования на стоянке
    var offParkingTime = 0     // простой оборудования на стоянке

    //--- для SumCollector
    constructor()

    constructor( aAlWorkOnOff: List<AbstractPeriodData>?, alMultiplePeriods: List<MultiplePeriodData>? ) {
        alWorkOnOff = aAlWorkOnOff

        if( alWorkOnOff != null )
            for( apd in alWorkOnOff!! ) {
                if( apd.getState() != 0 ) onTime += apd.endTime - apd.begTime
                else offTime += apd.endTime - apd.begTime
            }

        //--- нет необходимости хранить экземпляр
        if( !alMultiplePeriods.isNullOrEmpty() )
            for( mpd in alMultiplePeriods ) {
                val timeDiff = mpd.endTime - mpd.begTime
                //--- в движении
                if( mpd.state1 != 0 ) {
                    if( mpd.state2 != 0 ) onMovingTime += timeDiff
                    else offMovingTime += timeDiff
                }
                //--- на стоянке
                else {
                    if( mpd.state2 != 0 ) onParkingTime += timeDiff
                    else offParkingTime += timeDiff
                }
            }
    }
}
