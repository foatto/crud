package foatto.core.model.response.system.form.cells

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class FormFileData(
    val id: Int,
    val action: AppAction,
    val name: String,
)