import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import foatto.ts_compose.TSRoot

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = TSRoot()
    CanvasBasedWindow(canvasElementId = "root") {
        root.Content()
    }
    root.start()
}