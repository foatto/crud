package foatto.shop_web

import foatto.core.util.getSplittedDouble
import foatto.core_web.*
import foatto.core_web.external.vue.that
import foatto.shop_core.app.ACTION_CASH_CALCULATOR
import foatto.shop_core.app.ICON_NAME_ADD_MARKED_ITEM
import foatto.shop_core.app.ICON_NAME_CALC
import foatto.shop_core.app.ICON_NAME_FISCAL
import foatto.shop_core.app.PARAM_DOC_COST
import foatto.shop_core.app.PARAM_FISCAL_URL
import foatto.shop_core.app.PARAM_PRINT_URL
import kotlinx.browser.window
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

        tableTemplateAdd = """
            <div v-if="isCalcShow"
                 v-bind:style="style_calc_container"
            >
                <div v-bind:style="style_calc_top_expander">
                    &nbsp;
                </div>
                <div v-bind:style="style_calc_body">
                    <div v-bind:style="style_calc_label">
                        Стоимость: ${"&nbsp;".repeat(2)} 
                        <span>
                            {{docCostStr}}
                        </span>
                    </div>
                                          
                    <div> &nbsp; </div>
                    
                    <div v-bind:style="style_calc_label">
                        Наличные: &nbsp;
                        <input type="text"
                               v-model="docCash"
                               v-bind:size="10"
                               v-bind:style="style_calc_cash_input"
                               v-bind:autofocus="true"
                               v-on:keyup.enter.exact="doCalcCash()"
                               v-on:keyup.esc.exact="doCalcClose()"
                        >
                    </div>
                    
                    <div> &nbsp; </div>
                    
                    <div v-bind:style="style_calc_label">
                        Сдача: ${"&nbsp;".repeat(8)}
                        <span v-bind:style="[ style_calc_text, { 'color': docRestColor } ]">
                            {{docRest}}
                        </span>
                    </div>
                    
                    <div> &nbsp; </div>
                    <div> &nbsp; </div>
                    
                    <div>
                        <button v-if="calcFiscalUrl"
                                v-on:click="doCalcFiscal()"
                                v-bind:style="[ style_icon_button, style_button_with_border ]"
                                title="Кассовый чек"
                        >
                            <img src="/web/images/ic_theaters_black_48dp.png">
                        </button>

                        ${"&nbsp;".repeat(30)}
                        
                        <button v-if="calcPrintUrl"
                                v-on:click="doCalcPrint()"
                                v-bind:style="[ style_icon_button, style_button_with_border ]"
                                title="Товарный чек"
                        >
                            <img src="/web/images/ic_print_black_48dp.png">
                        </button>

                        ${"&nbsp;".repeat(30)}
                        
                        <button v-on:click="doCalcClose()"
                                v-bind:style="[ style_text_button, style_button_with_border ]"
                                title="Закрыть"
                        >
                            <img src="/web/images/ic_close_black_48dp.png">
                        </button>
                    </div>
                </div>
                <div v-bind:style="style_calc_bottom_expander">
                    &nbsp;
                </div>
            </div>
        """

        tableClientActionFun = { action: String, params: Array<Pair<String, String>>, that: dynamic ->
            when(action) {
                ACTION_CASH_CALCULATOR -> {
                    that.calcFiscalUrl = params.find { pair -> pair.first == PARAM_FISCAL_URL }?.second
                    that.calcPrintUrl = params.find { pair -> pair.first == PARAM_PRINT_URL }?.second

                    val docCost = params.find { pair -> pair.first == PARAM_DOC_COST }?.second?.toDoubleOrNull() ?: -1.0
                    that.docCost = docCost
                    that.docCostStr = getSplittedDouble(docCost, 2, true, ',')
                    that.isCalcShow = true
                }
            }
        }

        tableMethodsAdd = json(
            "doCalcCash" to {
                val docCost = that().docCost.unsafeCast<Double>()
                val docCash = that().docCash.unsafeCast<String>().toDoubleOrNull() ?: 0.0
                val docRest = docCash - docCost
                that().docRest = getSplittedDouble(docRest, 2, true, ',')
                that().docRestColor = if(docRest < 0) "red" else "green"
            },
            "doCalcFiscal" to {
                val calcFiscalUrl = that().calcFiscalUrl.unsafeCast<String>()
                that().invoke(calcFiscalUrl, true)
                that().isCalcShow = false
            },
            "doCalcPrint" to {
                val calcPrintUrl = that().calcPrintUrl.unsafeCast<String>()
                that().invoke(calcPrintUrl, true)
                that().isCalcShow = false
            },
            "doCalcClose" to {
                that().isCalcShow = false
            }
        )

        tableDataAdd = json(
            "calcFiscalUrl" to null,
            "calcPrintUrl" to null,
            "docCost" to -1.0,
            "docCostStr" to "-1.0",
            "docCash" to "",
            "docRest" to "-",
            "docRestColor" to COLOR_TEXT,
            "isCalcShow" to false,
            "style_calc_container" to json(
                "position" to "fixed",
                "top" to 0,
                "left" to 0,
                "width" to "100%",
                "height" to "100%",
                "z-index" to "998",
                "background" to "rgba( $COLOR_WAIT, 0.9 )",
                "display" to "grid",
                "grid-template-rows" to "1fr auto 1fr",
                "grid-template-columns" to "1fr auto 1fr"
            ),
            "style_calc_top_expander" to json(
                "grid-area" to "1 / 2 / 2 / 3"
            ),
            "style_calc_body" to json(
                "grid-area" to "2 / 2 / 3 / 3",
                "z-index" to "999",
            ),
            "style_calc_bottom_expander" to json(
                "grid-area" to "3 / 2 / 4 / 3"
            ),

            "style_calc_label" to json(
                "font-size" to "${COMMON_FONT_SIZE*2}rem",
            ),
            "style_calc_text" to json(
                "font-weight" to "bold",
            ),
            "style_calc_cash_input" to json(
                "background" to COLOR_BACK,
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS_SMALL,
                "font-size" to "${COMMON_FONT_SIZE*2}rem",
                "padding" to styleCommonEditorPadding(),
            ),
        )
    }
}