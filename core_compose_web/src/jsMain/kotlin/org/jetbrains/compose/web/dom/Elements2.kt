package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLDetailsElement
import org.w3c.dom.HTMLElement

private open class ElementBuilderImplementation<TElement : Element>(private val tagName: String) : ElementBuilder<TElement> {
    private val el: Element by lazy { document.createElement(tagName) }

    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement = el.cloneNode() as TElement
}

private val Details: ElementBuilder<HTMLDetailsElement> = ElementBuilderImplementation("details")
private val Summary: ElementBuilder<HTMLElement> = ElementBuilderImplementation("summary")

@Composable
fun Details(
    attrs: AttrBuilderContext<HTMLDetailsElement>? = null,
    content: ContentBuilder<HTMLDetailsElement>? = null
) {
    TagElement(
        elementBuilder = Details,
        applyAttrs = attrs,
        content = content
    )
}

@Composable
fun Summary(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) {
    TagElement(
        elementBuilder = Summary,
        applyAttrs = attrs,
        content = content
    )
}
