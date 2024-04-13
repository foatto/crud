package foatto.core_compose.utils

import kotlinx.browser.window

internal actual fun openFileByUrl(url: String) {
    window.open(url)
}