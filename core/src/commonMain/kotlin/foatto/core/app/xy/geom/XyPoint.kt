package foatto.core.app.xy.geom

import kotlin.math.sqrt

//import kotlinx.serialization.Serializable

//@Serializable
class XyPoint( var x: Int, var y: Int ) {

    constructor( aX: Float, aY: Float ) : this( aX.toInt(), aY.toInt() )

    fun set( aP: XyPoint) { set( aP.x, aP.y ) }

    fun set( aX: Float, aY: Float ) { set( aX.toInt(), aY.toInt() ) }

    fun set( aX: Int, aY: Int ) {
        x = aX
        y = aY
    }

    fun distance( p: XyPoint): Double {
        return distance(x.toDouble(), y.toDouble(), p.x.toDouble(), p.y.toDouble())
    }

    companion object {
        //--- double-координаты, иначе получаем переполнение при возведении в квадрат начиная с 65 км
        fun distance( x1: Double, y1: Double, x2: Double, y2: Double ): Double {
            val dx = x1 - x2
            val dy = y1 - y2
            return sqrt( dx * dx + dy * dy )
        }
    }
}
