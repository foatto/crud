package foatto.core.model.model.xy

import foatto.core.model.response.xy.geom.XyPoint

class GeoData(aWgsX: Int, aWgsY: Int, val speed: Int, val distance: Int) {
    var wgs = XyPoint(0, 0)

    init {
        wgs.set(aWgsX, aWgsY)
    }
}
