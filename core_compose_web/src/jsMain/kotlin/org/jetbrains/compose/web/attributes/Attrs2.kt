package org.jetbrains.compose.web.attributes

import org.w3c.dom.HTMLImageElement

fun AttrsScope<HTMLImageElement>.disabled() =
    attr("disabled", "")
