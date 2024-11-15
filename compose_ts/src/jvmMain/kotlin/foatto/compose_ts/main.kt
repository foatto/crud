package foatto.compose_ts

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import foatto.compose.utils.applicationDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    val root = TSRoot()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Техсмарт-Онлайн",
    ) {
        root.Content()
    }
    GlobalScope.launch(applicationDispatcher) {
        root.start()
    }
}
