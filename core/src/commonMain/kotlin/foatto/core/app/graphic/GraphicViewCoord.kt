package foatto.core.app.graphic

class GraphicViewCoord(var t1: Int, var t2: Int) {

    val width: Int
        get() = t2 - t1

    fun moveRel(dx: Int) {
        t1 += dx
        t2 += dx
    }
}
