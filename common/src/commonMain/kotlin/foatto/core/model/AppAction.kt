package foatto.core.model

import foatto.core.ActionType
import kotlinx.serialization.Serializable

@Serializable
data class AppAction(
    val type: String,

    val module: String? = null,
    val id: Long? = null,

    val parentModule: String? = null,
    val parentId: Long? = null,

    val prevAction: AppAction? = null,

    val isSelectorMode: Boolean = false,
    val selectorData: Map<String, String> = emptyMap(),
    val selectorPath: Map<String, String> = emptyMap(),

    val sortName: String? = null,
    val isSortAsc: Boolean = true,

    val url: String? = null,
    val isAutoClose: Boolean = false,
)

fun emptyAction() = AppAction(type = ActionType.NOTHING)
