package foatto.compose.control.model.composite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import foatto.compose.control.ChartControl
import foatto.compose.control.MapControl
import foatto.compose.control.SchemeControl
import foatto.compose.control.TableControl

class CompositeBlockControl(
    val id: Int,

    val isStatic: Boolean,

    val chartBlock: ChartControl? = null,
    val mapBlock: MapControl? = null,
    val schemeBlock: SchemeControl? = null,
    val tableBlock: TableControl? = null,
) {
    var isHidden: Boolean by mutableStateOf(false)

    var x: Int by mutableIntStateOf(0)
    var y: Int by mutableIntStateOf(0)
    var w: Int by mutableIntStateOf(0)
    var h: Int by mutableIntStateOf(0)

    fun isIntersect(xx: Int, yy: Int, ww: Int, hh: Int): Boolean {
        val x2 = x + w
        val y2 = y + h
        val xx2 = xx + ww
        val yy2 = yy + hh

        return xx < x2 && xx2 > x && yy < y2 && yy2 > y
    }
}