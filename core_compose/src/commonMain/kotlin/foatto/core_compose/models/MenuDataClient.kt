package foatto.core_compose.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class MenuDataClient(
    val url: String,
    val text: String,
    val alSubMenu: List<MenuDataClient>? = null,
    val inNewWindow: Boolean = false,
    val isHover: MutableState<Boolean> = mutableStateOf(false),
)