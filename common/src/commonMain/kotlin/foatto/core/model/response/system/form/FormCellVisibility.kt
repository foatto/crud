package foatto.core.model.response.system.form

import kotlinx.serialization.Serializable

@Serializable
class FormCellVisibility(
    val name: String,
    val state: Boolean,
    val values: Set<String>,
)