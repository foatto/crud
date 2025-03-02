package foatto.core.model.model.xy

import foatto.core.model.response.xy.geom.XyPoint
import kotlin.math.*

object XyProjection {

//    const val WGS_KOEF_i: Int = 10_000_000
//    const val WGS_KOEF_d: Double = 10_000_000.0

    private const val MAX_MAP_SIZE = 1024 * 1024 * 1024
    private const val CENTER_COORD = MAX_MAP_SIZE / 2
    private const val PIXEL_PER_LON_DEGREE = MAX_MAP_SIZE / 360.0
    private const val PIXEL_PER_LON_RADIAN = MAX_MAP_SIZE.toDouble() / 2.0 / PI

//    fun wgs_pix(wgs: XyPoint): XyPoint = wgs_pix(wgs.x, wgs.y, XyPoint(0, 0))

//    fun wgs_pix(wgs: XyPoint, pix: XyPoint): XyPoint = wgs_pix(wgs.x, wgs.y, pix)

//    fun wgs_pix(wgsX: Int, wgsY: Int, pix: XyPoint = XyPoint(0, 0)): XyPoint {
//        val sinLat = sin((wgsY / WGS_KOEF_d) * PI / 180)
//
//        pix.x = round(CENTER_COORD + wgsX / WGS_KOEF_d * PIXEL_PER_LON_DEGREE).toInt()
//        pix.y = round(CENTER_COORD - ln((1 + sinLat) / (1 - sinLat)) * PIXEL_PER_LON_RADIAN / 2).toInt()
//
//        return pix
//    }

    fun wgs_pix(wgsX: Double, wgsY: Double, pix: XyPoint = XyPoint(0, 0)): XyPoint {
        val sinLat = sin(wgsY * PI / 180)

        pix.x = round(CENTER_COORD + wgsX * PIXEL_PER_LON_DEGREE).toInt()
        pix.y = round(CENTER_COORD - ln((1 + sinLat) / (1 - sinLat)) * PIXEL_PER_LON_RADIAN / 2).toInt()

        return pix
    }

    //    public static String[] wgs_stringClassic( XyPoint p ) {
    //        int xDegree = (int) p.getX();
    //        int xMinute = (int) ( ( p.getX() - xDegree ) * 60.0 );
    //        int xSecond = (int) ( ( p.getX() - xDegree - xMinute / 60.0 ) * 3600.0 );
    //        int xSecondFloat = (int) ( ( p.getX() - xDegree - xMinute / 60.0 - xSecond / 3600.0 ) * 360000.0 );
    //
    //        int yDegree = (int) p.getY();
    //        int yMinute = (int) ( ( p.getY() - yDegree ) * 60.0 );
    //        int ySecond = (int) ( ( p.getY() - yDegree - yMinute / 60.0 ) * 3600.0 );
    //        int ySecondFloat = (int) ( ( p.getY() - yDegree - yMinute / 60.0 - ySecond / 3600.0 ) * 360000.0 );
    //
    //        StringBuilder sbX = new StringBuilder( p.getX() >=0 ? "E" : "W" ).append( ' ' )
    //                            .append( xDegree < 10 ? "0" : "" ).append( xDegree ).append( '^' )
    //                            .append( xMinute < 10 ? "0" : "" ).append( xMinute ).append( '\'' )
    //                            .append( xSecond < 10 ? "0" : "" ).append( xSecond ).append( '.' )
    //                            .append( xSecondFloat < 10 ? "0" : "" ).append( xSecondFloat ).append( '"' );
    //        StringBuilder sbY = new StringBuilder( p.getY() >=0 ? "N" : "S" ).append( ' ' )
    //                            .append( yDegree < 10 ? "0" : "" ).append( yDegree ).append( '^' )
    //                            .append( yMinute < 10 ? "0" : "" ).append( yMinute ).append( '\'' )
    //                            .append( ySecond < 10 ? "0" : "" ).append( ySecond ).append( '.' )
    //                            .append( ySecondFloat < 10 ? "0" : "" ).append( ySecondFloat ).append( '"' );
    //
    //        return new String[] { sbX.toString(), sbY.toString() };
    //    }

    //    public static String[] wgs_stringNumber( Point2D p, int precision ) {
    //        StringBuilder sbX = new StringBuilder( p.getX() >=0 ? "E" : "W" ).append( ' ' )
    //                            .append( StringFunction.getSplittedDouble( abs( p.getX() ), precision ) );
    //        StringBuilder sbY = new StringBuilder( p.getY() >=0 ? "N" : "S" ).append( ' ' )
    //                            .append( StringFunction.getSplittedDouble( abs( p.getY() ), precision ) );
    //        return new String[] { sbX.toString(), sbY.toString() };
    //    }

    //--- there is no usual inverse transformation function, we make an iterative approximation
//    fun pix_wgs(pix: XyPoint, delta: Int): XyPoint = pix_wgs(pix, XyPoint(0, 0), delta)

//    fun pix_wgs(pix: XyPoint, wgs: XyPoint, delta: Int): XyPoint {
//        val wgsMin = XyPoint(-180 * WGS_KOEF_i, -85 * WGS_KOEF_i)
//        val wgsMax = XyPoint(180 * WGS_KOEF_i, 85 * WGS_KOEF_i)
//        val pixCalc = XyPoint(0, 0)
//
//        while (true) {
//            //--- 1. the classic version is fraught with overflow when adding two extreme numbers
//            //wgs.x = ( wgsMin.x + wgsMax.x ) / 2;
//            //wgs.y = ( wgsMin.y + wgsMax.y ) / 2;
//            //--- 2.the corrected version gives an overflow when subtracting an extreme negative number
//            //--- from the extreme positive, for example, with 18 * 10 ^ 8 - (-18 * 10 ^ 8) gives addition with overflow to negative numbers
//            //wgs.x = wgsMin.x + ( wgsMax.x - wgsMin.x ) / 2;
//            //wgs.y = wgsMin.y + ( wgsMax.y - wgsMin.y ) / 2;
//            //--- 3.we use a modification of option 1 with an error / shift of integer division
//            //--- for 1 unit downward, but in our case it is not critical
//            wgs.x = wgsMin.x / 2 + wgsMax.x / 2
//            wgs.y = wgsMin.y / 2 + wgsMax.y / 2
//            wgs_pix(wgs, pixCalc)
//
//            //--- coordinates coincided (with some tolerance), we selected the wgs-coordinates corresponding to the component
//            val isFoundX = abs(pix.x - pixCalc.x) < delta
//            val isFoundY = abs(pix.y - pixCalc.y) < delta
//            //--- both coordinate components coincided - exit
//            if (isFoundX && isFoundY) break
//
//            //--- search correction
//            if (!isFoundX) {
//                if (pix.x < pixCalc.x) wgsMax.x = wgs.x
//                else wgsMin.x = wgs.x
//            }
//
//            //--- correction of search along Y "in the opposite direction", since Y-axes are directed in opposite directions
//            if (!isFoundY) {
//                if (pix.y < pixCalc.y) wgsMin.y = wgs.y
//                else wgsMax.y = wgs.y
//            }
//        }
//        return wgs
//    }

//    fun distancePrj(pix1: XyPoint, pix2: XyPoint, delta: Int): Double {
//        val wgs1 = pix_wgs(pix1, delta)
//        val wgs2 = pix_wgs(pix2, delta)
//        return distance(wgs1.x / WGS_KOEF_d, wgs1.y / WGS_KOEF_d, wgs2.x / WGS_KOEF_d, wgs2.y / WGS_KOEF_d)
//    }

//    fun distanceWGS(wgs1: XyPoint, wgs2: XyPoint): Double = distance(wgs1.x / WGS_KOEF_d, wgs1.y / WGS_KOEF_d, wgs2.x / WGS_KOEF_d, wgs2.y / WGS_KOEF_d)

    fun distance(lon1: Double, lat1: Double, lon2: Double, lat2: Double): Double {
        val D2R = PI / 180
        val a = 6378137.0             // Main axle shafts
        //final double b = 6356752.314245;        // Minor axle shafts - not used
        val e2 = 0.006739496742337    // Ellipsoid eccentricity squared
        //final double f = 0.003352810664747;     // Ellipsoid Alignment - Not Used

        //--- Calculate the difference between two longitudes and latitudes and get the average latitude
        val fdLambda = (lon1 - lon2) * D2R
        val fdPhi = (lat1 - lat2) * D2R
        val fPhimean = (lat1 + lat2) / 2 * D2R

        //--- We calculate the meridian and transverse radii of curvature of the middle latitude
        val fTemp = 1 - e2 * sin(fPhimean).pow(2.0)
        val fRho = a * (1 - e2) / fTemp.pow(1.5)
        //val fNu = a / sqrt( 1 - e2 * sin( fPhimean ).pow( 2.0 ) ) // can fTemp be used?
        val fNu = a / sqrt(fTemp)

        //--- Calculate the angular distance
        var fz = sqrt(sin(fdPhi / 2).pow(2.0) + cos(lat2 * D2R) * cos(lat1 * D2R) * sin(fdLambda / 2).pow(2.0))
        fz = 2 * asin(fz)

        //--- Calculate the offset
        var fAlpha = cos(lat2 * D2R) * sin(fdLambda) / sin(fz)
        fAlpha = asin(fAlpha)

        //--- Calculate the radius of the Earth
        val fR = fRho * fNu / (fRho * sin(fAlpha).pow(2.0) + fNu * cos(fAlpha).pow(2.0))

        return fz * fR
    }
}
