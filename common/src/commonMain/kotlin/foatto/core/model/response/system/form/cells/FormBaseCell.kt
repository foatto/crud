package foatto.core.model.response.system.form.cells

import foatto.core.model.response.system.form.FormCellCaption
import foatto.core.model.response.system.form.FormCellVisibility
import kotlinx.serialization.Serializable
import foatto.core.model.response.system.form.FormPinMode

@Serializable
sealed class FormBaseCell {
    abstract val name: String
    abstract val caption: String
    abstract val isEditable: Boolean
    abstract val isAutoFocus: Boolean
    abstract val pinMode: FormPinMode
    abstract val visibility: FormCellVisibility?
    abstract val captions: FormCellCaption?
}
