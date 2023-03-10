package foatto.core_compose_web.control.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import foatto.core_compose_web.control.getColorRefreshButtonBack
import foatto.core_compose_web.control.getStyleToolbarButtonBorder
import foatto.core_compose_web.style.arrStyleCommonMargin
import foatto.core_compose_web.style.setBorder
import foatto.core_compose_web.style.setMargins
import foatto.core_compose_web.style.styleCommonButtonFontSize
import foatto.core_compose_web.style.styleIconButtonPadding
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Img

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

@Composable
fun getRefreshSubToolbar(
    refreshInterval: MutableState<Int>,
    onButtonClick: (interval: Int) -> Unit
) {
    getToolBarSpan {
        // 1s-interval shortly too
        listOf(0, /*1,*/ 5, 10, 30).forEach { interval ->
            if (interval == 0 || interval != refreshInterval.value) {
                Img(
                    src = "/web/images/ic_replay_${if (interval == 0) "" else "${interval}_"}black_48dp.png",
                    attrs = {
                        style {
                            backgroundColor(getColorRefreshButtonBack())
                            setBorder(getStyleToolbarButtonBorder())
                            fontSize(styleCommonButtonFontSize)
                            padding(styleIconButtonPadding)
                            setMargins(arrStyleCommonMargin)
                            cursor("pointer")
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
}

