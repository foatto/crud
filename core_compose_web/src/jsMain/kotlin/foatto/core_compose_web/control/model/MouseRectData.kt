package foatto.core_compose_web.control.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class MouseRectData(
    val isVisible: MutableState<Boolean> = mutableStateOf(false),
    val x1: MutableState<Int> = mutableStateOf(0),
    val y1: MutableState<Int> = mutableStateOf(0),
    val x2: MutableState<Int> = mutableStateOf(0),
    val y2: MutableState<Int> = mutableStateOf(0),
)
