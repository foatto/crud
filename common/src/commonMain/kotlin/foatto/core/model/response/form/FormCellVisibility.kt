package foatto.core.model.response.form

import kotlinx.serialization.Serializable

@Serializable
class FormCellVisibility(
    val name: String,
    val state: Boolean,
    val values: Set<String>,
)