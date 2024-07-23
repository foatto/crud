package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.cells.FormBaseCell

@Serializable
class FormResponse(
    val id: Int?,
    val tabCaption: String,
    val alHeader: List<Pair<AppAction?, String>>,
    val alFormColumn: List<String>,
    val alFormCell: List<FormBaseCell>,
    val alFormButton: List<FormButton>,
    val prevAction: AppAction?,
)





