package foatto.core_compose_web.control.model

import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.core.link.ServerActionButton
import foatto.core_compose_web.control.TableControl

class XyServerActionButtonData(
    val id: Int,
    val caption: String,
    val tooltip: String,
    val icon: String,
    val url: String,
    val isForWideScreenOnly: Boolean,
) {

    companion object {

        fun readXyServerActionButton(
            alServerActionButton: List<ServerActionButton>,
            alXyServerButton: SnapshotStateList<XyServerActionButtonData>,
        ) {
            var serverButtonId = 0
            alXyServerButton.clear()
            for (sab in alServerActionButton) {
                val icon = TableControl.hmTableIcon[sab.icon] ?: ""
                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                val caption = if (!sab.icon.isNullOrBlank() && icon.isBlank()) {
                    sab.icon
                } else {
                    sab.text
                }
                alXyServerButton.add(
                    XyServerActionButtonData(
                        id = serverButtonId++,
                        caption = caption ?: "",
                        tooltip = sab.tooltip ?: "",
                        icon = icon,
                        url = sab.action,
                        isForWideScreenOnly = sab.isForWideScreenOnly,
                    )
                )
            }
        }
    }
}