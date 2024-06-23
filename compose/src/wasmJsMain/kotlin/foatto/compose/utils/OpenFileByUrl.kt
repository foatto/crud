package foatto.compose.utils

import kotlinx.browser.window

internal actual fun openFileByUrl(url: String) {
    window.open(url)
}