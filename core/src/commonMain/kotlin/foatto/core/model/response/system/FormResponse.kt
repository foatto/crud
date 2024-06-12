package foatto.core.model.response.system

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable
import foatto.core.model.response.system.form.FormButton
import foatto.core.model.response.system.form.cells.FormBaseCell

@Serializable
class FormResponse(
    val id: Long?,
    val tabCaption: String,
    val alHeader: List<Pair<AppAction?, String>>,
    //--- number of columns for GRID-form (> 0)
    //--- or display mode for normal form (== 0 or -1)
    //--- (in the desktop version, the narrow display mode is ignored - it is displayed as normal, because "reaching" a narrow screen on a computer / laptop is problematic)
    val columnCount: Int,
    val alFormColumn: List<String>,
    val alFormCell: List<FormBaseCell>,
    val alFormButton: List<FormButton>,
    val prevAction: AppAction?,
)





