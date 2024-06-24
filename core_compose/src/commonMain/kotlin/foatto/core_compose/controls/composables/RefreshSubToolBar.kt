package foatto.core_compose.controls.composables

import androidx.compose.runtime.Composable
import foatto.core_compose.getStyleToolbarIconNameSuffix

@Composable
fun RefreshSubToolBar(
    isWideScreen: Boolean,
    refreshInterval: Int,
    onButtonClick: (interval: Int) -> Unit,
) {
    ToolBarBlock {
        // 1s-interval shortly too for all devices
        // 5s-interval shortly too for mobile devices
        if (!isWideScreen) {
            listOf(0, /*1, 5,*/ 10, 30)
        } else {
            listOf(0, /*1,*/ 5, 10, 30)
        }.forEach { interval ->
            ToolBarIconButton(
                isEnabled = (interval == 0 || interval != refreshInterval),
                name = "/web/images/ic_replay_${if (interval == 0) "" else "${interval}_"}${getStyleToolbarIconNameSuffix()}.png",
                tooltip = when (interval) {
                    0 -> "Обновить сейчас"
                    1 -> "Обновлять каждую секунду"
                    else -> "Обновлять каждые $interval сек"
                },
            ) {
                onButtonClick(interval)
            }
        }

    }
}
