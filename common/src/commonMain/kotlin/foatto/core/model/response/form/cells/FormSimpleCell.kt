package foatto.core.model.response.form.cells

import foatto.core.model.AppAction
import foatto.core.model.response.form.FormCellCaption
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.FormPinMode
import kotlinx.serialization.Serializable

@Serializable
class FormSimpleCell(
    override val name: String,
    override val caption: String,
    override val minWidth: Int = 0,
    override val isEditable: Boolean,
    override val isAutoFocus: Boolean = false,
    override val pinMode: FormPinMode = FormPinMode.AUTO,
    override val visibility: FormCellVisibility? = null,
    override val captions: FormCellCaption? = null,

    val value: String,
    val columns: Int = 40,
    val rows: Int = 1,
    val isPassword: Boolean = false,
    val selectorAction: AppAction? = null,
) : FormBaseCell()
