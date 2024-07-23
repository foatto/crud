package foatto.compose.control.model.table

import foatto.core.model.AppAction
import foatto.core.model.response.ServerActionButton

class ServerActionButtonClient(
    val name: String,
    val tooltip: String? = null,
    val action: AppAction,
    val inNewTab: Boolean,
    val isForWideScreenOnly: Boolean,
) {
    companion object {

        fun readFromServerActionButton(sab: ServerActionButton) =
            ServerActionButtonClient(
                name = sab.icon ?: sab.text ?: "(не заданы иконка и текст)",
                tooltip = sab.tooltip,
                action = sab.action,
                inNewTab = sab.inNewTab,
                isForWideScreenOnly = sab.isForWideScreenOnly,
            )
    }
}

