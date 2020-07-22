package foatto.mms.core_mms

import foatto.core.app.xy.geom.XyPoint

class GeoData( aWgsX: Int, aWgsY: Int, val speed: Int, val distance: Int ) {
    var wgs = XyPoint(0, 0)

    init {
        wgs.set( aWgsX, aWgsY )
    }
}
