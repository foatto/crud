package foatto.core.model.model.xy

import foatto.core.model.response.xy.geom.XyRect
import kotlinx.serialization.Serializable

@Serializable
class XyViewCoord(var scale: Int, var x1: Int, var y1: Int, var x2: Int, var y2: Int) {

    constructor() : this(1, 0, 0, 1, 1)
    constructor(view: XyViewCoord) : this(view.scale, view.x1, view.y1, view.x2, view.y2)
    constructor(aScale: Int, aRect: XyRect) : this(aScale, aRect.x, aRect.y, aRect.x + aRect.width, aRect.y + aRect.height)

    //----------------------------------------------------------------------------------------------------------------------

    fun isEquals(view: XyViewCoord) = scale == view.scale && x1 == view.x1 && y1 == view.y1 && x2 == view.x2 && y2 == view.y2

    fun moveRel(dx: Int, dy: Int) {
        x1 += dx
        y1 += dy
        x2 += dx
        y2 += dy
    }
}
