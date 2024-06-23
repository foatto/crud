package foatto.core_compose.controls.models.table

import foatto.core.link.ServerActionButton

class ServerActionButtonClient(
    val name: String,
    val tooltip: String? = null,
    val action: String,
    val inNewWindow: Boolean,
    val isForWideScreenOnly: Boolean,
) {
    companion object {

        fun readFromServerActionButton(sab: ServerActionButton) =
            ServerActionButtonClient(
                name = sab.icon ?: sab.text ?: "(не заданы иконка и текст)",
                tooltip = sab.tooltip,
                action = sab.action,
                inNewWindow = sab.inNewWindow,
                isForWideScreenOnly = sab.isForWideScreenOnly,
            )
    }
}

