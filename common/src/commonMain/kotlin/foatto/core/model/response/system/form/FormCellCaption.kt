package foatto.core.model.response.system.form

import kotlinx.serialization.Serializable

@Serializable
class FormCellCaption(
    val name: String,
    val captions: Map<String, Set<String>>,
)