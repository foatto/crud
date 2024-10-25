package foatto.server.util

import foatto.core.model.response.xy.geom.XyPoint
import kotlin.math.E
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.log
import kotlin.math.sqrt

fun ch(x: Double): Double = (exp(x) + exp(-x)) / 2

fun arch(x: Double): Double = log(x + sqrt(x * x - 1), E)

fun getAngle(x: Double, y: Double): Double = Math.toDegrees(atan2(y, x))

//--- возвращает расположение и угол поворота текста, расположенного вдоль отрезка (независимо от направления/вектора отрезка)
fun getTextPointAndAngle(point1: XyPoint, point2: XyPoint, textPoint: XyPoint): Double {
    //--- нормализуем координаты отрезка - чтобы текст всегда шел слева направо и не вверх ногами
    val p1 = if (point1.x <= point2.x) {
        point1
    } else {
        point2
    }
    val p2 = if (point1.x <= point2.x) {
        point2
    } else {
        point1
    }

    textPoint.set(p1.x + (p2.x - p1.x) / 2, p1.y + (p2.y - p1.y) / 2)
    return if (p1.x == p2.x) {
        90.0
    } else {
        Math.toDegrees(atan(1.0 * (p2.y - p1.y) / (p2.x - p1.x)))
    }
}

//--- возвращает расположение и угол поворота текста, расположенного вдоль отрезка (независимо от направления/вектора отрезка)
fun getTextPointAndAngle(aX1: Double, aY1: Double, aX2: Double, aY2: Double): DoubleArray {
    //--- нормализуем координаты отрезка - чтобы текст всегда шел слева направо и не вверх ногами
    val x1: Double
    val y1: Double
    val x2: Double
    val y2: Double
    if (aX1 <= aX2) {
        x1 = aX1
        y1 = aY1
        x2 = aX2
        y2 = aY2
    } else {
        x1 = aX2
        y1 = aY2
        x2 = aX1
        y2 = aY1
    }
    return doubleArrayOf(
        x1 + (x2 - x1) / 2, y1 + (y2 - y1) / 2,
        if (x1 == x2) {
            90.0
        } else {
            Math.toDegrees(atan(1.0 * (y2 - y1) / (x2 - x1)))
        }
    )
}

//    //--- возвращает левый (по ходу движения) перпендикуляр к заданному вектору
//    public static final Point2D getOrthoLeft( Point2D p1, Point2D p2, Point2D pd ) {
//        if( pd == null ) pd = new Point2D.Double();
//        double dist = Point2D.distance( p1.getX(), p1.getY(), p2.getX(), p2.getY() );
//        if( dist == 0 ) pd.setLocation( 0, 0 );
//        else {
//            double dx = ( p2.getX() - p1.getX() ) / dist;
//            double dy = ( p2.getY() - p1.getY() ) / dist;
//            pd.setLocation( dy, -dx );
//        }
//        return pd;
//    }

//    //--- превращает полилинию в линейный полигон заданной ширины
//    public static final ArrayList<Point2D> convertPolylineToPolygon( ArrayList<Point2D> alPointSour, double polygonWidth,
//                                                                     ArrayList<Point2D> alPointDest ) {
//        //--- набор полигонных точек слева и справа по ходу полилинии
//        ArrayList<Point2D> alLeft = new ArrayList<Point2D>();
//        ArrayList<Point2D> alRight = new ArrayList<Point2D>();
//
//        Point2D pd1 = new Point2D.Double();
//        Point2D pd2 = new Point2D.Double();
//        for( int i = 0; i < alPointSour.size(); i++ ) {
//            if( i == 0 ) {
//                Point2D p0 = alPointSour.get( 0 );
//                getOrthoLeft( p0, alPointSour.get( 1 ), pd1 );
//                alLeft.add( new Point2D.Double( p0.getX() + pd1.getX() * polygonWidth,
//                                                p0.getY() + pd1.getY() * polygonWidth ) );
//                //--- правую сторону заполняем в обратном порядке,
//                //--- т.е. первая точка полилинии будет последней точкой полигона с правой стороны
//                alRight.add( 0, new Point2D.Double( p0.getX() - pd1.getX() * polygonWidth,
//                                                    p0.getY() - pd1.getY() * polygonWidth ) );
//            }
//            else if( i == alPointSour.size() - 1 ) {
//                Point2D p0 = alPointSour.get( alPointSour.size() - 1 );
//                getOrthoLeft( alPointSour.get( alPointSour.size() - 2 ), p0, pd1 );
//                alLeft.add( new Point2D.Double( p0.getX() + pd1.getX() * polygonWidth,
//                                                p0.getY() + pd1.getY() * polygonWidth ) );
//                alRight.add( 0, new Point2D.Double( p0.getX() - pd1.getX() * polygonWidth,
//                                                    p0.getY() - pd1.getY() * polygonWidth ) );
//            }
//            else {
//                Point2D p1 = alPointSour.get( i - 1 );
//                Point2D p0 = alPointSour.get( i );
//                Point2D p2 = alPointSour.get( i + 1 );
//                getOrthoLeft( p1, p0, pd1 );
//                getOrthoLeft( p0, p2, pd2 );
//                //--- среднее арифметическое - как бы средний угол между векторами :)
//                pd1.setLocation( ( pd1.getX() + pd2.getX() ) / 2, ( pd1.getY() + pd2.getY() ) / 2 );
//                alLeft.add( new Point2D.Double( p0.getX() + pd1.getX() * polygonWidth,
//                                                p0.getY() + pd1.getY() * polygonWidth ) );
//                alRight.add( 0, new Point2D.Double( p0.getX() - pd1.getX() * polygonWidth,
//                                                    p0.getY() - pd1.getY() * polygonWidth ) );
//            }
//        }
//        //--- подготовка и возврат результата
//        if( alPointDest == null ) alPointDest = new ArrayList<Point2D>();
//        alPointDest.addAll( alLeft );
//        alPointDest.addAll( alRight );
//        return alPointDest;
//    }

//--- книжные варианты, иногда глючат на вертикальных и горизонтальных линиях.
//--- Лучше пользоваться Line2D.ptSegDist
//    //--- возвращает расстояние между точкой и отрезком или прямой ------------------------------
//    public static final double getDistance( double x0, double y0,
//                                            double t1, double y1,
//                                            double t2, double y2,
//                                            boolean isSegment ) {
//        return getNormalPointAndDistance( x0, y0, t1, y1, t2, y2, isSegment ) [ 2 ];
////        double dx = t2 - t1;
////        double dy = y2 - y1;
////
////        double dist = 0;
////
////        //--- если точка за краями отрезка
////        if( isSegment ) {
////            double[] arr = getNormalPoint( x0, y0, t1, y1, t2, y2 );
////            //--- точка за краями отрезка
////            if( arr.length > 3 ) dist = arr[ 2 ];
////            else dist = ( (y0 - y1) * dx - (x0 - t1) * dy ) / Math.sqrt( dx * dx + dy * dy );
////        }
////        //--- расстояние между точкой и прямой
////        else dist = ( (y0 - y1) * dx - (x0 - t1) * dy ) / Math.sqrt( dx * dx + dy * dy );
////        //--- расстояние может быть отрицательным - в зависимости, с какой стороны точка
////        return Math.abs( dist );
//    }

//!!! вроде бы пока не нужны, позже надо пересмотреть генерализацию с учетом новой пиксельной системы координат

//    //--- вернуть точку пересечения нормали из заданной точки к линии/отрезку
//    //--- [0] = xp
//    //--- [1] = yp
//    //--- [2] = расстояние от заданной точки до точки пересечения или до ближайшего конца отрезка, если точка пересения лежит за краями отрезка
//    //--- [3] = 0/1 - номер ближайшего конца отрезка, если точка перечения лежит за краями отрезка
//    public static double[] getNormalPointAndDistance( double x0, double y0,
//                                                      double t1, double y1,
//                                                      double t2, double y2,
//                                                      boolean isSegment ) {
//        double A = y1 - y2;
//        double B = t2 - t1;
//        double C = t1 * y2 - t2 * y1;
//
//        double xp = ( B * B * x0 - A * B * y0 - A * C ) / ( A * A + B * B );
//        double yp = ( - A * B * x0 + A * A * y0 - B * C ) / ( A * A + B * B );
//
//        //--- для отрезка проверяем выход точки пересечения за края отрезка
//        if( isSegment && ( xp < Math.min( t1, t2 ) || xp > Math.max( t1, t2 ) ||
//                           yp < Math.min( y1, y2 ) || yp > Math.max( y1, y2 ) ) ) {
//            double dist1 = Point2D.distance( x0, y0, t1, y1 );
//            double dist2 = Point2D.distance( x0, y0, t2, y2 );
//            return new double[] { xp, yp, Math.min( dist1, dist2 ), ( dist1 <= dist2 ? 0 : 1 ) };
//        }
//        else return new double[] { xp, yp, Point2D.distance( x0, y0, xp, yp ) };
//    }
//
//    //--- получить точку пересения двух отрезков - книжный вариант ---
//    public static double[] getSegmentIntersect( double ax1, double ay1, double ax2, double ay2,
//                                                double bx1, double by1, double bx2, double by2 ) {
//        double A1 = ay1 - ay2;
//        double B1 = ax2 - ax1;
//        double C1 = ax1 * ay2 - ax2 * ay1;
//
//        double A2 = by1 - by2;
//        double B2 = bx2 - bx1;
//        double C2 = bx1 * by2 - bx2 * by1;
//
//        //--- проверка на параллельность
//        if( A1 * B2 == A2 * B1 ) return null;
//        else {
//            double xp = ( B1 * C2 - B2 * C1 ) / ( A1 * B2 - A2 * B1 );
//            double yp = ( C1 * A2 - C2 * A1 ) / ( A1 * B2 - A2 * B1 );
//
//            if( xp < Math.min( ax1, ax2 ) || xp > Math.max( ax1, ax2 ) ||
//                yp < Math.min( ay1, ay2 ) || yp > Math.max( ay1, ay2 ) ||
//                xp < Math.min( bx1, bx2 ) || xp > Math.max( bx1, bx2 ) ||
//                yp < Math.min( by1, by2 ) || yp > Math.max( by1, by2 ) ) return null;
//            else
//                return new double[] { xp, yp };
//        }
//    }
//
//    //--- получить точку пересения двух отрезков - самописный вариант ---
//    public static double[] getSegmentIntersect2( double ax1, double ay1, double ax2, double ay2,
//                                                 double bx1, double by1, double bx2, double by2 ) {
//
//        double aMinX = Math.min( ax1, ax2 );
//        double aMaxX = Math.max( ax1, ax2 );
//        double aMinY = Math.min( ay1, ay2 );
//        double aMaxY = Math.max( ay1, ay2 );
//
//        double bMinX = Math.min( bx1, bx2 );
//        double bMaxX = Math.max( bx1, bx2 );
//        double bMinY = Math.min( by1, by2 );
//        double bMaxY = Math.max( by1, by2 );
//
//        //--- проверка на абсолютную удаленность
//        if( aMinX > bMaxX || aMaxX < bMinX || aMinY > bMaxY || aMaxY < bMinY ) return null;
//
//        double adx = ax2 - ax1;
//        double ady = ay2 - ay1;
//        double bdx = bx2 - bx1;
//        double bdy = by2 - by1;
//
//        //--- первый отрезок - вертикальный
//        if( adx == 0 ) {
//            //--- второй отрезок тоже вертикальный
//            if( bdx == 0 ) return null;
//            //--- если второй - горизонтальный - то они точно пересекаются, т.к. абсолютная удаленность уже проверена
//            else if( bdy == 0 ) return new double[] { ax1, by1 };
//            else {
//                double py = ( ax1 - bx1 ) * bdy / bdx + by1;
//                if( py >= aMinY && py <= aMaxY ) return new double[] { ax1, py };
//                else return null;
//            }
//        }
//        //--- первый отрезок - горизонтальный
//        else if( ady == 0 ) {
//            //--- второй отрезок тоже горизонтальный
//            if( bdy == 0 ) return null;
//            //--- если второй отрезок - вертикальный - то они точно пересекаются, т.к. абсолютная удаленность уже проверена
//            else if( bdx == 0 ) return new double[] { bx1, ay1 };
//            else {
//                double px = ( ay1 - by1 ) * bdx / bdy + bx1;
//                if( px >= aMinX && px <= aMaxX ) return new double[] { px, ay1 };
//                else return null;
//            }
//        }
//        //--- иначе первый отрезок - наклонный
//        else {
//            //--- второй отрезок - вертикальный
//            if( bdx == 0 ) {
//                double py = ( bx1 - ax1 ) * ady / adx + ay1;
//                if( py >= bMinY && py <= bMaxY ) return new double[] { bx1, py };
//                else return null;
//            }
//            //--- второй отрезок - горизонтальный
//            else if( bdy == 0 ) {
//                double px = ( by1 - ay1 ) * adx / ady + ax1;
//                if( px >= bMinX && px <= bMaxX ) return new double[] { px, by1 };
//                else return null;
//            }
//            //--- второй отрезок - наклонный и параллелен первому
//            else if( ady / adx == bdy / bdx ) return null;
//            //--- иначе они где-то пересекаются
//            else {
//                double ak = ady / adx;
//                double bk = bdy / bdx;
//                double px = ( ak * ax1 - bk * bx1 + by1 - ay1 ) / ( ak - bk );
//                double py = ak * ( px - ax1 ) + ay1;
//
//                if( px >= aMinX && px <= aMaxX && py >= aMinY && py <= aMaxY &&
//                    px >= bMinX && px <= bMaxX && py >= bMinY && py <= bMaxY )
//
//                    return new double[] { px, py };
//                else
//                    return null;
//            }
//        }
//    }
//
//    //--- проверка на пересечение и/или наложение полигонов -----------------
//    public static boolean isIntersect( Polygon p1, Polygon p2 ) {
//
//        Rectangle b1 = p1.getBounds();
//        int w1 = b1.width;
//        int h1 = b1.height;
//        int x1a = b1.x;
//        int y1a = b1.y;
//        int x1b = x1a + w1;
//        int y1b = y1a + h1;
//
//        Rectangle b2 = p2.getBounds();
//        int w2 = b2.width;
//        int h2 = b2.height;
//        int x2a = b2.x;
//        int y2a = b2.y;
//        int x2b = x2a + w2;
//        int y2b = y2a + h2;
//
//        int i, j;
//
//        //--- проверка на абсолютную удаленность
//        if( x2a >= x1b || x1a >= x2b || y2a >= y1b || y1a >= y2b ) return false;
//
//        //--- после этой проверки можно провести дополнительную инициализацию переменных
//        int   n1 = p1.npoints;
//        int[] arrX1 = p1.xpoints;
//        int[] arrY1 = p1.ypoints;
//
//        int   n2 = p2.npoints;
//        int[] arrX2 = p2.xpoints;
//        int[] arrY2 = p2.ypoints;
//
//        //--- проверка на вложенность
//        for( i = 0; i < n1; i++ ) if( p2.contains( arrX1[i], arrY1[i] ) ) return true;
//        for( i = 0; i < n2; i++ ) if( p1.contains( arrX2[i], arrY2[i] ) ) return true;
//
//        //--- проверка на взаимное пересечение сторон
//        for( i = 0; i < n1; i++ )
//
//            for( j = 0; j < n2; j++ )
//
//                if( Line2D.linesIntersect( arrX1[ i==0 ? n1-1 : i-1 ],  arrY1[ i==0 ? n1-1 : i-1 ],
//                                           arrX1[ i ],                  arrY1[ i ],
//                                           arrX2[ j==0 ? n2-1 : j-1 ],  arrY2[ j==0 ? n2-1 : j-1 ],
//                                           arrX2[ j ],                  arrY2[ j ] ) )
//                    return true;
//
//        return false;
//    }
//
//    //--- проверка на полное вхождение меньшего полигона в больший -----------------
//    public static boolean isInner( Polygon small, Polygon big ) {
//
//        Rectangle b1 = small.getBounds();
//        int w1 = b1.width;
//        int h1 = b1.height;
//        int x1a = b1.x;
//        int y1a = b1.y;
//        int x1b = x1a + w1;
//        int y1b = y1a + h1;
//
//        Rectangle b2 = big.getBounds();
//        int w2 = b2.width;
//        int h2 = b2.height;
//        int x2a = b2.x;
//        int y2a = b2.y;
//        int x2b = x2a + w2;
//        int y2b = y2a + h2;
//
//        int i, j;
//
//        //--- проверка на абсолютную удаленность
//        if( x2a >= x1b || x1a >= x2b || y2a >= y1b || y1a >= y2b ) return false;
//
//        //--- после этой проверки можно провести дополнительную инициализацию переменных
//        int   n1 = small.npoints;
//        int[] arrX1 = small.xpoints;
//        int[] arrY1 = small.ypoints;
//
//        int   n2 = big.npoints;
//        int[] arrX2 = big.xpoints;
//        int[] arrY2 = big.ypoints;
//
//        //--- проверка на вложенность
//        for( i = 0; i < n1; i++ ) if( ! big.contains( arrX1[i], arrY1[i] ) ) return false;
//
//        //--- проверка на взаимное пересечение сторон
//        for( i = 0; i < n1; i++ )
//
//            for( j = 0; j < n2; j++ )
//
//                if( Line2D.linesIntersect( arrX1[ i==0 ? n1-1 : i-1 ],  arrY1[ i==0 ? n1-1 : i-1 ],
//                                           arrX1[ i ],                  arrY1[ i ],
//                                           arrX2[ j==0 ? n2-1 : j-1 ],  arrY2[ j==0 ? n2-1 : j-1 ],
//                                           arrX2[ j ],                  arrY2[ j ] ) )
//                    return false;
//
//        return true;
//    }
//
//    //--- проверка на прямоугольность полигона -----------------
//    public static boolean isRect( Polygon p ) {
//
//        if( p.npoints != 4 ) return false;
//
//        int x0 = p.xpoints[0];
//        int t1 = p.xpoints[1];
//        int t2 = p.xpoints[2];
//        int x3 = p.xpoints[3];
//
//        int y0 = p.ypoints[0];
//        int y1 = p.ypoints[1];
//        int y2 = p.ypoints[2];
//        int y3 = p.ypoints[3];
//
//        if( x0 == x3 && t1 == t2 && y0 == y1 && y2 == y3 ) return true;
//        if( x0 == t1 && t2 == x3 && y0 == y3 && y1 == y2 ) return true;
//
//        return false;
//    }
//
//    //--- определить площадь полигона ---------------------------------------------
//    public static int getPolygonArea( Polygon poly ) {
//
//        int area = 0;
//        int x = poly.getBounds().x;
//        int y = poly.getBounds().y;
//        int i, j;
//
//        for( i = 0; i < poly.getBounds().width; i++ )
//            for( j = 0; j < poly.getBounds().height; j++ )
//
//                if( poly.contains( x + i, y + j ) ) area++;
//
//        return area;
//    }
//
//    //--- сравнить два полигона независимо от их местоположения и угла поворота -------
//    //
//    // p1, p2       - сравниваемые полигоны
//    // lenDelta     - допуск на разность длин
//    // angleDelta   - допуск на разность углов
//    // enableMirror - допускается зеркальное отражение
//    public static boolean comparePolygon( Polygon p1, Polygon p2,
//                                          int lenDelta, int angleDelta,
//                                          boolean enableMirror ) {
//
//        //--- для начала сравним кол-во вершин
//        if( p1.npoints != p2.npoints ) return false;
//
//        int n = p1.npoints;
//
//        int[] arrX1 = p1.xpoints;
//        int[] arrY1 = p1.ypoints;
//
//        int[] arrX2 = p2.xpoints;
//        int[] arrY2 = p2.ypoints;
//
//        int[] arrLen1 = new int[ n ];
//        int[] arrLen2 = new int[ n ];
//
//        //--- вычислить длины сторон и периметры
//        int perim1 = 0;
//        int perim2 = 0;
//        int i;
//
//        for( i = 0; i < n; i++ ) {
//
//            arrLen1[ i ] = (int) Math.sqrt( Math.pow( arrX1[ ( i + 1 ) % n ] - arrX1[ i ], 2 ) +
//                                            Math.pow( arrY1[ ( i + 1 ) % n ] - arrY1[ i ], 2 ) );
//            arrLen2[ i ] = (int) Math.sqrt( Math.pow( arrX2[ ( i + 1 ) % n ] - arrX2[ i ], 2 ) +
//                                            Math.pow( arrY2[ ( i + 1 ) % n ] - arrY2[ i ], 2 ) );
//            perim1 += arrLen1[ i ];
//            perim2 += arrLen2[ i ];
//        }
//        //--- если периметры разнятся более, чем на сумму допусков сторон,
//        //--- то полигоны не равны
//        if( Math.abs( perim1 - perim2 ) > lenDelta * n ) return false;
//
//        //--- сопоставление сторон
//        boolean find;
//        boolean forward;
//        int a1, a2, da1, da2;
//
//        //--- сравниваем стартовую (т.е. 0-ю) сторону первой фигуры
//        //--- последовательно с каждой стороной второй фигуры
//        for( int offset = 0; offset < n; offset++ ) {
//
//            //--- если разница длин больше допуска, то переходим к следующей стороне
//            if( Math.abs( arrLen1[ 0 ] - arrLen2[ offset ] ) > lenDelta ) continue;
//
//            //--- поиск в прямую сторону
//            //--- смещение стартовой стороны второй фигуры определено.
//            //--- бежим по сторонам вперед
//            for( i = 1; i < n; i++ )
//
//                //--- если найдено хотя бы одно несовпадение, то это смещение не подходит
//                if( Math.abs( arrLen1[ i ] - arrLen2[ ( i + offset ) % n ] ) > lenDelta ) break;
//
//            //--- пробежали все стороны второй фигуры, несовпадений не было.
//            //--- значит стартовое смещение сторон найдено правильно.
//            if( i == n ) {
//
//                //--- теперь сравниваем изменения углов векторов обхода контура (о как ! :)
//
//                for( i = 0; i < n; i++ ) {
//
//                    //--- по первой фигуре
//                    a1 = (int) Math.toDegrees( Math.atan2( arrY1[ ( i + 1 ) % n ] - arrY1[ i ],
//                                                           arrX1[ ( i + 1 ) % n ] - arrX1[ i ]) );
//                    a2 = (int) Math.toDegrees( Math.atan2( arrY1[ ( i + 2 ) % n ] - arrY1[ ( i + 1 ) % n ],
//                                                           arrX1[ ( i + 2 ) % n ] - arrX1[ ( i + 1 ) % n ]) );
//                    //--- борьба с отрицательным представлением углов
//                    da1 = ( a2 - a1 + 360 ) % 360;
//
//                    //--- по второй фигуре
//                    a1 = (int) Math.toDegrees( Math.atan2( arrY2[ ( offset + i + 1 ) % n ] - arrY2[ ( offset + i ) % n ],
//                                                           arrX2[ ( offset + i + 1 ) % n ] - arrX2[ ( offset + i ) % n ]) );
//                    a2 = (int) Math.toDegrees( Math.atan2( arrY2[ ( offset + i + 2 ) % n ] - arrY2[ ( offset + i + 1 ) % n ],
//                                                           arrX2[ ( offset + i + 2 ) % n ] - arrX2[ ( offset + i + 1 ) % n ]) );
//                    //--- борьба с отрицательным представлением углов
//                    da2 = ( a2 - a1 + 360 ) % 360;
//
//                    if( enableMirror ) {
//
//                        if( Math.abs( da2 ) - Math.abs( da1 ) > angleDelta ) break;
//
//                    }
//                    else {
//
//                        if( da2 - da1 > angleDelta ) break;
//                    }
//                }
//
//                //--- углы тоже совпали - ура.
//                if( i == n ) return true;
//            }
//
//            //--- поиск в обратную сторону
//            //--- смещение стартовой стороны второй фигуры определено.
//            //--- бежим по сторонам обратно
//            for( i = 1; i < n; i++ )
//
//                //--- если найдено хотя бы одно несовпадение, то это смещение не подходит
//                if( Math.abs( arrLen1[ i ] - arrLen2[ ( n - i + offset ) % n ] ) > lenDelta ) break;
//
//            //--- пробежали все стороны второй фигуры, несовпадений не было.
//            //--- значит стартовое смещение сторон найдено правильно.
//            if( i == n ) {
//
//                //--- теперь сравниваем изменения углов векторов обхода контура (о как ! :)
//
//                for( i = 0; i < n; i++ ) {
//
//                    //--- по первой фигуре
//                    a1 = (int) Math.toDegrees( Math.atan2( arrY1[ ( i + 1 ) % n ] - arrY1[ i ],
//                                                           arrX1[ ( i + 1 ) % n ] - arrX1[ i ]) );
//                    a2 = (int) Math.toDegrees( Math.atan2( arrY1[ ( i + 2 ) % n ] - arrY1[ ( i + 1 ) % n ],
//                                                           arrX1[ ( i + 2 ) % n ] - arrX1[ ( i + 1 ) % n ]) );
//                    //--- борьба с отрицательным представлением углов
//                    da1 = ( a2 - a1 + 360 ) % 360;
//
//                    //--- по второй фигуре
//                    a1 = (int) Math.toDegrees( Math.atan2( arrY2[ ( offset + i ) % n ] - arrY2[ ( offset + i + 1 ) % n ],
//                                                           arrX2[ ( offset + i ) % n ] - arrX2[ ( offset + i + 1 ) % n ]) );
//                    a2 = (int) Math.toDegrees( Math.atan2( arrY2[ ( offset + i - 1 ) % n ] - arrY2[ ( offset + i ) % n ],
//                                                           arrX2[ ( offset + i - 1 ) % n ] - arrX2[ ( offset + i ) % n ]) );
//                    //--- борьба с отрицательным представлением углов
//                    da2 = ( a2 - a1 + 360 ) % 360;
//
//                    if( enableMirror ) {
//
//                        if( Math.abs( da2 ) - Math.abs( da1 ) > angleDelta ) break;
//
//                    }
//                    else {
//
//                        if( da2 - da1 > angleDelta ) break;
//                    }
//                }
//
//                //--- углы тоже совпали - ура.
//                if( i == n ) return true;
//            }
//        }
//        //--- до сих пор ничего не найдено - полигоны не равны
//        return false;
//    }
//
//    //--- генерализация полилинии/полигона
//    public static void generalizePoly( ArrayList<Point2D> alPointOld, int[] arrScale, double genKoef,
//                                       ArrayList<Point2D> alPointNew, ArrayList<Integer> alPointMaxScale ) {
//
//        //--- массив координат предыдущих точек
//        double[] arrLastX = new double[ arrScale.length ];
//        double[] arrLastY = new double[ arrScale.length ];
//
//        for( int pointIndex = 0; pointIndex < alPointOld.size(); pointIndex++ ) {
//            Point2D p = alPointOld.get( pointIndex );
//            double x = p.getX();
//            double y = p.getY();
//            //--- первая точка доступна для всех масштабов
//            if( alPointNew.size() == 0 ) {
//                alPointNew.add( p );
//                alPointMaxScale.add( arrScale[ arrScale.length - 1 ] );
//                for( int i = 0; i < arrScale.length; i++ ) {
//                    arrLastX[ i ] = p.getX();
//                    arrLastY[ i ] = p.getY();
//                }
//            }
//            else
//                //--- ищем максимальный масштаб, для которого эта точка достаточно далека от предыдущей
//                for( int scaleIndex = arrScale.length - 1; scaleIndex >= 0; scaleIndex-- )
//                    //--- (точность вычисления расстояния не важна, используем проекционные координаты)
//                    if( Point2D.distance( x, y, arrLastX[ scaleIndex ], arrLastY[ scaleIndex ] ) > arrScale[ scaleIndex ] * genKoef ) {
//                        alPointNew.add( p );
//                        alPointMaxScale.add( arrScale[ scaleIndex ] );
//                        for( int i = 0; i <= scaleIndex; i++ ) {
//                            arrLastX[ i ] = x;
//                            arrLastY[ i ] = y;
//                        }
//                        break;
//                    }
//        }
//    }
//
//    //--- генерализация полилинии/полигона
//    public static void generalizePoly( ArrayList<Integer> alPointOldX, ArrayList<Integer> alPointOldY,
//                                       int[] arrScale, double genKoef,
//                                       ArrayList<Integer> alPointNewX, ArrayList<Integer> alPointNewY,
//                                       ArrayList<Integer> alPointMaxScale ) {
//
//        //--- массив координат предыдущих точек
//        int[] arrLastX = new int[ arrScale.length ];
//        int[] arrLastY = new int[ arrScale.length ];
//
//        for( int pointIndex = 0; pointIndex < alPointOldX.size(); pointIndex++ ) {
//            int x = alPointOldX.get( pointIndex );
//            int y = alPointOldY.get( pointIndex );
//            //--- первая точка доступна для всех масштабов
//            if( alPointNewX.isEmpty() ) {
//                alPointNewX.add( x );
//                alPointNewY.add( y );
//                alPointMaxScale.add( arrScale[ arrScale.length - 1 ] );
//                for( int i = 0; i < arrScale.length; i++ ) {
//                    arrLastX[ i ] = x;
//                    arrLastY[ i ] = y;
//                }
//            }
//            else
//                //--- ищем максимальный масштаб, для которого эта точка достаточно далека от предыдущей
//                for( int scaleIndex = arrScale.length - 1; scaleIndex >= 0; scaleIndex-- )
//                    //--- (точность вычисления расстояния не важна, используем проекционные координаты)
//                    if( Point2D.distance( x, y, arrLastX[ scaleIndex ], arrLastY[ scaleIndex ] ) > ( arrScale[ scaleIndex ] ) * genKoef ) {
//                        alPointNewX.add( x );
//                        alPointNewY.add( y );
//                        alPointMaxScale.add( arrScale[ scaleIndex ] );
//                        for( int i = 0; i <= scaleIndex; i++ ) {
//                            arrLastX[ i ] = x;
//                            arrLastY[ i ] = y;
//                        }
//                        break;
//                    }
//        }
//    }
