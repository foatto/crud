package foatto.compose.control.model.form

internal class FormButtonData(
    val actionType: String,
    val withNewData: Boolean,
    val question: String?,
    val name: String,
    val tooltip: String,
)