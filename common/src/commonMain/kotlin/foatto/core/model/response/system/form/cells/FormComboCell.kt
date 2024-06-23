package foatto.core.model.response.system.form.cells

import kotlinx.serialization.Serializable
import foatto.core.model.response.system.form.FormPinMode

@Serializable
class FormComboCell(
    override val name: String,
    override val caption: String,
    override val isEditable: Boolean,
    override val isAutoFocus: Boolean,
    override val formPinMode: FormPinMode = FormPinMode.AUTO,

    val value: String,
    val values: List<Pair<String, String>>,
    val asRadioButtons: Boolean = false,
) : FormBaseCell()