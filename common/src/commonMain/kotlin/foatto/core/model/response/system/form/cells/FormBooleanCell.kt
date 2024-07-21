package foatto.core.model.response.system.form.cells

import foatto.core.model.response.system.form.FormCellCaption
import foatto.core.model.response.system.form.FormCellVisibility
import kotlinx.serialization.Serializable
import foatto.core.model.response.system.form.FormPinMode

@Serializable
class FormBooleanCell(
    override val name: String,
    override val caption: String,
    override val isEditable: Boolean,
    override val isAutoFocus: Boolean,
    override val pinMode: FormPinMode = FormPinMode.AUTO,
    override val visibility: FormCellVisibility? = null,
    override val captions: FormCellCaption? = null,

    val value: Boolean,
    val alSwitchText: List<String> = emptyList(),
) : FormBaseCell()
