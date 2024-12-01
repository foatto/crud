package foatto.core.model.response.form

import kotlinx.serialization.Serializable

@Serializable
class FormButton(
    val actionType: String,
    val withNewData: Boolean,
    val name: String,
    val question: String? = null,
    val key: FormButtonKey? = null,
    val params: Map<String, String> = emptyMap(),
)
