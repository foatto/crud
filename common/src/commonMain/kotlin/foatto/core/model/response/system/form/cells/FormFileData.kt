package foatto.core.model.response.system.form.cells

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class FormFileData(
    val id: Long,
    val action: AppAction,
    val name: String,
)