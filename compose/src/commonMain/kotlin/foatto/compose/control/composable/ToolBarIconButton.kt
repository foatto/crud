package foatto.compose.control.composable

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import foatto.compose.composable.ImageOrTextFromNameControl
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
                IconButton(
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

