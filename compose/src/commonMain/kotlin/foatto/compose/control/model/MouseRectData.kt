package foatto.compose.control.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class MouseRectData(
    var isVisible: MutableState<Boolean> = mutableStateOf(false),
    var x1: MutableState<Float> = mutableStateOf(0.0f),
    var y1: MutableState<Float> = mutableStateOf(0.0f),
    var x2: MutableState<Float> = mutableStateOf(0.0f),
    var y2: MutableState<Float> = mutableStateOf(0.0f),
)
