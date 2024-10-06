package foatto.core.model.response.form.cells

import foatto.core.model.response.form.FormCellCaption
import foatto.core.model.response.form.FormCellVisibility
import kotlinx.serialization.Serializable
import foatto.core.model.response.form.FormPinMode

@Serializable
sealed class FormBaseCell {
    abstract val name: String
    abstract val caption: String
    abstract val minWidth: Int
    abstract val isEditable: Boolean
    abstract val isAutoFocus: Boolean
    abstract val pinMode: FormPinMode
    abstract val visibility: FormCellVisibility?
    abstract val captions: FormCellCaption?
}
