package foatto.core.model.response.xy.geom

class XyRect(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0) {

    companion object {
        private const val OUT_LEFT = 1
        private const val OUT_TOP = 2
        private const val OUT_RIGHT = 4
        private const val OUT_BOTTOM = 8
    }

    constructor(aX: Float, aY: Float, aWidth: Float, aHeight: Float) : this(aX.toInt(), aY.toInt(), aWidth.toInt(), aHeight.toInt())
    constructor(aX: Double, aY: Double, aWidth: Double, aHeight: Double) : this(aX.toInt(), aY.toInt(), aWidth.toInt(), aHeight.toInt())

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun set(aX: Int, aY: Int, aWidth: Int, aHeight: Int) {
        x = aX
        y = aY
        width = aWidth
        height = aHeight
    }

    fun set(aX: Float, aY: Float, aWidth: Float, aHeight: Float) {
        set(aX.toInt(), aY.toInt(), aWidth.toInt(), aHeight.toInt())
    }

    fun set(aX: Double, aY: Double, aWidth: Double, aHeight: Double) {
        set(aX.toInt(), aY.toInt(), aWidth.toInt(), aHeight.toInt())
    }

    fun isContains(aP: XyPoint): Boolean = isContains(aP.x, aP.y)

    fun isContains(aX: Int, aY: Int): Boolean = aX >= x && aY >= y && aX < x + width && aY < y + height

    fun isIntersects(aLine: XyLine): Boolean = isIntersects(aLine.x1.toDouble(), aLine.y1.toDouble(), aLine.x2.toDouble(), aLine.y2.toDouble())

    fun isIntersects(aX1: Double, aY1: Double, aX2: Double, aY2: Double): Boolean {
        var x1 = aX1
        var y1 = aY1
        val outCode2 = getOutCode(aX2, aY2)
        if (outCode2 == 0) return true

        var outCode1 = getOutCode(x1, y1)
        while (outCode1 != 0) {
            if (outCode1 and outCode2 != 0) return false
            if (outCode1 and (OUT_LEFT or OUT_RIGHT) != 0) {
                var tmpX = x.toDouble()
                if (outCode1 and OUT_RIGHT != 0) tmpX += width.toDouble()
                y1 += (tmpX - x1) * (aY2 - y1) / (aX2 - x1)
                x1 = tmpX
            } else {
                var tmpY = y.toDouble()
                if (outCode1 and OUT_BOTTOM != 0) tmpY += height.toDouble()
                x1 += (tmpY - y1) * (aX2 - x1) / (aY2 - y1)
                y1 = tmpY
            }
            outCode1 = getOutCode(x1, y1)
        }
        return true
    }

    fun isIntersects(aRect: XyRect): Boolean = x < aRect.x + aRect.width && y < aRect.y + aRect.height && x + width > aRect.x && y + height > aRect.y

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun getOutCode(aX: Double, aY: Double): Int {
        var outCode = 0
        if (width <= 0) outCode = outCode or (OUT_LEFT or OUT_RIGHT)
        else if (aX < x) outCode = outCode or OUT_LEFT
        else if (aX > x + width) outCode = outCode or OUT_RIGHT

        if (height <= 0) outCode = outCode or (OUT_TOP or OUT_BOTTOM)
        else if (aY < y) outCode = outCode or OUT_TOP
        else if (aY > y + height) outCode = outCode or OUT_BOTTOM

        return outCode
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun toString(): String = "x = $x, y = $y, width = $width, height = $height"

}
