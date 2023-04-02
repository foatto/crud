package foatto.core_compose_web.control.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import foatto.core_compose_web.control.getColorRefreshButtonBack
import foatto.core_compose_web.control.getStyleToolbarButtonBorder
import foatto.core_compose_web.style.arrStyleCommonMargin
import foatto.core_compose_web.style.getStyleIconNameSuffix
import foatto.core_compose_web.style.setBorder
import foatto.core_compose_web.style.setMargins
import foatto.core_compose_web.style.styleCommonButtonFontSize
import foatto.core_compose_web.style.styleIconButtonPadding
import foatto.core_compose_web.style.styleIsNarrowScreen
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Img

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

@Composable
fun getRefreshSubToolbar(
    refreshInterval: MutableState<Int>,
    onButtonClick: (interval: Int) -> Unit
) {
    getToolBarSpan {
        // 1s-interval shortly too for all devices
        // 5s-interval shortly too for mobile devices
        if (styleIsNarrowScreen) {
            listOf(0, /*1, 5,*/ 10, 30)
        } else {
            listOf(0, /*1,*/ 5, 10, 30)
        }.forEach { interval ->
            val isEnabledButton = (interval == 0 || interval != refreshInterval.value)
            Img(
                src = "/web/images/ic_replay_${if (interval == 0) "" else "${interval}_"}${getStyleIconNameSuffix()}.png",
                attrs = {
                    style {
                        fontSize(styleCommonButtonFontSize)
                        padding(styleIconButtonPadding)
                        setMargins(arrStyleCommonMargin)
                        if (isEnabledButton) {
                            backgroundColor(getColorRefreshButtonBack())
                            setBorder(getStyleToolbarButtonBorder())
                            cursor("pointer")
                        }
                    }
                    if (!isEnabledButton) {
                        disabled()
                    }
                    title(
                        when (interval) {
                            0 -> "Обновить сейчас"
                            1 -> "Обновлять каждую секунду"
                            else -> "Обновлять каждые $interval сек"
                        }
                    )
                    onClick {
                        onButtonClick(interval)
                    }
                }
            )
        }
    }
}

