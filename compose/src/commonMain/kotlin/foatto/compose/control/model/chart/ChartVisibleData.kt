package foatto.compose.control.model.chart

import androidx.compose.runtime.MutableState

class ChartVisibleData(
    val descr: String,
    val name: String,
    val check: MutableState<Boolean>,
)
