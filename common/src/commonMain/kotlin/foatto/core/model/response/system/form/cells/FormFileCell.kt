package foatto.core.model.response.system.form.cells

import kotlinx.serialization.Serializable
import foatto.core.model.response.system.form.FormPinMode

@Serializable
class FormFileCell(
    override val name: String,
    override val caption: String,
    override val isEditable: Boolean,
    override val isAutoFocus: Boolean,
    override val formPinMode: FormPinMode = FormPinMode.AUTO,

    val fileId: Long?,
    val files: List<FormFileData>
) : FormBaseCell()