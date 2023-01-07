package org.jetbrains.compose.web.css.properties

import org.jetbrains.compose.web.css.StyleScope

fun StyleScope.zIndex(level: Int) {
    property("z-index", "$level")
}