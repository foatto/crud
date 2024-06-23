import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import foatto.compose_ts.TSRoot

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = TSRoot()
    CanvasBasedWindow(canvasElementId = "root") {
        root.Content()
    }
    root.start()
}