import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import foatto.compose.utils.applicationDispatcher
import foatto.compose_mms.MMSRoot
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, DelicateCoroutinesApi::class)
fun main() {
    val root = MMSRoot()
    CanvasBasedWindow(canvasElementId = "root") {
        root.Content()
    }
    GlobalScope.launch(applicationDispatcher) {
        root.start()
    }
}