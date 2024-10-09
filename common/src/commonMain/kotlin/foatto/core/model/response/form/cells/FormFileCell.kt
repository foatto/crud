package foatto.core.model.response.form.cells

import foatto.core.model.response.form.FormCellCaption
import foatto.core.model.response.form.FormCellVisibility
import kotlinx.serialization.Serializable
import foatto.core.model.response.form.FormPinMode

@Serializable
class FormFileCell(
    override val name: String,
    override val caption: String,
    override val minWidth: Int = 0,
    override val isEditable: Boolean,
    override val isAutoFocus: Boolean = false,
    override val pinMode: FormPinMode = FormPinMode.AUTO,
    override val visibility: FormCellVisibility? = null,
    override val captions: FormCellCaption? = null,

    val fileId: Int?,
    val files: List<FormFileData>,
    val maxFileCount: Int = Int.MAX_VALUE,
) : FormBaseCell()