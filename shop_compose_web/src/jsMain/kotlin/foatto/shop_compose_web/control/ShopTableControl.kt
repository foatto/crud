package foatto.shop_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.link.CustomRequest
import foatto.core.link.TableResponse
import foatto.core.util.getSplittedDouble
import foatto.core.util.normalizeForDouble
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.control.TableControl
import foatto.core_compose_web.control.getColorToolbarButtonBack
import foatto.core_compose_web.control.getStyleToolbarButtonBorder
import foatto.core_compose_web.link.invokeCustom
import foatto.core_compose_web.style.*
import foatto.shop_core.app.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.size
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private val CALC_BACK = hsla(0, 0, 100, 0.9)
private val COLOR_DARK_RED: CSSColorValue = hsl(0, 50, 50)
private val COLOR_DARK_GREEN: CSSColorValue = hsl(120, 100, 25)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class ShopTableControl(
    root: Root,
    appControl: AppControl,
    appParam: String,
    tableResponse: TableResponse,
    tabId: Int,
) : TableControl(
    root,
    appControl,
    appParam,
    tableResponse,
    tabId,
) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val isCalcShow = mutableStateOf(false)

    private val docCost = mutableStateOf("-1.0")
    private val docTerminal = mutableStateOf("")
    private val docSberbank = mutableStateOf("")
    private val docSertificat = mutableStateOf("")
    private val docCash = mutableStateOf("")
    private val docRest = mutableStateOf("")

    private val calcFiscalUrl = mutableStateOf("")
    private val calcPrintUrl = mutableStateOf("")

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var docId = "0"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getAdditionalBody() {
        super.getAdditionalBody()

        if (isCalcShow.value) {
            Div(
                attrs = {
                    style {
                        position(Position.Fixed)
                        top(0.px)
                        left(0.px)
                        width(100.percent)
                        height(100.percent)
                        zIndex(Z_INDEX_ACTION_CONTAINER)
                        backgroundColor(CALC_BACK)
                        display(DisplayStyle.Grid)
                        gridTemplateRows("1fr auto 1fr")
                        gridTemplateColumns("1fr auto 1fr")
                    }
                }
            ) {
                Div(
                    attrs = {
                        style {
                            gridArea("1", "2", "2", "3")
                        }
                    }
                ) {
                    Br()
                }
                Div(
                    attrs = {
                        style {
                            gridArea("2", "2", "3", "3")
                            zIndex(Z_INDEX_ACTION_BODY)
                        }
                    }
                ) {
                    Div(
                        attrs = {
                            style {
                                fontSize(COMMON_FONT_SIZE * 2)
                            }
                        }
                    ) {
                        Text("Стоимость:")
                        getPseudoNbsp(6)
                        Text(docCost.value)
                    }
                    Br()
                    Br()
                    getFullPayVariant(
                        docVariable = docTerminal,
                        docLabel = "Терминал:",
                        afterLabelGap = 7,
                        docTitle = "Всё оплачено через терминал",
                        docButton = "Всё через терминал",
                    ) {
                        doAllOverTerminal()
                    }
                    Br()
                    getFullPayVariant(
                        docVariable = docSberbank,
                        docLabel = "Сбербанк:",
                        afterLabelGap = 7,
                        docTitle = "Всё оплачено через сбербанк",
                        docButton = "Всё через сбербанк",
                    ) {
                        doAllOverSberbank()
                    }
                    Br()
                    getFullPayVariant(
                        docVariable = docSertificat,
                        docLabel = "Сертификат:",
                        afterLabelGap = 1,
                    )
                    Br()
                    getFullPayVariant(
                        docVariable = docCash,
                        docLabel = "Наличные:",
                        afterLabelGap = 6,
                        focusId = "cash_input",
                        docTitle = "Посчитать сдачу",
                        docButton = "Посчитать сдачу",
                        isBoldButton = true,
                    ) {
                        doCalcCash()
                    }
                    Br()
                    Br()
                    Div(
                        attrs = {
                            style {
                                fontSize(COMMON_FONT_SIZE * 2)
                            }
                        }
                    ) {
                        Text("Сдача:")
                        getPseudoNbsp(12)
                        Span(
                            attrs = {
                                style {
                                    width(22.cssRem)    // 21 хватает, но сделаем запас
                                    padding(styleCommonEditorPadding)
                                    color(
                                        docRest.value.normalizeForDouble().toDoubleOrNull()?.let { value ->
                                            if (value < 0.0) {
                                                COLOR_DARK_RED
                                            } else {
                                                COLOR_DARK_GREEN
                                            }
                                        } ?: COLOR_DARK_GREEN
                                    )
                                    fontSize(COMMON_FONT_SIZE * 3)
                                    fontWeight("bold")
                                }
                            }
                        ) {
                            Text(docRest.value)
                        }
                    }
                    Br()
                    Br()
                    Div {
                        if (calcFiscalUrl.value.isNotEmpty()) {
                            Button(
                                attrs = {
                                    style {
                                        width(16.cssRem)    // 15 хватает, но сделаем запас
                                        fontSize(COMMON_FONT_SIZE * 2)
                                        padding(styleCommonEditorPadding)
                                    }
                                    title("Кассовый чек")
                                    onClick {
                                        doCalcFiscal()
                                    }
                                }
                            ) {
                                Text("Кассовый чек")
                            }
                        }
                        getPseudoNbsp(42)
                        if (calcPrintUrl.value.isNotEmpty()) {
                            Button(
                                attrs = {
                                    style {
                                        width(16.cssRem)    // 15 хватает, но сделаем запас
                                        fontSize(COMMON_FONT_SIZE * 2)
                                        padding(styleCommonEditorPadding)
                                    }
                                    title("Товарный чек")
                                    onClick {
                                        doCalcPrint()
                                    }
                                }
                            ) {
                                Text("Товарный чек")
                            }
                        }
                        getPseudoNbsp(42)
                        Button(
                            attrs = {
                                style {
                                    backgroundColor(getColorToolbarButtonBack())
                                    setBorder(getStyleToolbarButtonBorder())
                                    fontSize(styleCommonButtonFontSize)
                                    padding(styleIconButtonPadding)
                                    setMargins(arrStyleCommonMargin)
                                    cursor("pointer")
                                }
                                title("Закрыть чек")
                                onClick {
                                    doCalcClose()
                                }
                            }
                        ) {
                            Img(src = "/web/images/ic_close_black_48dp.png")
                        }
                    }
                }
                Div(
                    attrs = {
                        style {
                            gridArea("3", "2", "4", "3")
                        }
                    }
                ) {
                    Br()
                }
            }
        }
    }

    @Composable
    private fun getFullPayVariant(
        docVariable: MutableState<String>,
        docLabel: String,
        afterLabelGap: Int,
        focusId: String? = null,
        docTitle: String? = null,
        docButton: String? = null,
        isBoldButton: Boolean = false,
        docAction: (() -> Unit)? = null,
    ) {
        Div(
            attrs = {
                style {
                    fontSize(COMMON_FONT_SIZE * 2)
                }
            }
        ) {
            Text(docLabel)
            getPseudoNbsp(afterLabelGap)
            Input(InputType.Text) {
                style {
                    backgroundColor(COLOR_MAIN_BACK_0)
                    setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
                    padding(styleCommonEditorPadding)
                    fontSize(COMMON_FONT_SIZE * 2)
                }
                focusId?.let {
                    id(focusId)
                }
                size(10)
                value(docVariable.value)
                onInput { event ->
                    docVariable.value = event.value
                }
                onKeyUp { event ->
                    when (event.key) {
                        "Enter" -> doCalcCash()
                        "Escape" -> doCalcClose()
                    }
                }
            }
            getPseudoNbsp(1)
            if (docTitle != null && docButton != null && docAction != null) {
                Button(
                    attrs = {
                        style {
                            width(22.cssRem)    // 21 хватает, но сделаем запас
                            if (isBoldButton) {
                                fontWeight("bold")
                            }
                            padding(styleCommonEditorPadding)
                            fontSize(COMMON_FONT_SIZE * 2)
                        }
                        title(docTitle)
                        onClick {
                            docAction()
                        }
                    }
                ) {
                    Text(docButton)
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        super.start()

        tableClientActionFun = { action: String, alParam: List<Pair<String, String>>, tableControl: TableControl ->
            when (action) {
                ACTION_CASH_CALCULATOR -> {
                    docId = alParam.find { (first, _) -> first == PARAM_DOC_ID }?.second!!

                    val docCostD = alParam.find { (first, _) -> first == PARAM_DOC_COST }?.second?.toDoubleOrNull() ?: -1.0
                    docCost.value = getSplittedDouble(docCostD, 2, true, ',')

                    calcFiscalUrl.value = alParam.find { (first, _) -> first == PARAM_FISCAL_URL }?.second!!
                    calcPrintUrl.value = alParam.find { (first, _) -> first == PARAM_PRINT_URL }?.second!!

                    isCalcShow.value = true

                    window.setTimeout({
                        val element = document.getElementById("cash_input")
                        if (element is HTMLElement) {
                            element.focus()
                        }
                    }, 1000)
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun doAllOverTerminal() {
        docTerminal.value = docCost.value
        docSberbank.value = ""
        docSertificat.value = ""
        docCash.value = ""
        docRest.value = ""
    }

    private fun doAllOverSberbank() {
        docTerminal.value = ""
        docSberbank.value = docCost.value
        docSertificat.value = ""
        docCash.value = ""
        docRest.value = ""
    }

    private fun doCalcCash() {
        val docCostD = docCost.value.normalizeForDouble().toDoubleOrNull() ?: 0.0
        val docTerminalD = docTerminal.value.normalizeForDouble().toDoubleOrNull() ?: 0.0
        val docSberbankD = docSberbank.value.normalizeForDouble().toDoubleOrNull() ?: 0.0
        val docSertificatD = docSertificat.value.normalizeForDouble().toDoubleOrNull() ?: 0.0
        val docCashD = docCash.value.normalizeForDouble().toDoubleOrNull() ?: 0.0

        val docRestD = docTerminalD + docSberbankD + docSertificatD + docCashD - docCostD
        docRest.value = getSplittedDouble(docRestD, 2, true, ',')
    }

    private fun doCalcFiscal() {
        doSaveDocPaymentRequest()

        call(calcFiscalUrl.value, true)
        isCalcShow.value = false
    }

    private fun doCalcPrint() {
        doSaveDocPaymentRequest()

        call(calcPrintUrl.value, true)
        isCalcShow.value = false
    }

    private fun doCalcClose() {
        doSaveDocPaymentRequest()

        isCalcShow.value = false
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun doSaveDocPaymentRequest() {
        val docIdStr = docId
        val docTerminalStr = docTerminal.value.normalizeForDouble().ifBlank { "0" }
        val docSberbankStr = docSberbank.value.normalizeForDouble().ifBlank { "0" }
        val docSertificatStr = docSertificat.value.normalizeForDouble().ifBlank { "0" }
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

}
