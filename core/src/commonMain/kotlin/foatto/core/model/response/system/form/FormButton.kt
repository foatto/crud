package foatto.core.model.response.system.form

import foatto.core.model.ActionType
import kotlinx.serialization.Serializable

@Serializable
class FormButton(
    val actionType: ActionType,
    val withNewData: Boolean,
    val caption: String,
    val iconName: String,
    val key: FormButtonKey? = null,
    val question: String? = null,
)
