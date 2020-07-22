package foatto.mms.core_mms.calc

import foatto.core.app.xy.geom.XyPoint

class GeoCalcData {
    var descr: String? = null
    var run = 0.0
    var outTime = 0
    var inTime = 0
    var movingTime = 0
    var parkingCount = 0
    var parkingTime = 0

    var alMovingAndParking: List<AbstractPeriodData>? = null
    var alOverSpeed: List<AbstractPeriodData>? = null
    var alPointTime: List<Int>? = null
    var alPointXY: List<XyPoint>? = null
    var alPointSpeed: List<Int>? = null
    var alPointOverSpeed: List<Int>? = null

    //--- для SumCollector
    constructor()

    constructor(aDescr: String, aRun: Double, aOutTime: Int, aInTime: Int, aMovingTime: Int, aParkingCount: Int, aParkingTime: Int,
                aAlMovingAndParking: List<AbstractPeriodData>, aAlOverSpeed: List<AbstractPeriodData>,
                aAlPointTime: List<Int>, aAlPointXY: List<XyPoint>, aAlPointSpeed: List<Int>, aAlPointOverSpeed: List<Int> ) {
        descr = aDescr
        run = aRun
        outTime = aOutTime
        inTime = aInTime
        movingTime = aMovingTime
        parkingCount = aParkingCount
        parkingTime = aParkingTime
        alMovingAndParking = aAlMovingAndParking
        alOverSpeed = aAlOverSpeed
        alPointTime = aAlPointTime
        alPointXY = aAlPointXY
        alPointSpeed = aAlPointSpeed
        alPointOverSpeed = aAlPointOverSpeed
    }
}
