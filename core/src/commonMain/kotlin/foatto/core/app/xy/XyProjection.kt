package foatto.core.app.xy

import foatto.core.app.xy.geom.XyPoint
import kotlin.math.*

object XyProjection {

    const val WGS_KOEF_i = 10000000
    const val WGS_KOEF_d = 10_000_000.0

//    private val DEGREES_TO_RADIANS = 0.017453292519943295

    private val MAX_MAP_SIZE = 1024 * 1024 * 1024      // 2^30 или чуть больше 1 млрд
    private val CENTER_COORD = MAX_MAP_SIZE / 2
    private val PIXEL_PER_LON_DEGREE = MAX_MAP_SIZE / 360.0
    private val PIXEL_PER_LON_RADIAN = MAX_MAP_SIZE.toDouble() / 2.0 / PI

    fun wgs_pix( wgs: XyPoint): XyPoint = wgs_pix(wgs.x, wgs.y, XyPoint(0, 0))

    fun wgs_pix(wgs: XyPoint, pix: XyPoint): XyPoint = wgs_pix(wgs.x, wgs.y, pix)

    fun wgs_pix( wgsX: Int, wgsY: Int, pix: XyPoint = XyPoint(0, 0)): XyPoint {
        val sinLat = sin( ( wgsY / WGS_KOEF_d) * PI / 180 )
//        val sinLat = sin( DEGREES_TO_RADIANS * wgsY / WGS_KOEF_d )

        pix.x = round( CENTER_COORD + wgsX / WGS_KOEF_d * PIXEL_PER_LON_DEGREE).toInt()
        pix.y = round( CENTER_COORD - ln( ( 1 + sinLat) / ( 1 - sinLat ) ) * PIXEL_PER_LON_RADIAN / 2 ).toInt()
//        pix.y = round( CENTER_COORD - log( ( 1 + sinLat) / ( 1 - sinLat ), E ) * PIXEL_PER_LON_RADIAN / 2 ).toInt()

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

    //--- обычной функции обратного преобразования нет, делаем итерационное приближение
    fun pix_wgs(pix: XyPoint, delta: Int ): XyPoint = pix_wgs(pix, XyPoint(0, 0), delta)

    fun pix_wgs(pix: XyPoint, wgs: XyPoint, delta: Int ): XyPoint {
        val wgsMin = XyPoint(-180 * WGS_KOEF_i, -85 * WGS_KOEF_i)
        val wgsMax = XyPoint(180 * WGS_KOEF_i, 85 * WGS_KOEF_i)
        val pixCalc = XyPoint(0, 0)

        while( true ) {
            //--- 1. классический вариант чреват переполнением при сложении двух крайних чисел
            //wgs.x = ( wgsMin.x + wgsMax.x ) / 2;
            //wgs.y = ( wgsMin.y + wgsMax.y ) / 2;
            //--- 2. исправленный вариант даёт переполнение при вычитании крайнего отрицательного числа
            //--- из крайнего положительного, например при 18*10^8 - ( -18*10^8) даёт сложение с переполнением в отрицательные числа
            //wgs.x = wgsMin.x + ( wgsMax.x - wgsMin.x ) / 2;
            //wgs.y = wgsMin.y + ( wgsMax.y - wgsMin.y ) / 2;
            //--- 3. используем модификацию варианта п.1 с погрешностью/сдвигом целочисленного деления
            //--- на 1 ед. в меньшую сторону, но в нашем случае это не критично
            wgs.x = wgsMin.x / 2 + wgsMax.x / 2
            wgs.y = wgsMin.y / 2 + wgsMax.y / 2
            wgs_pix(wgs, pixCalc)

            //--- координаты совпали (с некоторым допуском), мы подобрали соответствующую компоненту wgs-координаты
            val isFoundX = abs( pix.x - pixCalc.x ) < delta
            val isFoundY = abs( pix.y - pixCalc.y ) < delta
            //--- совпали обе компоненты координат - выходим
            if( isFoundX && isFoundY ) break

            //--- коррекция поиска
            if( !isFoundX ) {
                if( pix.x < pixCalc.x ) wgsMax.x = wgs.x
                else wgsMin.x = wgs.x
            }

            //--- коррекция поиска по Y "в обратную сторону", т.к. оси Y направлены в противоположные стороны
            if( !isFoundY ) {
                if( pix.y < pixCalc.y ) wgsMin.y = wgs.y
                else wgsMax.y = wgs.y
            }
        }
        return wgs
    }

    fun distancePrj(pix1: XyPoint, pix2: XyPoint, delta: Int ): Double {
        val wgs1 = pix_wgs(pix1, delta)
        val wgs2 = pix_wgs(pix2, delta)
        return distance(wgs1.x / WGS_KOEF_d, wgs1.y / WGS_KOEF_d, wgs2.x / WGS_KOEF_d, wgs2.y / WGS_KOEF_d)
    }

    fun distanceWGS(wgs1: XyPoint, wgs2: XyPoint): Double = distance(wgs1.x / WGS_KOEF_d, wgs1.y / WGS_KOEF_d, wgs2.x / WGS_KOEF_d, wgs2.y / WGS_KOEF_d)

    fun distance( lon1: Double, lat1: Double, lon2: Double, lat2: Double ): Double {
        val D2R = PI / 180
        val a = 6378137.0             // Основные полуоси
        //final double b = 6356752.314245;        // Неосновные полуоси - не используется
        val e2 = 0.006739496742337    // Квадрат эксцентричности эллипсоида
        //final double f = 0.003352810664747;     // Выравнивание эллипсоида - не используется

        //--- Вычисляем разницу между двумя долготами и широтами и получаем среднюю широту
        val fdLambda = ( lon1 - lon2 ) * D2R
        val fdPhi = ( lat1 - lat2 ) * D2R
        val fPhimean = ( lat1 + lat2 ) / 2 * D2R

        //--- Вычисляем меридианные и поперечные радиусы кривизны средней широты
        val fTemp = 1 - e2 * sin( fPhimean ).pow( 2.0 )
        val fRho = a * ( 1 - e2 ) / fTemp.pow( 1.5 )
        val fNu = a / sqrt( 1 - e2 * sin( fPhimean ).pow( 2.0 ) ) //!!! можно использовать fTemp?
        //double fNu = a / ( sqrt( fTemp ) );

        //--- Вычисляем угловое расстояние
        var fz = sqrt( sin( fdPhi / 2 ).pow( 2.0 ) + cos( lat2 * D2R ) * cos( lat1 * D2R ) * sin( fdLambda / 2 ).pow( 2.0 ) )
        fz = 2 * asin( fz )

        //--- Вычисляем смещение
        var fAlpha = cos( lat2 * D2R ) * sin( fdLambda ) / sin( fz )
        fAlpha = asin( fAlpha )

        //--- Вычисляем радиус Земли
        val fR = fRho * fNu / ( fRho * sin( fAlpha ).pow( 2.0 ) + fNu * cos( fAlpha ).pow( 2.0 ) )
        return fz * fR
    }
}
