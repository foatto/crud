package foatto.shop_web

import foatto.core_web.*
import foatto.core_web.external.vue.Vue
import foatto.core_web.external.vue.VueComponentOptions
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import foatto.shop_core.app.ICON_NAME_ADD_MARKED_ITEM
import foatto.shop_core.app.ICON_NAME_CALC
import foatto.shop_core.app.ICON_NAME_FISCAL
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.HTMLSpanElement
import kotlin.js.Json
import kotlin.js.json

@Suppress("UnsafeCastFromDynamic")
fun main() {

    window.onload = {
        val index = ShopIndex()
        index.init()
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class ShopIndex : Index() {

    override fun addBeforeMounted() {
        super.addBeforeMounted()

        //--- бледно-голубая группировка (по пожеланиям продавцов :)
        colorGroupBack0 = "#c0eeee"
        colorGroupBack1 = "#c0ffff"

        //--- marked item adding
        hmTableIcon[ICON_NAME_ADD_MARKED_ITEM] = "/web/images/ic_line_weight_black_48dp.png"
        hmTableIcon[ICON_NAME_FISCAL] = "/web/images/ic_theaters_black_48dp.png"
        hmTableIcon[ICON_NAME_CALC] = "/web/images/ic_shopping_cart_black_48dp.png"
    }
}