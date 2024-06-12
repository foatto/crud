package foatto.core.model.response.system.table

import kotlinx.serialization.Serializable

@Serializable
class ClientActionButton(
    val icon: String?,
    val text: String?,
    val tooltip: String?,
    val action: String,
    val alParam: List<Pair<String, String>>,
    val isForWideScreenOnly: Boolean,
)
