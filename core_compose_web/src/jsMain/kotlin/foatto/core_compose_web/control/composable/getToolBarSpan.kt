package foatto.core_compose_web.control.composable

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.HTMLSpanElement

@Composable
fun getToolBarSpan(content: ContentBuilder<HTMLSpanElement>) {
    Span(
        attrs = {
            style {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Row)
                flexWrap(FlexWrap.Nowrap)
                justifyContent(JustifyContent.Center)
                alignItems(AlignItems.Center)
            }
        }
    ) {
        content()
    }
}
