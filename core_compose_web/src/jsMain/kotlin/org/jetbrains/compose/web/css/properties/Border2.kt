package org.jetbrains.compose.web.css.properties

import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.CSSNumeric
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.StyleScope

fun StyleScope.borderTop(
    width: CSSNumeric,
    lineStyle: LineStyle,
    color: CSSColorValue,
) {
    property("border-top", "$width $lineStyle $color")
}

fun StyleScope.borderRight(
    width: CSSNumeric,
    lineStyle: LineStyle,
    color: CSSColorValue,
) {
    property("border-right", "$width $lineStyle $color")
}

fun StyleScope.borderBottom(
    width: CSSNumeric,
    lineStyle: LineStyle,
    color: CSSColorValue,
) {
    property("border-bottom", "$width $lineStyle $color")
}

fun StyleScope.borderLeft(
    width: CSSNumeric,
    lineStyle: LineStyle,
    color: CSSColorValue,
) {
    property("border-left", "$width $lineStyle $color")
}

fun StyleScope.borderColor(
    top: CSSColorValue,
    right: CSSColorValue,
    bottom: CSSColorValue,
    left: CSSColorValue,
) {
    property("border-color", "$top $right $bottom $left")
}