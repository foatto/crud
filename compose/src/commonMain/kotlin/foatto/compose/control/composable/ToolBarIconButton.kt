package foatto.compose.control.composable

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import foatto.compose.colorIconButton
import foatto.compose.composable.ImageOrTextFromNameControl
import foatto.compose.singleButtonShape
import foatto.compose.styleToolbarIconSize

@Composable
fun ToolBarIconButton(
    isVisible: Boolean = true,
    isEnabled: Boolean = true,
    name: String,
    onClick: () -> Unit,
) {
    if (isVisible) {
        ImageOrTextFromNameControl(
            name = name,
            iconSize = styleToolbarIconSize,
            imageButton = { func ->
                FilledIconButton(
                    shape = singleButtonShape,
                    colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                    enabled = isEnabled,
                    onClick = {
                        onClick()
                    }
                ) {
                    func()
                }
            },
            textButton = {},
        )
    }
}

