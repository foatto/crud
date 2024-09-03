package foatto.core.model.response.form

import foatto.core.model.AppAction
import foatto.core.model.response.form.cells.FormBaseCell
import kotlinx.serialization.Serializable

@Serializable
class FormResponse(
    val id: Int?,
    val tabCaption: String,
    val header: String,
    val alFormColumn: List<String>,
    val alFormCell: List<FormBaseCell>,
    val alFormButton: List<FormButton>,
    val prevAction: AppAction?,
)
