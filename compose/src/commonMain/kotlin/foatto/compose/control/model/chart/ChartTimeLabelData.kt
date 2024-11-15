package foatto.compose.control.model.chart

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

internal class ChartTimeLabelData(
    val isVisible: MutableState<Boolean> = mutableStateOf(false),
    val x: MutableState<Float> = mutableFloatStateOf(0.0f),
    val text: MutableState<String> = mutableStateOf(""),
)