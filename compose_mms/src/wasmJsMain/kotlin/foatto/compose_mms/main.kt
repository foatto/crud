import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import foatto.compose_mms.MMSRoot

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = MMSRoot()
    CanvasBasedWindow(canvasElementId = "root") {
        root.Content()
    }
    root.start()
}