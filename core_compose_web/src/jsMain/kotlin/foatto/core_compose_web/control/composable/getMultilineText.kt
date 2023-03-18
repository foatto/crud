package foatto.core_compose_web.control.composable

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Text

@Composable
fun getMultilineText(text: String) {
    text.split('\n').forEachIndexed { index, s ->
        if (index > 0) {
            Br()
        }
        Text(s)
    }
}