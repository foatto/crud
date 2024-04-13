package foatto.core_compose.controls.models.table

class ServerActionButtonClient(
    val name: String,
    val tooltip: String? = null,
    val action: String,
    val inNewWindow: Boolean,
    val isForWideScreenOnly: Boolean,
)
