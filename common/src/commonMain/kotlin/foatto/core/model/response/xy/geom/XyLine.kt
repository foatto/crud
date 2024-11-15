package foatto.core.model.response.xy.geom

import kotlin.math.sqrt

class XyLine(var x1: Int = 0, var y1: Int = 0, var x2: Int = 0, var y2: Int = 0) {

    constructor(p1: XyPoint, p2: XyPoint) : this(p1.x, p1.y, p2.x, p2.y)

    fun set(p1: XyPoint, p2: XyPoint) {
        set(p1.x, p1.y, p2.x, p2.y)
    }

    fun set(aX1: Int, aY1: Int, aX2: Int, aY2: Int) {
        x1 = aX1
        y1 = aY1
        x2 = aX2
        y2 = aY2
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun distanceSeg(aP: XyPoint): Double = distanceSeg(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), aP.x.toDouble(), aP.y.toDouble())
    fun distanceSeg(aPX: Int, aPY: Int): Double = distanceSeg(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), aPX.toDouble(), aPY.toDouble())
    fun distanceLine(aP: XyPoint): Double = distanceLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), aP.x.toDouble(), aP.y.toDouble())
    fun distanceLine(aPX: Int, aPY: Int): Double = distanceLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), aPX.toDouble(), aPY.toDouble())
    fun isIntersects(aLine: XyLine): Boolean =
        isIntersects(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), aLine.x1.toDouble(), aLine.y1.toDouble(), aLine.x2.toDouble(), aLine.y2.toDouble())

    fun isIntersects(aX1: Int, aY1: Int, aX2: Int, aY2: Int): Boolean =
        isIntersects(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), aX1.toDouble(), aY1.toDouble(), aX2.toDouble(), aY2.toDouble())

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        fun distanceSeg(aX1: Double, aY1: Double, aX2: Double, aY2: Double, aPX: Double, aPY: Double): Double {
            var x2 = aX2
            var y2 = aY2
            var px = aPX
            var py = aPY
            x2 -= aX1
            y2 -= aY1
            px -= aX1
            py -= aY1
            var dotprod = px * x2 + py * y2
            val projlenSq: Double
            if (dotprod <= 0.0) projlenSq = 0.0
            else {
                px = x2 - px
                py = y2 - py
                dotprod = px * x2 + py * y2

                projlenSq = if (dotprod <= 0.0) 0.0 else dotprod * dotprod / (x2 * x2 + y2 * y2)
            }
            var lenSq = px * px + py * py - projlenSq
            if (lenSq < 0) lenSq = 0.0

            return sqrt(lenSq)
        }

        fun distanceLine(aX1: Double, aY1: Double, aX2: Double, aY2: Double, aPX: Double, aPY: Double): Double {
            var x2 = aX2
            var y2 = aY2
            var px = aPX
            var py = aPY
            x2 -= aX1
            y2 -= aY1
            px -= aX1
            py -= aY1
            val dotprod = px * x2 + py * y2
            val projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2)
            var lenSq = px * px + py * py - projlenSq
            if (lenSq < 0) lenSq = 0.0

            return sqrt(lenSq)
        }

        fun isIntersects(aX1: Double, aY1: Double, aX2: Double, aY2: Double, aX3: Double, aY3: Double, aX4: Double, aY4: Double): Boolean =
            relativeCCW(aX1, aY1, aX2, aY2, aX3, aY3) * relativeCCW(aX1, aY1, aX2, aY2, aX4, aY4) <= 0 &&
                relativeCCW(aX3, aY3, aX4, aY4, aX1, aY1) * relativeCCW(aX3, aY3, aX4, aY4, aX2, aY2) <= 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        private fun relativeCCW(aX1: Double, aY1: Double, aX2: Double, aY2: Double, aPX: Double, aPY: Double): Int {
            var x2 = aX2
            var y2 = aY2
            var px = aPX
            var py = aPY
            x2 -= aX1
            y2 -= aY1
            px -= aX1
            py -= aY1
            var ccw = px * y2 - py * x2
            if (ccw == 0.0) {
                ccw = px * x2 + py * y2
                if (ccw > 0.0) {
                    px -= x2
                    py -= y2
                    ccw = px * x2 + py * y2
                    if (ccw < 0.0) ccw = 0.0
                }
            }
            return if (ccw < 0.0) -1 else if (ccw > 0.0) 1 else 0
        }
    }
}
