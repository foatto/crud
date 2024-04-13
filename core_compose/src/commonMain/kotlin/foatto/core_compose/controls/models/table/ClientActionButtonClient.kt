package foatto.core_compose.controls.models.table

class ClientActionButtonClient(
    val name: String,
    val tooltip: String? = null,
    val action: String,
    val params: List<Pair<String, String>>,
    val isForWideScreenOnly: Boolean,
)