package foatto.core.model.response.form.cells

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class FormFileData(
    val id: Int,
    val ref: Long,
    val action: AppAction,
    val name: String,
)