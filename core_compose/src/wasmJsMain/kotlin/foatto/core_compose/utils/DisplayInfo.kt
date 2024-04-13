package foatto.core_compose.utils

import androidx.compose.runtime.Composable
import kotlinx.browser.window

@Composable
internal actual fun getScaledScreenWidth(): Int = if (window.outerWidth > 0) {
    window.outerWidth
} else {
    //--- на мобильных устройствах это показывает ширину с учётом devicePixelRatio,
    //--- причём на некоторых устройствах (особенно с iOS) глючит как outerWidth == 0, и тогда приходится использовать innerWidth
    window.innerWidth
}
