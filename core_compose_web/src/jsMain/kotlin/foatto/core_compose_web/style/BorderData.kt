package foatto.core_compose_web.style

import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.hsl
import org.jetbrains.compose.web.css.px

class BorderData(
    val color: CSSColorValue,
    val style: LineStyle = LineStyle.Solid,
    val width: CSSSize = 1.px,
    val radius: CSSSize,
)

val noBorder: BorderData = BorderData(
    color = hsl(0, 0, 0),
    style = LineStyle.None,
    width = 0.px,
    radius = 0.px
)

