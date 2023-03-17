package foatto.core_compose.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class MenuDataClient(
    val url: String,
    val text: String,
    val arrSubMenu: Array<MenuDataClient>? = null,
    val inNewWindow: Boolean = false,
    val isHover: MutableState<Boolean> = mutableStateOf(false),
)