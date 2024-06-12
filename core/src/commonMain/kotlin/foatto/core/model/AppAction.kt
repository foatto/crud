package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppAction(
    val type: ActionType,

    val module: AppModule? = null,
    val id: Long? = null,

    val parentModule: AppModule? = null,
    val parentId: Long? = null,

    val prevAction: AppAction? = null,

    val isSelectorMode: Boolean = false,
    val selectorData: Map<String,String> = emptyMap(),
    val selectorPath: Map<String,String> = emptyMap(),

    val url: String? = null,
    val isAutoClose: Boolean = false,
)

fun emptyAction() = AppAction(type = ActionType.NOTHING)
