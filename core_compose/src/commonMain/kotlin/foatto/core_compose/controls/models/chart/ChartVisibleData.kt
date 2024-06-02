package foatto.core_compose.controls.models.chart

import androidx.compose.runtime.MutableState

class ChartVisibleData(
    val descr: String,
    val name: String,
    val check: MutableState<Boolean>,
)
