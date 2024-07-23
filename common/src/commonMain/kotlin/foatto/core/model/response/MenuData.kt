package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class MenuData(
    val caption: String,
    val action: AppAction? = null,
    val alSubMenu: List<MenuData>? = null,
)
