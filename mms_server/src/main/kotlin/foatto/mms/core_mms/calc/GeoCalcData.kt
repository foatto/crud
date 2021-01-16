package foatto.mms.core_mms.calc

import foatto.core.app.xy.geom.XyPoint

class GeoCalcData(
    val group: String,
    val descr: String,

    var run: Double,
    val outTime: Int,
    val inTime: Int,
    val movingTime: Int,
    val parkingCount: Int,
    val parkingTime: Int,

    val alMovingAndParking: List<AbstractPeriodData>,
    val alOverSpeed: List<AbstractPeriodData>,
    val alPointTime: List<Int>,
    val alPointXY: List<XyPoint>,
    val alPointSpeed: List<Int>,
    val alPointOverSpeed: List<Int>,
)
