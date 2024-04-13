package foatto.core_compose.controls.composables

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import foatto.core_compose.composables.ImageOrTextFromName

@Composable
fun ToolBarIconButton(
    isVisible: Boolean = true,
    isEnabled: Boolean = true,
    name: String,
    tooltip: String? = null,
    onClick: () -> Unit,
) {
    if (isVisible) {
        IconButton(
            enabled = isEnabled,
            onClick = {
                onClick()
            }
        ) {
            ImageOrTextFromName(name, tooltip)
//                        fontSize(styleCommonButtonFontSize)
//                        padding(styleIconButtonPadding)
//                        setMargins(arrStyleCommonMargin)
//                        if (isEnabled) {
//                            backgroundColor(getColorToolbarButtonBack())
//                            setBorder(getStyleToolbarButtonBorder())
//                            cursor("pointer")
//                        }

//                                backgroundColor(getColorButtonBack())
//                                setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
//                                fontSize(styleCommonButtonFontSize)
//                                padding(styleTextButtonPadding)
//                                setMargins(arrStyleCommonMargin)
//                                cursor("pointer")
        }
    }
}

