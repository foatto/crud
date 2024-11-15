package foatto.compose.control.model.chart

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState

class ChartLineData(
    val isVisible: MutableState<Boolean>,
    val x1: MutableFloatState,
    val y1: MutableFloatState,
    val x2: MutableFloatState,
    val y2: MutableFloatState,
    var width: MutableFloatState,
)