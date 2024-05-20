package foatto.core_compose.controls.models.xy

import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.core.link.XyServerActionButton

class XyServerActionButtonData(
    val id: Int,
    val icon: String,
    val url: String,
    val caption: String,
    val tooltip: String,
    val isForWideScreenOnly: Boolean,
) {

    companion object {

        fun readXyServerActionButton(
            alServerActionButton: List<XyServerActionButton>,
            alXyServerButton: SnapshotStateList<XyServerActionButtonData>,
        ) {
            var serverButtonId = 0
            alXyServerButton.clear()
            for (sab in alServerActionButton) {
                alXyServerButton.add(
                    XyServerActionButtonData(
                        id = serverButtonId++,
                        icon = sab.icon,
                        url = sab.url,
                        caption = sab.caption,
                        tooltip = sab.tooltip,
                        isForWideScreenOnly = sab.isForWideScreenOnly,
                    )
                )
            }
        }
    }
}