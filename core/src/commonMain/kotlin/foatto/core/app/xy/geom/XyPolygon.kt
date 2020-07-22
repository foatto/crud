package foatto.core.app.xy.geom

//import kotlinx.serialization.Serializable

//@Serializable
class XyPolygon {

    val alPoint = mutableListOf<XyPoint>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun isContains(aP: XyPoint): Boolean {
        return isContains(aP.x, aP.y)
    }

    fun isContains( aX: Int, aY: Int ): Boolean {
        var crossCount = 0

        for( i in alPoint.indices ) {
            val p1 = alPoint[ i ]
            val p2 = alPoint[ if( i + 1 == alPoint.size ) 0 else i + 1 ]

            crossCount += checkPointCrossing( aX.toDouble(), aY.toDouble(), p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble() )
        }
        return crossCount != 0
    }

    //--- взято из XyRect ---
    //
    //    public boolean isIntersect( XyLine aLine ) {
    //        return isIntersect( aLine.t1, aLine.y1, aLine.t2, aLine.y2 );
    //    }
    //
    //    public boolean isIntersect( double aX1, double aY1, double aX2, double aY2 ) {
    //        int outCode2 = getOutCode( aX2, aY2 );
    //        if( outCode2 == 0 ) return true;
    //
    //        int outCode1 = getOutCode( aX1, aY1 );
    //        while( outCode1 != 0) {
    //            if( ( outCode1 & outCode2 ) != 0 ) return false;
    //            if( ( outCode1 & ( OUT_LEFT | OUT_RIGHT ) ) != 0 ) {
    //                double tmpX = x;
    //                if( ( outCode1 & OUT_RIGHT ) != 0 ) tmpX += width;
    //                aY1 += ( tmpX - aX1 ) * ( aY2 - aY1 ) / ( aX2 - aX1 );
    //                aX1 = tmpX;
    //            }
    //            else {
    //                double tmpY = y;
    //                if( ( outCode1 & OUT_BOTTOM ) != 0 ) tmpY += height;
    //                aX1 += ( tmpY - aY1 ) * ( aX2 - aX1 ) / ( aY2 - aY1 );
    //                aY1 = tmpY;
    //            }
    //            outCode1 = getOutCode( aX1, aY1 );
    //        }
    //        return true;
    //    }
    //
    //    public boolean isIntersect( XyRect aRect ) {
    //        return x < aRect.x + aRect.width &&
    //               y < aRect.y + aRect.height &&
    //               x + width > aRect.x &&
    //               y + height > aRect.y;
    //    }

    //+ вариант isIntersect с другим полигоном

    //-------------------------------------------------------------------------------------------------------

    //--- проверка пересечения горизонтального луча от точки (px,py) к правой бесконечности с заданным отрезком
    //--- (double-аргументы во избежание переполнения при умножениях)
    private fun checkPointCrossing( px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double ): Int {
        //--- частные случаи:

        //--- луч проходит выше отрезка
        if( py < y1 && py < y2 ) return 0
        //--- луч проходит ниже отрезка или совпадает с горизонтальным отрезком
        if( py >= y1 && py >= y2 ) return 0
        //--- луч начался правее отрезка
        if( px >= x1 && px >= x2 ) return 0
        //--- луч начался левее отрезка
        if( px < x1 && px < x2 ) return if( y1 < y2 ) 1 else -1

        //--- общий случай

        //--- вычисляем x-координату _возможного_ пересечения луча с отрезком
        val x12 = x1 + 1.0 * ( py - y1 ) * ( x2 - x1 ) / ( y2 - y1 )
        //--- исходная точка правее или левее точки пересечения?
        return if( px >= x12 ) 0 else if( y1 < y2 ) 1 else -1
    }
}
