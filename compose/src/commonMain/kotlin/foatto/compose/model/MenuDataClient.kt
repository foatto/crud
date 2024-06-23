package foatto.compose.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.model.AppAction

class MenuDataClient(
    val caption: String,
    val action: AppAction? = null,
    val alSubMenu: List<MenuDataClient>? = null,
    val inNewTab: Boolean = false,
    var isExpanded: MutableState<Boolean> = mutableStateOf(false),
    val isHover: MutableState<Boolean> = mutableStateOf(false),
)