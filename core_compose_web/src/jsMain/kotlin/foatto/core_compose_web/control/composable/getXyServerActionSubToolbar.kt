package foatto.core_compose_web.control.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.core_compose_web.control.getColorToolbarButtonBack
import foatto.core_compose_web.control.getStyleToolbarButtonBorder
import foatto.core_compose_web.control.model.XyServerActionButtonData
import foatto.core_compose_web.style.*
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text

@Composable
fun getXyServerActionSubToolbar(
    alXyServerButton: SnapshotStateList<XyServerActionButtonData>,
    invokeServerButton: (url: String) -> Unit
) {
    getToolBarSpan {
        for (serverButton in alXyServerButton) {
            if (!styleIsNarrowScreen || !serverButton.isForWideScreenOnly) {
                Button(
                    attrs = {
                        style {
                            backgroundColor(getColorToolbarButtonBack())
                            setBorder(getStyleToolbarButtonBorder())
                            if (serverButton.icon.isNotEmpty()) {
                                padding(styleIconButtonPadding)
                            } else {
                                setPaddings(arrStyleStateServerButtonTextPadding)
                            }
                            setMargins(arrStyleCommonMargin)
                            fontSize(styleCommonButtonFontSize)
                            fontWeight(styleStateServerButtonTextFontWeight)
                            cursor("pointer")
                        }
                        title(serverButton.tooltip)
                        onClick {
                            invokeServerButton(serverButton.url)
                        }
                    }
                ) {
                    if (serverButton.icon.isNotEmpty()) {
                        Img(src = serverButton.icon)
                    } else {
                        Text(serverButton.caption)
                    }
                }
            }
        }
    }
}