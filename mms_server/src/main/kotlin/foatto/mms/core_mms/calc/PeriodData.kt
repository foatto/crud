package foatto.mms.core_mms.calc

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.xy.geom.XyPoint
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.SensorConfigA

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class MultiplePeriodData( val begTime: Int, val endTime: Int, val state1: Int, val state2: Int )

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

abstract class AbstractPeriodData(var begTime: Int, var endTime: Int) {

    abstract fun getState() : Int
    abstract fun setState( aState: Int )
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class GeoPeriodData : AbstractPeriodData {

    var moveState = 0
    var parkingCoord: XyPoint? = null

    var calc: ObjectCalc? = null
    var sbZoneName: StringBuilder? = null

    constructor(aBegTime: Int, aEndTime: Int, aMoveState: Int) : super( aBegTime, aEndTime ) {
        moveState = aMoveState
    }

    constructor(aBegTime: Int, aEndTime: Int, aParkingCoord: XyPoint) : super( aBegTime, aEndTime ) {
        moveState = 0
        parkingCoord = aParkingCoord
    }

    override fun getState(): Int = moveState

    override fun setState( aState: Int ) {
        moveState = aState
    }
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class OverSpeedPeriodData : AbstractPeriodData {
    var overSpeedState = 0

    var maxOverSpeedTime = 0          // время максимального превышения
    var maxOverSpeedCoord: XyPoint? = null  // координаты точки максимального превышения
    var maxOverSpeedMax = 0            // скорость при максимальном превышении
    var maxOverSpeedDiff = 0           // величина максимального превышения

    var objectConfig: ObjectConfig? = null
    var sbZoneName: StringBuilder? = null

    constructor(aBegTime: Int, aEndTime: Int) : super( aBegTime, aEndTime ) {
        overSpeedState = 0
    }

    constructor(aBegTime: Int, aEndTime: Int, aMaxOverSpeedTime: Int, aMaxOverSpeedCoord: XyPoint, aMaxOverSpeedMax: Int, aMaxOverSpeedDiff: Int ) : super( aBegTime, aEndTime ) {
        overSpeedState = 1

        maxOverSpeedTime = aMaxOverSpeedTime
        maxOverSpeedCoord = aMaxOverSpeedCoord
        maxOverSpeedMax = aMaxOverSpeedMax
        maxOverSpeedDiff = aMaxOverSpeedDiff
    }

    override fun getState(): Int = overSpeedState

    override fun setState( aState: Int ) {
        overSpeedState = aState
    }
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class WorkPeriodData(aBegTime: Int, aEndTime: Int, private var workState: Int) : AbstractPeriodData( aBegTime, aEndTime ) {

    override fun getState(): Int = workState

    override fun setState( aState: Int ) {
        workState = aState
    }
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class LiquidStatePeriodData( var begPos: Int, var endPos: Int, var colorIndex: GraphicColorIndex )

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class LiquidLevelPeriodData(aBegTime: Int, aEndTime: Int, private var colorIndex: GraphicColorIndex ) : AbstractPeriodData( aBegTime, aEndTime ) {

    override fun getState(): Int = if( colorIndex == GraphicColorIndex.LINE_NORMAL_0 ) 1 else 0

    override fun setState( aState: Int ) {
        colorIndex = if( aState == 1 ) GraphicColorIndex.LINE_NORMAL_0 else GraphicColorIndex.LINE_NEUTRAL  // ???
    }
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class OverSensorPeriodData(
    aBegTime: Int, aEndTime: Int,
    val maxOverSensorTime: Int, val maxOverSensorCoord: XyPoint, val maxOverSensorMax: Double, val maxOverSensorDiff: Double ) : AbstractPeriodData( aBegTime, aEndTime ) {
    var overSensorState = 1

    var objectConfig: ObjectConfig? = null
    var sca: SensorConfigA? = null
    var sbZoneName: StringBuilder? = null

    override fun getState(): Int = overSensorState

    override fun setState( aState: Int ) {
        overSensorState = aState
    }
}
