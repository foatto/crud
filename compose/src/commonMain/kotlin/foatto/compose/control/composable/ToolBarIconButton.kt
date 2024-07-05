package foatto.compose.control.composable

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import foatto.compose.composable.ImageOrTextFromName
import foatto.compose.styleToolbarIconSize

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
            ImageOrTextFromName(name, styleToolbarIconSize)
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

