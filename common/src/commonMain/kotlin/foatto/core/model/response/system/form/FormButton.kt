package foatto.core.model.response.system.form

import kotlinx.serialization.Serializable

@Serializable
class FormButton(
    val actionType: String,
    val withNewData: Boolean,
    val caption: String,
    val iconName: String,
    val key: FormButtonKey? = null,
    val question: String? = null,
)
