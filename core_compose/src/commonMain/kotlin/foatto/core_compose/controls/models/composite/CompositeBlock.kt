package foatto.core_compose.controls.models.composite

import foatto.core_compose.controls.AbstractXyControl
import foatto.core_compose.controls.ChartControl

class CompositeBlock(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,

    val xyBlock: AbstractXyControl? = null,
    val chartBlock: ChartControl? = null,
)