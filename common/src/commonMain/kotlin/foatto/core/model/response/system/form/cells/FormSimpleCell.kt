package foatto.core.model.response.system.form.cells

import foatto.core.model.AppAction
import foatto.core.model.response.system.form.FormPinMode
import kotlinx.serialization.Serializable

@Serializable
class FormSimpleCell(
    override val name: String,
    override val caption: String,
    override val isEditable: Boolean,
    override val isAutoFocus: Boolean,
    override val formPinMode: FormPinMode = FormPinMode.AUTO,

    val value: String,
    val columns: Int = 40,
    val rows: Int = 1,
    val isPassword: Boolean = false,
    val selectorAction: AppAction? = null,
    val isAutoStartSelector: Boolean = false,
) : FormBaseCell()
