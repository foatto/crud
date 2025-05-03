package foatto.core.model

import foatto.core.ActionType
import kotlinx.serialization.Serializable

@Serializable
data class AppAction(
    val type: String,

    val module: String? = null,
    val id: Int? = null,

    val parentModule: String? = null,
    val parentId: Int? = null,

    val prevAction: AppAction? = null,

    val isSelectorMode: Boolean = false,
    val selectorData: Map<String, String> = emptyMap(),
    val selectorPath: Map<String, String> = emptyMap(),
    val selectorClear: Map<String, String> = emptyMap(),
    val selectorFunId: Long? = null,

    val sortName: String? = null,
    val isSortAsc: Boolean = true,

    val pageNo: Int = 0,

    val findText: String? = null,

    val url: String? = null,
    val isAutoClose: Boolean = false,

    val timeRangeType: Int = 0,
    val begTime: Int? = null,
    val endTime: Int? = null,

    val params: Map<String, String> = emptyMap(),
)

fun emptyAction(): AppAction = AppAction(type = ActionType.NOTHING)
