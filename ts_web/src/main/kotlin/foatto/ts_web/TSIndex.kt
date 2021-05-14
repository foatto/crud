package foatto.ts_web

import foatto.core_web.*
import foatto.core_web.external.vue.Vue
import foatto.core_web.external.vue.VueComponentOptions
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.HTMLSpanElement
import kotlin.js.Json
import kotlin.js.json

@Suppress("UnsafeCastFromDynamic")
fun main() {

    window.onload = {
        val index = TSIndex()
        index.init()
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class TSIndex : Index() {

    init {
        //--- constantly showed menu bar
        styleIsHiddenMenuBar = localStorage.getItem(IS_HIDDEN_MENU_BAR)?.toBooleanStrictOrNull() ?: false
    }

    override fun addBeforeMounted() {
        super.addBeforeMounted()

        colorGroupBack0 = "#c0eeee"
        colorGroupBack1 = "#c0ffff"
    }
}
