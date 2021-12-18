package foatto.shop_web

import foatto.core.link.CustomRequest
import foatto.core.util.getSplittedDouble
import foatto.core_web.*
import foatto.core_web.external.vue.that
import foatto.shop_core.app.*
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
                        Стоимость: ${"&nbsp;".repeat(3)} 
                        <span>
                            {{docCost}}
                        </span>
                    </div>
                                          
                    <div> &nbsp; </div>
                    
                    <div v-bind:style="style_calc_label">
                        Терминал: ${"&nbsp;".repeat(3)}
                        <input type="text"
                               v-model="docTerminal"
                               v-bind:size="10"
                               v-bind:style="style_calc_cash_input"
                               v-on:keyup.enter.exact="doCalcCash()"
                               v-on:keyup.esc.exact="doCalcClose()"
                        >
                        <button v-on:click="doAllOverTerminal()"
                                v-bind:style="[ style_button_with_border , style_calc_text_button ]"
                                title="Всё оплачено через терминал"
                        >
                            Всё через терминал
                        </button>
                    </div>
                    
                    <div> &nbsp; </div>
                    
                    <div v-bind:style="style_calc_label">
                        Сбербанк: ${"&nbsp;".repeat(3)}
                        <input type="text"
                               v-model="docSberbank"
                               v-bind:size="10"
                               v-bind:style="style_calc_cash_input"
                               v-on:keyup.enter.exact="doCalcCash()"
                               v-on:keyup.esc.exact="doCalcClose()"
                        >
                        <button v-on:click="doAllOverSberbank()"
                                v-bind:style="[ style_button_with_border , style_calc_text_button ]"
                                title="Всё оплачено через сбербанк"
                        >
                            Всё через сбербанк
                        </button>
                    </div>
                    
                    <div> &nbsp; </div>
                    
                    <div v-bind:style="style_calc_label">
                        Сертификат: 
                        <input type="text"
                               v-model="docSertificat"
                               v-bind:size="10"
                               v-bind:style="style_calc_cash_input"
                               v-on:keyup.enter.exact="doCalcCash()"
                               v-on:keyup.esc.exact="doCalcClose()"
                        >
                    </div>
                    
                    <div> &nbsp; </div>
                    
                    <div v-bind:style="style_calc_label">
                        Наличные: ${"&nbsp;".repeat(2)}
                        <input type="text"
                               v-model="docCash"
                               v-bind:size="10"
                               v-bind:style="style_calc_cash_input"
                               v-bind:autofocus="true"
                               v-on:keyup.enter.exact="doCalcCash()"
                               v-on:keyup.esc.exact="doCalcClose()"
                        >
                        <button v-on:click="doCalcCash()"
                                v-bind:style="[ style_button_with_border , style_calc_text_button, { 'font-weight': 'bold' } ]"
                                title="Посчитать сдачу"                                                             
                        >
                            Посчитать сдачу
                        </button>
                    </div>
                    
                    <div> &nbsp; </div>
                    
                    <div v-bind:style="style_calc_label">
                        Сдача: ${"&nbsp;".repeat(10)}
                        <span v-bind:style="[ style_calc_text, { 'color': docRestColor } ]">
                            {{docRest}}
                        </span>
                    </div>
                    
                    <div> &nbsp; </div>
                    <div> &nbsp; </div>
                    
                    <div>
                        <button v-if="calcFiscalUrl"
                                v-on:click="doCalcFiscal()"
                                v-bind:style="[ style_button_with_border , style_do_text_button ]"
                                title="Кассовый чек"
                        >
                            <!-- <img src="/web/images/ic_theaters_black_48dp.png"> -->
                            Кассовый чек
                        </button>

                        ${"&nbsp;".repeat(26)} 
                        
                        <button v-if="calcPrintUrl"
                                v-on:click="doCalcPrint()"
                                v-bind:style="[ style_button_with_border , style_do_text_button ]"
                                title="Товарный чек"
                        >
                            <!-- <img src="/web/images/ic_print_black_48dp.png"> -->
                            Товарный чек
                        </button>

                        ${"&nbsp;".repeat(26)}
                        
                        <button v-on:click="doCalcClose()"
                                v-bind:style="[ style_icon_button, style_button_with_border ]"
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
            when (action) {
                ACTION_CASH_CALCULATOR -> {
                    that.docId = params.find { pair -> pair.first == PARAM_DOC_ID }?.second

                    val docCost = params.find { pair -> pair.first == PARAM_DOC_COST }?.second?.toDoubleOrNull() ?: -1.0
                    that.docCost = getSplittedDouble(docCost, 2, true, ',')

                    that.calcFiscalUrl = params.find { pair -> pair.first == PARAM_FISCAL_URL }?.second
                    that.calcPrintUrl = params.find { pair -> pair.first == PARAM_PRINT_URL }?.second

                    that.isCalcShow = true
                }
            }
        }

        tableMethodsAdd = json(
            "doAllOverTerminal" to {
                val docCostStr = that().docCost.unsafeCast<String>()
                that().docTerminal = docCostStr
                that().docSberbank = ""
                that().docSertificat = ""
                that().docCash = ""
                that().docRest = ""
            },
            "doAllOverSberbank" to {
                val docCostStr = that().docCost.unsafeCast<String>()
                that().docTerminal = ""
                that().docSberbank = docCostStr
                that().docSertificat = ""
                that().docCash = ""
                that().docRest = ""
            },
            "doCalcCash" to {
                val docCost = that().docCost.unsafeCast<String>().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                val docTerminal = that().docTerminal.unsafeCast<String>().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                val docSberbank = that().docSberbank.unsafeCast<String>().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                val docSertificat = that().docSertificat.unsafeCast<String>().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                val docCash = that().docCash.unsafeCast<String>().replace(',', '.').replace(" ", "").toDoubleOrNull() ?: 0.0
                
                val docRest = docTerminal + docSberbank + docSertificat + docCash - docCost
                that().docRest = getSplittedDouble(docRest, 2, true, ',')
                that().docRestColor = if (docRest < 0) {
                    "red"
                } else {
                    "green"
                }
            },
            "doCalcFiscal" to {
                doSaveDocPaymentRequest(that())

                val calcFiscalUrl = that().calcFiscalUrl.unsafeCast<String>()
                that().invoke(calcFiscalUrl, true)
                that().isCalcShow = false
            },
            "doCalcPrint" to {
                doSaveDocPaymentRequest(that())

                val calcPrintUrl = that().calcPrintUrl.unsafeCast<String>()
                that().invoke(calcPrintUrl, true)
                that().isCalcShow = false
            },
            "doCalcClose" to {
                doSaveDocPaymentRequest(that())

                that().isCalcShow = false
            }
        )

        tableDataAdd = json(
            "docId" to "0",
            "docCost" to "-1.0",

            "docTerminal" to "",
            "docSberbank" to "",
            "docSertificat" to "",
            "docCash" to "",

            "docRest" to "-",
            "docRestColor" to COLOR_TEXT,

            "calcFiscalUrl" to null,
            "calcPrintUrl" to null,

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
                "font-size" to "${COMMON_FONT_SIZE * 2}rem",
            ),
            "style_calc_text" to json(
                "font-weight" to "bold",
            ),
            "style_calc_cash_input" to json(
                "background" to COLOR_BACK,
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS_SMALL,
                "font-size" to "${COMMON_FONT_SIZE * 2}rem",
                "padding" to styleCommonEditorPadding(),
            ),
            "style_calc_text_button" to json(
                "width" to "22rem",     // 21 хватает, но сделаем запас
                "font-size" to "${COMMON_FONT_SIZE * 2}rem",
                "padding" to styleCommonEditorPadding(),
            ),
            "style_do_text_button" to json(
                "width" to "16rem",     // 15 хватает, но сделаем запас
                "font-size" to "${COMMON_FONT_SIZE * 2}rem",
                "padding" to styleCommonEditorPadding(),
            ),
        )
    }
}

private fun doSaveDocPaymentRequest(that: dynamic) {
    val docIdStr = that.docId.unsafeCast<String>()
    val docTerminalStr = that.docTerminal.unsafeCast<String>().replace(',', '.').replace(" ", "").ifBlank { "0" }
    val docSberbankStr = that.docSberbank.unsafeCast<String>().replace(',', '.').replace(" ", "").ifBlank { "0" }
    val docSertificatStr = that.docSertificat.unsafeCast<String>().replace(',', '.').replace(" ", "").ifBlank { "0" }
    invokeCustom(
        CustomRequest(
            command = CUSTOM_COMMAND_SAVE_DOC_PAYMENT,
            hmData = mapOf(
                PARAM_DOC_PAYMENT_ID to docIdStr,
                PARAM_DOC_PAYMENT_TERMIMAL to docTerminalStr,
                PARAM_DOC_PAYMENT_SBERBANK to docSberbankStr,
                PARAM_DOC_PAYMENT_SERTIFICATE to docSertificatStr,
            )
        )
    )
}