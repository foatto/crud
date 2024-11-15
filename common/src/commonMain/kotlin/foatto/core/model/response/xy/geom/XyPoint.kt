package foatto.core.model.response.xy.geom

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
class XyPoint(var x: Int, var y: Int) {

    constructor(aX: Float, aY: Float) : this(aX.toInt(), aY.toInt())

    constructor(aX: Double, aY: Double) : this(aX.toInt(), aY.toInt())

    fun set(aP: XyPoint) {
        set(aP.x, aP.y)
    }

    fun set(aX: Float, aY: Float) {
        set(aX.toInt(), aY.toInt())
    }

    fun set(aX: Int, aY: Int) {
        x = aX
        y = aY
    }

    fun distance(p: XyPoint): Double {
        return distance(x.toDouble(), y.toDouble(), p.x.toDouble(), p.y.toDouble())
    }

    companion object {
        fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            return sqrt(dx * dx + dy * dy)
        }
    }
}
