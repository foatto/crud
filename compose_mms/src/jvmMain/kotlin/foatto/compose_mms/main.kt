package foatto.compose_mms

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import foatto.compose.utils.applicationDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    val root = MMSRoot()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Система контроля технологического оборудования и транспорта \"Пульсар\"",
    ) {
        root.Content()
    }
    GlobalScope.launch(applicationDispatcher) {
        root.start()
    }
}
