package foatto.compose.control.model.map

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

internal class DistancerTextData(
    val pos: MutableState<Offset> = mutableStateOf(Offset.Unspecified),
    val text: MutableState<String> = mutableStateOf(""),
)