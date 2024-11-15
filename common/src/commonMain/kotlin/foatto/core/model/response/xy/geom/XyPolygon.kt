package foatto.core.model.response.xy.geom

class XyPolygon(val alPoint: MutableList<XyPoint> = mutableListOf()) {

    fun isContains(aP: XyPoint): Boolean {
        return isContains(aP.x, aP.y)
    }

    fun isContains(aX: Int, aY: Int): Boolean {
        var crossCount = 0

        for (i in alPoint.indices) {
            val p1 = alPoint[i]
            val p2 = alPoint[if (i + 1 == alPoint.size) 0 else i + 1]

            crossCount += checkPointCrossing(aX.toDouble(), aY.toDouble(), p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble())
        }
        return crossCount != 0
    }

    private fun checkPointCrossing(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Int {
        //--- special cases:

        //--- the ray passes above the segment
        if (py < y1 && py < y2) return 0
        //--- the ray passes below the line segment or coincides with the horizontal segment
        if (py >= y1 && py >= y2) return 0
        //--- the ray started to the right of the segment
        if (px >= x1 && px >= x2) return 0
        //--- the ray started to the left of the segment
        if (px < x1 && px < x2) return if (y1 < y2) 1 else -1

        //--- common case:

        //--- calculate the x-coordinate of the _possible_ intersection of the ray with the segment
        val x12 = x1 + 1.0 * (py - y1) * (x2 - x1) / (y2 - y1)
        //--- is the origin to the right or left of the intersection?
        return if (px >= x12) 0 else if (y1 < y2) 1 else -1
    }
}
