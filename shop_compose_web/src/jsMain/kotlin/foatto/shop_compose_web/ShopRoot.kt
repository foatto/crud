package foatto.shop_compose_web

import foatto.core_compose_web.*
import foatto.core_compose_web.control.TableControl.Companion.hmTableIcon
import foatto.core_compose_web.control.*
import foatto.core_compose_web.style.*
import foatto.shop_core.app.*
import org.jetbrains.compose.web.css.hsl
import org.jetbrains.compose.web.css.hsla
import org.jetbrains.compose.web.renderComposable

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- фирменный бирюзовый           #00C0C0 = hsl(180,100%,37.6%)
private const val SHOP_FIRM_COLOR_1_H = 190 //180 - слишком яркий голубой
private const val SHOP_FIRM_COLOR_1_S = 100
private const val SHOP_FIRM_COLOR_1_L = 38

//--- фирменный красный             #FF0000 = hsl(0,100%,50%)
private const val SHOP_FIRM_COLOR_2_H = 350 // 0 - слишком ярко-красный
private const val SHOP_FIRM_COLOR_2_S = 50  // 100 - слишком ярко-красный
private const val SHOP_FIRM_COLOR_2_L = 50

private const val CALC_BACK = "hsla(0,0%,100%,0.9)"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

fun main() {

    val root = ShopRoot()
    root.init()

    renderComposable(rootElementId = "root") {
        root.getBody()
    }

    root.start()
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private class ShopRoot : Root(
    styleIsHiddenMenuBar = true,    //false, - для начала сделаем типовой дизайн
) {
    override fun init() {

        colorMainBack0 = hsl(SHOP_FIRM_COLOR_1_H, 50, 95)
        colorMainBack1 = hsl(SHOP_FIRM_COLOR_1_H, 50, 90)
        colorMainBack2 = hsl(SHOP_FIRM_COLOR_1_H, 50, 85)
        colorMainBack3 = hsl(SHOP_FIRM_COLOR_1_H, 50, 80)

        colorMainBorder = hsl(SHOP_FIRM_COLOR_2_H, SHOP_FIRM_COLOR_2_S, SHOP_FIRM_COLOR_2_L)

        //--- с фирменным красным получается кроваво-страшновато :)
        colorWaitBack = hsla(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 95, 0.75)
        colorWaitLoader0 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 80)
        colorWaitLoader1 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 85)
        colorWaitLoader2 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 90)
        colorWaitLoader3 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 95)

        colorDialogBack = hsla(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, SHOP_FIRM_COLOR_1_L, 0.75)

        //--- менять здесь
        //styleDarkIcon = true
        //styleIconSize = 36

        //--- marked item adding
        hmTableIcon[ICON_NAME_ADD_MARKED_ITEM] = "/web/images/ic_line_weight_${getStyleIconNameSuffix()}dp.png"
        hmTableIcon[ICON_NAME_FISCAL] = "/web/images/ic_theaters_${getStyleIconNameSuffix()}dp.png"
        hmTableIcon[ICON_NAME_CALC] = "/web/images/ic_shopping_cart_${getStyleIconNameSuffix()}dp.png"

        colorTableGroupBack0 = hsl(SHOP_FIRM_COLOR_2_H, 60, 90)
        colorTableGroupBack1 = hsl(SHOP_FIRM_COLOR_2_H, 60, 95)

        colorTableRowBack1 = colorMainBack0

        super.init()
    }
}

/*
    override fun addBeforeMounted() {
        super.addBeforeMounted()

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
                               id="cash_input"
                               v-model="docCash"
                               v-bind:size="10"
                               v-bind:style="style_calc_cash_input"
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

                    Vue.nextTick {
                        val element = document.getElementById("cash_input")
                        if (element is HTMLElement) {
                            element.focus()
                        }
                    }
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
            "docRestColor" to COLOR_MAIN_TEXT,

            "calcFiscalUrl" to null,
            "calcPrintUrl" to null,

            "isCalcShow" to false,

            "style_calc_container" to json(
                "position" to "fixed",
                "top" to 0,
                "left" to 0,
                "width" to "100%",
                "height" to "100%",
                "z-index" to Z_INDEX_ACTION_CONTAINER,
                "background" to CALC_BACK,
                "display" to "grid",
                "grid-template-rows" to "1fr auto 1fr",
                "grid-template-columns" to "1fr auto 1fr"
            ),
            "style_calc_top_expander" to json(
                "grid-area" to "1 / 2 / 2 / 3"
            ),
            "style_calc_body" to json(
                "grid-area" to "2 / 2 / 3 / 3",
                "z-index" to Z_INDEX_ACTION_BODY,
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
                "background" to COLOR_MAIN_BACK_0,
                "border" to "1px solid ${colorMainBorder()}",
                "border-radius" to styleInputBorderRadius,
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
} */