package foatto.compose.control.model.composite

import foatto.compose.control.ChartControl
import foatto.compose.control.MapControl
import foatto.compose.control.SchemeControl

class CompositeBlockControl(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,

    val chartBlock: ChartControl? = null,
    val mapBlock: MapControl? = null,
    val schemeBlock: SchemeControl? = null,
)