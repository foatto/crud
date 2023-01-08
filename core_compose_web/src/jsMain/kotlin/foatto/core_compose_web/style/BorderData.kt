package foatto.core_compose_web.style

import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.px

class BorderData(
    val color: CSSColorValue,
    val style: LineStyle = LineStyle.Solid,
    val width: CSSSize = 1.px,
    val radius: CSSSize,
)