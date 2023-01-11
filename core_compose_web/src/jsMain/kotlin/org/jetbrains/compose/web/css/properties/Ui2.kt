package org.jetbrains.compose.web.css.properties

import org.jetbrains.compose.web.css.StyleScope

fun StyleScope.appearance(value: String) {
    property("appearance", value)
}

fun StyleScope.userSelect(value: String) {
    property("user-select", value)
}