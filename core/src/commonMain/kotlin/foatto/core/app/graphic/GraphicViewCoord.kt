package foatto.core.app.graphic

class GraphicViewCoord( var t1: Int, var t2: Int ) {

    val width: Int
        get() = t2 - t1

//    constructor( view: GraphicViewCoord ) : this( view.t1, view.t2 )
//
//    fun set( aX1: Long, aX2: Long ) {
//        t1 = aX1
//        t2 = aX2
//    }

    fun moveRel( dx: Int ) {
        t1 += dx
        t2 += dx
    }

//    fun isEquals( view: GraphicViewCoord ): Boolean {
//        return t1 == view.t1 && t2 == view.t2
//    }

}
