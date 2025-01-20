package foatto.compose.utils

import androidx.compose.runtime.Composable
import kotlinx.browser.window

//--- на мобильных устройствах это показывает ширину с учётом devicePixelRatio,
//--- причём на некоторых устройствах (особенно с iOS) глючит как innerWidth == 0,
//--- и тогда приходится использовать outerWidth
@Composable
internal actual fun getScaledWindowWidth(): Int = if (window.innerWidth > 0) {
    window.innerWidth
} else {
    window.outerWidth
}
