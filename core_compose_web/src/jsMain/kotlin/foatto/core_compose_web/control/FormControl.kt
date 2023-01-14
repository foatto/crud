package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import foatto.core.app.*
import foatto.core.link.AppRequest
import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.FormPinMode
import foatto.core.link.FormResponse
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.style.*
import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.cols
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.multiple
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.attributes.rows
import org.jetbrains.compose.web.attributes.size
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.appearance
import org.jetbrains.compose.web.css.properties.borderBottom
import org.jetbrains.compose.web.css.properties.borderTop
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement
import kotlin.math.max

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

var getColorFormBack: () -> CSSColorValue = { colorMainBack1 }
var styleFormLabelWeight: String = "normal"
var getColorFormButtonBack: () -> CSSColorValue = { getColorButtonBack() }
var getStyleFormButtonBorder: () -> BorderData = { BorderData(color = getColorButtonBorder(), radius = styleButtonBorderRadius) }
var getColorFormActionButtonSaveBack: () -> CSSColorValue = { getColorButtonBack() }
var getColorFormActionButtonOtherBack: () -> CSSColorValue = { getColorButtonBack() }
var getStyleFormActionButtonBorder: () -> BorderData = { BorderData(color = getColorButtonBorder(), radius = styleButtonBorderRadius) }

private fun getStyleFormEditBoxColumn(initSize: Int) = if (styleIsNarrowScreen) {
    initSize
} else if (initSize <= scaledScreenWidth / 19) {
    initSize
} else {
    scaledScreenWidth / 19
}

//--- ! не убирать, так удобнее выравнивать label на форме, чем каждому тексту прописывать уникальный стиль
private val styleFormRowPadding = CONTROL_LEFT_RIGHT_SIDE_PADDING
private val styleFormRowTopBottomPadding = 0.1.cssRem
private val styleFormLabelPadding = 0.6.cssRem
private val styleFormCheckboxAndRadioMargin = 0.5.cssRem
private val styleFileNameButtonPadding = 0.95.cssRem
private val styleFileNameButtonMargin = 0.1.cssRem

private val colorFormSwitchBackOn = COLOR_MAIN_BACK_0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class FormControl(
    private val root: Root,
    private val appControl: AppControl,
    private val formResponse: FormResponse,
    private val tabId: Int
) : iControl {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        private val hmFormIcon = mutableMapOf(
            ICON_NAME_ARCHIVE to "/web/images/ic_archive_black_48dp.png",
            ICON_NAME_DELETE to "/web/images/ic_delete_forever_black_48dp.png",
            ICON_NAME_EXIT to "/web/images/ic_exit_to_app_black_48dp.png",
            ICON_NAME_FILE to "/web/images/ic_attachment_black_48dp.png",
            ICON_NAME_GRAPHIC to "/web/images/ic_timeline_black_48dp.png",
            ICON_NAME_MAP to "/web/images/ic_language_black_48dp.png",
            ICON_NAME_PRINT to "/web/images/ic_print_black_48dp.png",
            ICON_NAME_SAVE to "/web/images/ic_save_black_48dp.png",
            ICON_NAME_STATE to "/web/images/ic_router_black_48dp.png",
            ICON_NAME_UNARCHIVE to "/web/images/ic_unarchive_black_48dp.png",
            ICON_NAME_VIDEO to "/web/images/ic_play_circle_outline_black_48dp.png"
        )
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

// mutable state data

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val alTitleData = mutableListOf<FormTitleData>()
    private val alGridData = mutableListOf<FormGridData>()

    private val hmFormCellVisible = mutableMapOf<Int, MutableList<FormCellVisibleInfo>>()
    private val hmFormCellCaption = mutableMapOf<Int, MutableList<FormCellCaptionInfo>>()

    private val alFormButton = mutableListOf<FormButtonData>()

    private var formSaveUrl = ""
    private var formExitUrl = ""

    private var addStyleGrid: StyleScope.() -> Unit = {}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        Div(
            attrs = {
                style {
                    flexGrow(1)
                    flexShrink(1)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    height(100.percent)
                }
            }
        ) {

            //--- Form Header

            Div(
                attrs = {
                    style {
                        flexGrow(0)
                        flexShrink(0)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        flexWrap(FlexWrap.Wrap)
                        justifyContent(JustifyContent.Center)
                        alignItems(AlignItems.Center)   // "baseline" ?
                        borderTop(
                            width = if (!styleIsNarrowScreen) 0.px else 1.px,
                            lineStyle = LineStyle.Solid,
                            color = colorMainBorder
                        )
                        borderBottom(width = 1.px, lineStyle = LineStyle.Solid, color = colorMainBorder)
                        padding(styleControlPadding)
                        backgroundColor(getColorFormBack())
                    }
                }
            ) {
                for (titleData in alTitleData) {
                    if (titleData.url.isNotEmpty()) {
                        Button(
                            attrs = {
                                style {
                                    backgroundColor(getColorButtonBack())
                                    setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                    fontSize(styleCommonButtonFontSize)
                                    padding(styleTextButtonPadding)
                                    setMargins(arrStyleCommonMargin)
                                    cursor("pointer")
                                }
                                onClick {
                                    call(titleData.url, false, null)
                                }
                            }
                        ) {
                            Text(titleData.text)
                        }
                    } else {
                        Span(
                            attrs = {
                                style {
                                    fontSize(styleControlTitleTextFontSize)
                                    setPaddings(arrStyleControlTitlePadding)
                                }
                            }
                        ) {
                            Text(titleData.text)
                        }
                    }
                }
            }

            //--- Form Body

            Div(
                attrs = {
                    style {
                        backgroundColor(getColorFormBack())
                        flexGrow(1)
                        flexShrink(1)
                        height(100.percent) //- необязательно ?
                        overflow("auto")
                        display(DisplayStyle.Grid)
                        addStyleGrid()
                    }
                }
            ) {
                for (gridData in alGridData) {
                    if (!gridData.isHidden) {
                        Div(
                            attrs = {
                                style {
                                    gridData.style(this)
                                    gridData.styleAdd(this)
                                }
                                if (!gridData.isVisible.value) {
                                    hidden()    //!!! точно ли подходящий способ???
                                }
                            }
                        ) {
                            when (gridData.cellType) {
                                FormCellTypeClient.LABEL -> {
                                    Span(
                                        attrs = {
                                            style {
                                                fontSize(styleControlTextFontSize)
                                                fontWeight(styleFormLabelWeight)
                                            }
                                        }
                                    ) {
                                        Text(gridData.text.value)
                                    }
                                }

                                FormCellTypeClient.STRING -> {
                                    Input(
                                        if (gridData.isPassword) {
                                            InputType.Password
                                        } else {
                                            InputType.Text
                                        }
                                    ) {
                                        style {
                                            backgroundColor(COLOR_MAIN_BACK_0)
                                            setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
                                            fontSize(styleControlTextFontSize)
                                            padding(styleCommonEditorPadding)
                                            setMargins(arrStyleCommonMargin)
                                        }
                                        id("'i_${tabId}_${gridData.id}")
                                        size(gridData.colCount)
                                        if (gridData.isReadOnly) {
                                            readOnly()
                                        }
                                        value(gridData.text.value)
                                        onInput { event ->
                                            gridData.text.value = event.value
                                        }
//                                        onFocusIn {
//                                            selectAllText($ { '$' } event )
//                                        }
//                                        onKeyUp { event ->
//                                            if (event.key == "Enter") {
//                                                doNextFocus(0)
//                                            }
//                           v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                           v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                           v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                           v-on:keyup.f4="closeTabById()"
//                                        }
                                    }
                                }

                                FormCellTypeClient.TEXT -> {
                                    TextArea {
                                        style {
                                            backgroundColor(COLOR_MAIN_BACK_0)
                                            setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
                                            fontSize(styleControlTextFontSize)
                                            padding(styleCommonEditorPadding)
                                            setMargins(arrStyleCommonMargin)
                                        }
                                        id("'i_${tabId}_${gridData.id}")
                                        rows(gridData.rowCount)
                                        cols(gridData.colCount)
                                        if (gridData.isReadOnly) {
                                            readOnly()
                                        }
                                        value(gridData.text.value)
                                        onInput { event ->
                                            gridData.text.value = event.value
                                        }
                                    }
//                              v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                              v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                              v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                              v-on:keyup.f4="closeTabById()"
                                }

                                FormCellTypeClient.CHECKBOX -> {
                                    Input(InputType.Checkbox) {
                                        style {
                                            appearance("none")
                                            width(styleCheckBoxWidth)
                                            height(styleCheckBoxHeight)
                                            setBorder(getStyleCheckBoxBorder())
                                            margin(styleFormCheckboxAndRadioMargin)
                                        }
                                        id("'i_${tabId}_${gridData.id}")
                                        if (gridData.isReadOnly) {
                                            readOnly()
                                        }
                                        checked(gridData.bool.value)
                                        onInput { event ->
                                            gridData.bool.value = event.value
                                        }
//                                        onKeyUp { event ->
//                                            if (event.key == "Enter") {
//                                                doNextFocus(2)
//                                            }
//                                        }
//                           v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                           v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                           v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                           v-on:keyup.f4="closeTabById()"
//                           v-on:change="gridData.itReadOnly ? null : doVisibleAndCaptionChange( gridData )"
                                    }
                                }

                                FormCellTypeClient.SWITCH -> {
                                    Button(
                                        attrs = {
                                            style {
                                                backgroundColor(
                                                    if (gridData.bool.value) {
                                                        colorMainBack1
                                                    } else {
                                                        colorFormSwitchBackOn
                                                    }
                                                )
                                                setBorder(color = colorMainBorder, radius = styleButtonBorderRadius)
                                                fontSize(styleCommonButtonFontSize)
                                                padding(styleFileNameButtonPadding)
                                                cursor("pointer")
                                            }
                                            id("'i_${tabId}_${gridData.id}_0")
                                            if (gridData.isReadOnly) {
                                                disabled()
                                            }
                                            title(gridData.alSwitchText[0])
//                            <button v-on:click="gridData.itReadOnly || !gridData.bool ? null : doVisibleAndCaptionChange( gridData )"
//                                    v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                                    v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                                    v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                                    v-on:keyup.f4="closeTabById()"
                                        }
                                    ) {
                                        Text(gridData.alSwitchText[0])
                                    }

                                    Button(
                                        attrs = {
                                            style {
                                                backgroundColor(
                                                    if (gridData.bool.value) {
                                                        colorFormSwitchBackOn
                                                    } else {
                                                        colorMainBack1
                                                    }
                                                )
                                                setBorder(color = colorMainBorder, radius = styleButtonBorderRadius)
                                                fontSize(styleCommonButtonFontSize)
                                                padding(styleFileNameButtonPadding)
                                                cursor("pointer")
                                            }
                                            id("'i_${tabId}_${gridData.id}_1")
                                            if (gridData.isReadOnly) {
                                                disabled()
                                            }
                                            title(gridData.alSwitchText[1])
//                            <button v-on:click="gridData.itReadOnly || gridData.bool ? null : doVisibleAndCaptionChange( gridData )"
//                                    v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                                    v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                                    v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                                    v-on:keyup.f4="closeTabById()"
                                        }
                                    ) {
                                        Text(gridData.alSwitchText[1])
                                    }
                                }

                                FormCellTypeClient.DATE, FormCellTypeClient.TIME, FormCellTypeClient.DATE_TIME -> {
                                    for (index in 0 until gridData.alDateTime.size) {
                                        Input(InputType.Text) {
                                            style {
                                                backgroundColor(COLOR_MAIN_BACK_0)
                                                setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
                                                fontSize(styleControlTextFontSize)
                                                padding(styleCommonEditorPadding)
                                                setMargins(arrStyleCommonMargin)
                                            }
                                            id("'i_${tabId}_${gridData.id}_${gridData.alSubId[index]}")
                                            size(
                                                if (index == 2 && gridData.cellType != FormCellTypeClient.TIME) {
                                                    2
                                                } else {
                                                    1
                                                }
                                            )
                                            if (gridData.isReadOnly) {
                                                readOnly()
                                            }
                                            value(gridData.alDateTime[index].value)
                                            onInput { event ->
                                                gridData.alDateTime[index].value = event.value
                                            }
//                                            onFocusIn {
//                                                selectAllText($ { '$' } event )
//                                            }
                                        }
                                    }
//                               v-on:keyup.enter.exact="doNextFocus( gridData.id, gridData.arrSubId[index] )"
//                               v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                               v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                               v-on:keyup.f4="closeTabById()"
                                }

                                FormCellTypeClient.COMBO -> {
                                    Select(
                                        attrs = {
                                            style {
                                                fontSize(styleControlTextFontSize)
                                                padding(styleCommonEditorPadding)
                                                setMargins(arrStyleCommonMargin)
                                            }
                                            id("'i_${tabId}_${gridData.id}")
                                            if (gridData.isReadOnly) {
                                                disabled()
                                            }
//                        <select v-model="gridData.combo"
//                                v-on:change="gridData.itReadOnly ? null : doVisibleAndCaptionChange( gridData )"
//                                v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                                v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                                v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                                v-on:keyup.f4="closeTabById()"
                                        }
                                    ) {
//                            <option v-for="comboData in gridData.arrComboData"
//                                    v-bind:value="comboData.value"
//                            >
//                                {{ comboData.text }}
//                            </option>
                                    }
                                }

                                FormCellTypeClient.RADIO -> {
                                    gridData.alComboData.forEachIndexed { index, comboData ->
                                        Input(InputType.Radio) {
                                            style {
//                                                transform {
//                        "transform" to styleControlRadioTransform(),
//                                                }
                                                margin(styleFormCheckboxAndRadioMargin)
                                            }
                                            id("'i_${tabId}_${gridData.id}_${gridData.alSubId[index]}")
                                            if (gridData.isReadOnly) {
                                                readOnly()
                                            }
//                                   v-model="gridData.combo"
//                                   v-bind:value="comboData.value"
//                                   v-on:change="gridData.itReadOnly ? null : doVisibleAndCaptionChange( gridData )"
//                                   v-on:keyup.enter.exact="doNextFocus( gridData.id, gridData.arrSubId[index] )"
//                                   v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                                   v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                                   v-on:keyup.f4="closeTabById()"
                                        }
                                        Span(
                                            attrs = {
                                                style {
                                                    fontSize(styleControlTextFontSize)
                                                }
                                            }
                                        ) {
                                            Text(comboData.text)
                                        }
                                        Br()
                                    }
                                }

                                FormCellTypeClient.FILE -> {
                                    for (fileData in gridData.alFileData) {
                                        Div(
                                            attrs = {
                                                style {
                                                    display(DisplayStyle.Flex)
                                                    flexDirection(FlexDirection.Row)
                                                    flexWrap(FlexWrap.Wrap)
                                                    justifyContent(JustifyContent.FlexStart)
                                                    alignItems(AlignItems.Center)
                                                }
                                            }
                                        ) {
                                            Button(
                                                attrs = {
                                                    style {
                                                        backgroundColor(getColorButtonBack())
                                                        setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                                        fontSize(styleCommonButtonFontSize)
                                                        padding(styleFileNameButtonPadding)
                                                        margin(styleFileNameButtonMargin)
                                                        cursor("pointer")
                                                    }
                                                    title("Показать файл")
                                                    if (fileData.id < 0) {
                                                        disabled()
                                                    }
                                                }
//                            <button v-on:click="fileData.id < 0 ? null : showFile( fileData.url )"
//                                    v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                                    v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                                    v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                                    v-on:keyup.f4="closeTabById()"
                                            ) {
                                                Text(fileData.text)
                                            }
                                            if (!gridData.isReadOnly) {
                                                Img(
                                                    src = "/web/images/ic_delete_forever_black_48dp.png",
                                                    attrs = {
                                                        style {
                                                            backgroundColor(getColorFormButtonBack())
                                                            setBorder(getStyleFormButtonBorder())
                                                            fontSize(styleCommonButtonFontSize)
                                                            padding(styleIconButtonPadding)
                                                            setMargins(arrStyleCommonMargin)
                                                            cursor("pointer")
                                                        }
                                                        title("Удалить файл")
//                                 v-on:click="deleteFile( gridData, fileData )"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Br()
                                    Input(InputType.File) {
                                        style {
                                            display(DisplayStyle.None)
                                        }
                                        id("fileInput")
                                        multiple()
//                               v-on:change="addFile( gridData, ${'$'}event )"
                                    }
                                    if (!gridData.isReadOnly) {
                                        Button(
                                            attrs = {
                                                style {
                                                    backgroundColor(getColorButtonBack())
                                                    setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                                    fontSize(styleCommonButtonFontSize)
                                                    padding(styleFileNameButtonPadding)
                                                    margin(styleFileNameButtonMargin)
                                                    cursor("pointer")
                                                }
                                                title("Добавить файл(ы)")
//                                v-on:click="addFileDialog('fileInput')"
//                                v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
//                                v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true, null ) : null"
//                                v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false, null ) : null"
//                                v-on:keyup.f4="closeTabById()"
                                            }
                                        ) {
                                            Text("Добавить файл(ы)")
                                        }
                                    }
                                }
                            }

                            if (gridData.selectorSetURL.isEmpty()) {
                                Img(
                                    src = "/web/images/ic_reply_black_48dp.png",
                                    attrs = {
                                        style {
                                            backgroundColor(getColorFormButtonBack())
                                            setBorder(getStyleFormButtonBorder())
                                            fontSize(styleCommonButtonFontSize)
                                            padding(styleIconButtonPadding)
                                            setMargins(arrStyleCommonMargin)
                                            cursor("pointer")
                                        }
                                        title("Выбрать из справочника")
                                        onClick {
                                            call(gridData.selectorSetURL, true, null)
                                        }
                                    }
                                )
                            }

                            if (gridData.selectorClearURL.isEmpty()) {
                                Img(
                                    src = "/web/images/ic_delete_forever_black_48dp.png",
                                    attrs = {
                                        style {
                                            backgroundColor(getColorFormButtonBack())
                                            setBorder(getStyleFormButtonBorder())
                                            fontSize(styleCommonButtonFontSize)
                                            padding(styleIconButtonPadding)
                                            setMargins(arrStyleCommonMargin)
                                            cursor("pointer")
                                        }
                                        title("Очистить выбор")
                                        onClick {
                                            call(gridData.selectorClearURL, true, null)
                                        }
                                    }
                                )
                            }

                            if (gridData.error.isNotEmpty()) {
                                Div(
                                    attrs = {
                                        style {
                                            color(Color.red)
                                            fontSize(styleControlTextFontSize)
                                            fontWeight("bold")
                                            setMargins(arrStyleCommonMargin)
                                        }
                                    }
                                ) {
                                    Text(gridData.error)
                                }
                            }
                        }
                    }
                }
            }

            //--- Form Button Bar

            Div(
                attrs = {
                    style {
                        flexGrow(0)
                        flexShrink(0)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        flexWrap(FlexWrap.Wrap)
                        justifyContent(
                            if (!styleIsNarrowScreen) {
                                JustifyContent.Center
                            } else {
                                JustifyContent.SpaceBetween
                            }
                        )
                        alignItems(AlignItems.Center)
                        backgroundColor(getColorFormBack())
                        borderTop(width = 1.px, lineStyle = LineStyle.Solid, color = colorMainBorder)
                        padding(styleControlPadding)
                    }
                }
            ) {
                for (formButton in alFormButton) {
                    if (formButton.icon.isNotEmpty()) {
                        Img(
                            src = formButton.icon,
                            attrs = {
                                style {
                                    backgroundColor(
                                        if (formButton.withNewData) {
                                            getColorFormActionButtonSaveBack()
                                        } else {
                                            getColorFormActionButtonOtherBack()
                                        }
                                    )
                                    setBorder(getStyleFormActionButtonBorder())
                                    fontSize(styleCommonButtonFontSize)
                                    padding(styleIconButtonPadding)
                                    setMargins(arrStyleCommonMargin)
                                    cursor("pointer")
                                }
                                title(formButton.tooltip)
                                onClick {
                                    call(formButton.url, formButton.withNewData, formButton.question)
                                }
                            }
                        )
                    } else {
                        Text(formButton.text)
                    }
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun start() {
        readHeader()
        readForm()
    }

    private fun readHeader() {
        var tabToolTip = ""

        alTitleData.clear()
        for ((url, text) in formResponse.alHeader) {
            tabToolTip += (if (tabToolTip.isEmpty()) "" else " | ") + text
            alTitleData.add(FormTitleData(url, text))
        }
        root.setTabInfo(tabId, formResponse.tab, tabToolTip)
    }

    private fun readForm() {

        var columnNo = 0       // счётчик столбцов grid-формы
        var columnIndex = 0
        var rowIndex = 0
        val hmFormCellMaster = mutableMapOf<String, Int>()
        var autoFocusId: String? = null
        val alFormCellMasterPreAction = mutableListOf<FormGridData>()
        var autoClickUrl: String? = null

        rowIndex = getGridFormCaptions(formResponse.alFormColumn, rowIndex, 0, alGridData)
        //--- с запасом отдадим предыдущую сотню id-шек на построение grid-заголовков
        var gridCellId = 100

        var maxColumnCount = 0
        for (formCell in formResponse.alFormCell) {
            //--- поле без заголовка считается невидимым (hidden)
            if (formCell.caption.isEmpty()) {
                val formGridData = getFormGridData(
                    formCell = formCell,
                    gridCellId = gridCellId,
                    itHidden = true
                )

                alGridData.add(formGridData)
                when (formCell.cellType.toString()) {
                    FormCellType.BOOLEAN.toString() -> {
                        hmFormCellMaster[formCell.booleanName] = gridCellId
                        hmFormCellVisible[gridCellId] = mutableListOf()
                        hmFormCellCaption[gridCellId] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }

                    FormCellType.COMBO.toString() -> {
                        hmFormCellMaster[formCell.comboName] = gridCellId
                        hmFormCellVisible[gridCellId] = mutableListOf()
                        hmFormCellCaption[gridCellId] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }

                    FormCellType.RADIO.toString() -> {
                        hmFormCellMaster[formCell.comboName] = gridCellId
                        hmFormCellVisible[gridCellId] = mutableListOf()
                        hmFormCellCaption[gridCellId] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }
                }

                gridCellId++
                continue
            }

            val alSlaveId = mutableListOf<Int>()

            //--- обычная форма: начинаем новую строку
            if (formResponse.alFormColumn.isEmpty()) {
                columnIndex = 0
                if (formCell.formPinMode.toString() == FormPinMode.ON.toString() ||
                    formCell.formPinMode.toString() == FormPinMode.AUTO.toString() && !formCell.itEditable && formCell.selectorSetURL.isEmpty()
                ) {
                    rowIndex++
                } else {
                    //--- добавим разделитель для отвязки от предыдущего блока полей ввода
                    rowIndex++

                    val emptyCell = FormGridData(
                        id = gridCellId,
                        cellType = FormCellTypeClient.LABEL,
                        style = {
                            gridArea("${rowIndex + 1}", "1", "${rowIndex + 2}", "2")
                        },
                        styleAdd = {},
                        aText = "<br>",
                    )

                    alGridData.add(emptyCell)
                    alSlaveId.add(gridCellId)
                    gridCellId++

                    rowIndex++
                }
            }

            //--- если это обычная форма или первое поле в строке GRID-формы, то добавляем левый заголовок поля
            var captionGridCell: FormGridData? = null
            if (formResponse.alFormColumn.isEmpty() || columnNo == 0) {
                val isWideOrGrid = !styleIsNarrowScreen || formResponse.alFormColumn.isNotEmpty()
                captionGridCell = FormGridData(
                    id = gridCellId,
                    cellType = FormCellTypeClient.LABEL,
                    style = {
                        gridArea("${rowIndex + 1}", "${columnIndex + 1}", "${rowIndex + 2}", "${columnIndex + 2}")
                        justifySelf(
                            if (isWideOrGrid) {
                                "flex-end"
                            } else {
                                "flex-start"
                            }
                        )
                        alignSelf(AlignSelf.Center)
                        paddingTop(
                            if (isWideOrGrid) {
                                styleFormRowTopBottomPadding
                            } else {
                                styleFormLabelPadding
                            }
                        )
                        paddingRight(
                            if (isWideOrGrid) {
                                styleFormRowPadding
                            } else {
                                0.px
                            }
                        )
                        paddingBottom(styleFormRowTopBottomPadding)
                        paddingLeft(
                            if (isWideOrGrid) {
                                0.px
                            } else {
                                styleFormLabelPadding
                            }
                        )
                    },
                    styleAdd = {},
                    aText = formCell.caption,
                )

                alGridData.add(captionGridCell)

                alSlaveId.add(gridCellId)
                gridCellId++
                //--- если это широкий экран или строка GRID-формы
                if (!styleIsNarrowScreen || formResponse.alFormColumn.isNotEmpty()) {
                    columnIndex++
                } else {
                    rowIndex++
                }
            }

            val formGridData = getFormGridData(
                formCell = formCell,
                gridCellId = gridCellId,
                itHidden = false,
                rowIndex = rowIndex,
                columnIndex = columnIndex,
                isGridForm = formResponse.alFormColumn.isNotEmpty()
            )
            //--- на тачскринах автофокус только бесит автоматическим включением клавиатуры
            if (!getStyleIsTouchScreen()) {
                //--- set autofocus from server
                if (formCell.itAutoFocus) {
                    autoFocusId = if (formGridData.alSubId.isNotEmpty()) {
                        "i_${tabId}_${formGridData.id}_${formGridData.alSubId[0]}"
                    } else {
                        "i_${tabId}_${formGridData.id}"
                    }
                }
                //--- automatic autofocus setting
                else if (autoFocusId == null && !formGridData.isReadOnly) {
                    autoFocusId = if (formGridData.alSubId.isNotEmpty()) {
                        "i_${tabId}_${formGridData.id}_${formGridData.alSubId[0]}"
                    } else {
                        "i_${tabId}_${formGridData.id}"
                    }
                }
            }
            alGridData.add(formGridData)

            //--- проверка на изначальную пустоту поля-автоселектора
            //--- (по умолчанию оно заполнено, чтобы не запустить автоселектор на непроверяемых полях)
            var isEmptyFieldValue = false
            when (formCell.cellType.toString()) {
                FormCellType.STRING.toString(), FormCellType.INT.toString(), FormCellType.DOUBLE.toString() -> {
                    isEmptyFieldValue = formCell.value.isEmpty()
                }

                FormCellType.TEXT.toString() -> {
                    isEmptyFieldValue = formCell.textValue.isEmpty()
                }

                FormCellType.BOOLEAN.toString() -> {
                    hmFormCellMaster[formCell.booleanName] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }

                FormCellType.DATE.toString(), FormCellType.TIME.toString(), FormCellType.DATE_TIME.toString() -> {
                    isEmptyFieldValue = formCell.alDateTimeField[0].second.isEmpty()
                }

                FormCellType.COMBO.toString() -> {
                    hmFormCellMaster[formCell.comboName] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }

                FormCellType.RADIO.toString() -> {
                    hmFormCellMaster[formCell.comboName] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }
            }
            alSlaveId.add(gridCellId)
            gridCellId++

            columnNo++

            //--- если это широкий экран или строка GRID-формы
            if (!styleIsNarrowScreen || formResponse.alFormColumn.isNotEmpty()) {
                columnIndex++
            } else {
                rowIndex++
            }

            //--- если это последнее поле в строке GRID-формы, то добавляем правый заголовок поля
            if (formResponse.alFormColumn.isNotEmpty() && columnNo == formResponse.columnCount) {
                alGridData.add(
                    FormGridData(
                        id = gridCellId++,
                        cellType = FormCellTypeClient.LABEL,
                        style = {
                            gridArea("${rowIndex + 1}", "${columnIndex + 1}", "${rowIndex + 2}", "${columnIndex + 2}")
                            justifySelf("flex-start")
                            alignSelf(AlignSelf.Center)
                        },
                        styleAdd = {},
                        aText = formCell.caption,
                    )
                )

                //columnIndex++     // здесь нет смысла

                columnNo = 0   // новая строка GRID-формы
                columnIndex = 0
                rowIndex++
            }

            //--- автостарт селектора запускается, только если поле данных пустое,
            //-- иначе зациклимся на старте
            if (formCell.itAutoStartSelector && isEmptyFieldValue && autoClickUrl == null) {
                autoClickUrl = formGridData.selectorSetURL
            }

            //--- определим visible-зависимости
            for ((name, state, value) in formCell.alVisible) {
                hmFormCellMaster[name]?.let { masterId ->
                    for (slaveId in alSlaveId) {
                        hmFormCellVisible[masterId]?.add(FormCellVisibleInfo(state, value, slaveId))
                    }
                }
            }

            //--- определим caption-зависимости
            if (captionGridCell != null)
                for ((name, str, value) in formCell.alCaption) {
                    hmFormCellMaster[name]?.let { masterId ->
                        hmFormCellCaption[masterId]?.add(FormCellCaptionInfo(str, value, captionGridCell.id))
                    }
                }

            maxColumnCount = max(maxColumnCount, columnIndex)
        }
        rowIndex = getGridFormCaptions(formResponse.alFormColumn, rowIndex + 1, gridCellId, alGridData)

        formResponse.alFormButton.forEach { formButton ->
            val icon = hmFormIcon[formButton.iconName] ?: ""
            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
            val caption = if (formButton.iconName.isNotBlank() && icon.isBlank()) {
                formButton.iconName
            } else {
                formButton.caption
            }

            alFormButton.add(
                FormButtonData(
                    url = formButton.url,
                    withNewData = formButton.withNewData,
                    icon = icon,
                    text = caption,
                    tooltip = formButton.caption,
                    question = formButton.question,
                )
            )
            //--- назначение кнопок на горячие клавиши
            when (formButton.key) {
                BUTTON_KEY_AUTOCLICK -> if (autoClickUrl == null) {
                    autoClickUrl = formButton.url
                }

                BUTTON_KEY_SAVE -> formSaveUrl = formButton.url
                BUTTON_KEY_EXIT -> formExitUrl = formButton.url
            }
        }

        //--- если это грид-форма, то сделать одинаковые ячейки, если обычная - поделить между caption и edit в пропорции 1fr и 2fr
        addStyleGrid = {
            gridTemplateRows("repeat(${rowIndex + 1},max-content)")
            gridTemplateColumns("repeat($maxColumnCount,auto)")
        }

        //--- начальные установки видимости и caption-зависимостей
        for (gridData in alFormCellMasterPreAction) {
            doVisibleAndCaptionChangeBody(gridData)
        }

        autoClickUrl?.let { acUrl ->
            call(acUrl, true, null)
        } ?: autoFocusId?.let { afId ->
//            Vue.nextTick {
            val element = document.getElementById(afId)
            if (element is HTMLElement) {
                element.focus()
            }
//            }
        }
    }

    private fun getGridFormCaptions(alFormColumn: Array<String>, aRowIndex: Int, aGridCellId: Int, alGridData: MutableList<FormGridData>): Int {
        var rowIndex = aRowIndex
        var gridCellId = aGridCellId
        //--- верхние/нижние заголовки столбцов GRID-формы
        if (alFormColumn.isNotEmpty()) {
            var columnIndex = 1
            for (caption in alFormColumn) {
                alGridData.add(
                    FormGridData(
                        id = gridCellId++,
                        cellType = FormCellTypeClient.LABEL,
                        style = {
                            gridArea("${rowIndex + 1}", "${columnIndex + 1}", "${rowIndex + 2}", "${columnIndex + 2}")
                            justifySelf("center")
                            alignSelf(AlignSelf.Center)
                            padding(styleFormRowPadding)
                        },
                        styleAdd = {},
                        aText = caption,
                    )
                )
                columnIndex++
            }
            rowIndex++
        }
        return rowIndex
    }

    private fun getFormGridData(
        formCell: FormCell,
        gridCellId: Int,
        itHidden: Boolean,
        rowIndex: Int = 0,
        columnIndex: Int = 0,
        isGridForm: Boolean = false
    ): FormGridData {

        val style: StyleScope.() -> Unit = {
            gridArea("${rowIndex + 1}", "${columnIndex + 1}", "${rowIndex + 2}", "${columnIndex + 2}")
            justifySelf(
                if (isGridForm) {
                    "center"
                } else {
                    "flex-start"
                }
            )
            alignSelf(AlignSelf.Center)
            paddingTop(
                if (!styleIsNarrowScreen || isGridForm) {
                    styleFormRowTopBottomPadding
                } else {
                    styleFormRowPadding
                }
            )
            paddingRight(0.px)
            paddingBottom(
                if (!styleIsNarrowScreen || isGridForm) {
                    styleFormRowTopBottomPadding
                } else {
                    styleFormRowPadding
                }
            )
            paddingLeft(
                if (isGridForm) {
                    0.px
                } else {
                    styleFormRowPadding
                }
            )
        }
        //--- добавляем отдельно только для тех строк, где есть select-кнопки,
        //--- иначе разъезжаются radio-buttons в одну строчку (а не в один столбец)
        val styleAdd: StyleScope.() -> Unit = if (formCell.selectorSetURL.isNotBlank()) {
            {
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Row)
                flexWrap(FlexWrap.Wrap)
                alignItems(AlignItems.Center)
            }
        } else {
            {}
        }

        val gridData = when (formCell.cellType.toString()) {
            FormCellType.STRING.toString(), FormCellType.INT.toString(), FormCellType.DOUBLE.toString() -> {
                FormGridData(
                    id = gridCellId,
                    cellType = FormCellTypeClient.STRING,
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = itHidden,
                    error = formCell.errorMessage,
                    aText = formCell.value,
                    isPassword = formCell.itPassword,
                    colCount = getStyleFormEditBoxColumn(formCell.column),
                    isReadOnly = !formCell.itEditable,
                )
            }

            FormCellType.TEXT.toString() -> {
                FormGridData(
                    id = gridCellId,
                    cellType = FormCellTypeClient.TEXT,
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = itHidden,
                    error = formCell.errorMessage,
                    aText = formCell.textValue,
                    rowCount = formCell.textRow,
                    colCount = getStyleFormEditBoxColumn(formCell.textColumn),
                    isReadOnly = !formCell.itEditable,
                )
            }

            FormCellType.BOOLEAN.toString() -> {
                FormGridData(
                    id = gridCellId,
                    cellType = if (formCell.arrSwitchText.isEmpty()) {
                        FormCellTypeClient.CHECKBOX
                    } else {
                        FormCellTypeClient.SWITCH
                    },
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = itHidden,
                    error = formCell.errorMessage,
                    aBool = formCell.booleanValue,
                    isReadOnly = !formCell.itEditable,
                    alSwitchText = formCell.arrSwitchText.toList(),
                )
            }

            FormCellType.DATE.toString(), FormCellType.TIME.toString(), FormCellType.DATE_TIME.toString() -> {
                FormGridData(
                    id = gridCellId,
                    cellType = when (formCell.cellType) {
                        FormCellType.DATE -> {
                            FormCellTypeClient.DATE
                        }

                        FormCellType.TIME -> {
                            FormCellTypeClient.TIME
                        }

                        else -> {
                            FormCellTypeClient.DATE_TIME
                        }
                    },
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = itHidden,
                    error = formCell.errorMessage,
                    aAlDateTime = formCell.alDateTimeField.map { it.second },
                    isReadOnly = !formCell.itEditable,
                ).apply {
                    alSubId.clear()
                    alDateTime.indices.forEach { i ->
                        alSubId += i
                    }
                }
            }

            FormCellType.COMBO.toString() -> {
                FormGridData(
                    id = gridCellId,
                    cellType = FormCellTypeClient.COMBO,
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = itHidden,
                    error = formCell.errorMessage,
                    aCombo = formCell.comboValue,
                    alComboData = formCell.alComboData.map { FormComboData(it.first, it.second) },
                    isReadOnly = !formCell.itEditable,
                )
            }

            FormCellType.RADIO.toString() -> {
                FormGridData(
                    id = gridCellId,
                    cellType = FormCellTypeClient.RADIO,
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = itHidden,
                    error = formCell.errorMessage,
                    aCombo = formCell.comboValue,
                    alComboData = formCell.alComboData.map { FormComboData(it.first, it.second) },
                    isReadOnly = !formCell.itEditable,
                ).apply {
                    alSubId.clear()
                    alComboData.indices.forEach { i ->
                        alSubId += i
                    }
                }
            }

            FormCellType.FILE.toString() -> {
                FormGridData(
                    id = gridCellId,
                    cellType = FormCellTypeClient.FILE,
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = itHidden,
                    error = formCell.errorMessage,
                    fileId = formCell.fileID,
                    alFileData = formCell.alFile.map { FormFileData(it.first, it.second, it.third) },
                )
            }
            //--- недогадливость (ошибка) парсера/компилятора из-за использования enum.toString() - на самом деле больше нет вариантов
            else -> {
                println("ERROR: Call when-else-block in getFormGridData!")
                FormGridData(
                    id = 0,
                    cellType = FormCellTypeClient.LABEL,
                    style = style,
                    styleAdd = styleAdd,
                    isHidden = true, // от греха подальше :)
                    error = formCell.errorMessage,
                )
            }
        }
        gridData.selectorSetURL = formCell.selectorSetURL
        gridData.selectorClearURL = formCell.selectorClearURL

        return gridData
    }

    private fun doVisibleAndCaptionChangeBody(gdMaster: FormGridData) {
        //--- определение контрольного значения
        val controlValue =
            when (gdMaster.cellType) {
                FormCellTypeClient.CHECKBOX, FormCellTypeClient.SWITCH -> {
                    if (
                        if (gdMaster.isHidden) {
                            gdMaster.oldBool
                        } else {
                            gdMaster.bool.value
                        }
                    ) {
                        1
                    } else {
                        0
                    }
                }

                FormCellTypeClient.COMBO -> if (gdMaster.isHidden) {
                    gdMaster.oldCombo
                } else {
                    gdMaster.combo.value
                }

                FormCellTypeClient.RADIO -> if (gdMaster.isHidden) {
                    gdMaster.oldCombo
                } else {
                    gdMaster.combo.value
                }

                else -> 0
            }

        hmFormCellVisible[gdMaster.id]?.let { alFCVI ->
            alFCVI.forEach { fcvi ->
                alGridData.find { gd -> gd.id == fcvi.id }?.let { gdSlave ->
                    gdSlave.isVisible.value = (fcvi.state == fcvi.hsValue.contains(controlValue))
                }
            }
        }

        hmFormCellCaption[gdMaster.id]?.let { alFCCI ->
            alFCCI.forEach { fcci ->
                alGridData.find { gd -> gd.id == fcci.id }?.let { gdSlave ->
                    if (fcci.hsValue.contains(controlValue)) {
                        gdSlave.text.value = fcci.caption
                    }
                }
            }
        }
    }

    /*
            "selectAllText" to { event: Event ->
                //--- программный селект текста на тачскринах вызывает показ надоедливого окошка с копированием/вырезанием текста (и так на каждый input)
                if (!styleIsTouchScreen()) {
                    (event.target as? HTMLInputElement)?.select()
                }
            },
            "closeTabById" to {
                that().`$root`.closeTabById(tabId)
            },
            "doVisibleAndCaptionChange" to { gdMaster: FormGridData ->
                val that = that()
                if (gdMaster.cellType == FormCellType_.SWITCH) {
                    //--- из-за применения onlick
                    Vue.nextTick {
                        //--- manual switch because button realization instead standart checkbox
                        gdMaster.bool = !gdMaster.bool
                        doVisibleAndCaptionChangeBody(that, gdMaster)
                    }
                } else {
                    doVisibleAndCaptionChangeBody(that, gdMaster)
                }
            },
            "showFile" to { url: String ->
                that().`$root`.openTab(url)
            },
            "deleteFile" to { gridData: FormGridData, fileData: FormFileData ->
                gridData.arrFileData = gridData.arrFileData!!.filter { it.id != fileData.id }.toTypedArray()
                //--- сохраним ID удаляемого файла для передачи на сервер
                if (fileData.id > 0) {
                    gridData.alFileRemovedID.add(fileData.id)
                }
                //--- или просто удалим ранее добавленный файл из списка
                else {
                    gridData.hmFileAdd.remove(fileData.id)
                }
            },
            "addFileDialog" to { id: String ->
                (document.getElementById(id) as HTMLElement).click()
            },
            "addFile" to { gridData: FormGridData, event: Event ->
                val files = (event.target as? HTMLInputElement)?.files
                files?.let {
                    val alFileData = gridData.arrFileData!!.toMutableList()
                    val hmFileAdd = gridData.hmFileAdd

                    val formData = org.w3c.xhr.FormData().also {
                        for (file in files.asList()) {
                            val id = -getRandomInt()

                            alFileData.add(FormFileData(id, "", file.name))
                            hmFileAdd[id] = file.name

                            it.append("form_file_ids", id.toString())
                            it.append("form_file_blobs", file)
                        }
                    }

                    invokeUploadFormFile(formData)

                    gridData.arrFileData = alFileData.toTypedArray()
                }
            },
            "doNextFocus" to { gridDataId: Int, gridDataSubId: Int ->
                val arrGridData = that().arrGridData.unsafeCast<Array<FormGridData>>()

                val curIndex = arrGridData.indexOfFirst { formGridData ->
                    formGridData.id == gridDataId
                }

                if (curIndex >= 0) {
                    val curGridData = arrGridData[curIndex]

                    var nextGridId = -1
                    var nextSubGridId = -1

                    //--- try set focus to next sub-field into fields group (date/time-textfields or radio-buttons)
                    curGridData.arrSubId?.let { arrSubId ->
                        val curSubIndex = arrSubId.indexOf(gridDataSubId)
                        if (curSubIndex >= 0) {
                            if (curSubIndex < arrSubId.lastIndex) {
                                nextGridId = gridDataId
                                nextSubGridId = arrSubId[curSubIndex + 1]
                            }
                        }
                    }
                    //--- else try set focus to next field or first sub-field in next field
                    if (nextGridId == -1 && nextSubGridId == -1) {
                        if (curIndex < arrGridData.lastIndex) {
                            var nextIndex = curIndex + 1
                            //--- search non-label element
                            while (nextIndex <= arrGridData.lastIndex && arrGridData[nextIndex].cellType == FormCellType_.LABEL) {
                                nextIndex++
                            }
                            val nextGridData = arrGridData[nextIndex]
                            nextGridId = nextGridData.id
                            nextSubGridId = nextGridData.arrSubId?.firstOrNull() ?: -1
                        }
                    }

                    val nextFocusId = if (nextSubGridId < 0) {
                        "i_${tabId}_${nextGridId}"
                    } else {
                        "i_${tabId}_${nextGridId}_${nextSubGridId}"
                    }

    //                Vue.nextTick {
                    val element = document.getElementById(nextFocusId)
                    if (element is HTMLElement) {
                        element.focus()
                    }
    //                }
                }
            },
     */

    private fun call(formAppParam: String, withNewData: Boolean, dialogQuestion: String?) {
        dialogQuestion?.let {
            root.dialogActionFun = {
                doInvoke(formAppParam, withNewData)
            }
            root.dialogQuestion.value = dialogQuestion
            root.showDialogCancel.value = true
            root.showDialog.value = true
        } ?: run {
            doInvoke(formAppParam, withNewData)
        }
    }

    private fun doInvoke(formAppParam: String, withNewData: Boolean) {
        val alFormData = mutableListOf<FormData>()

        alGridData.forEach { gridData ->
            val withNewValues = withNewData && !gridData.isHidden

            when (gridData.cellType) {
                FormCellTypeClient.STRING -> {
                    alFormData += FormData(
                        stringValue = if (withNewValues) {
                            gridData.text.value
                        } else {
                            gridData.oldText
                        }
                    )
                }

                FormCellTypeClient.TEXT -> {
                    alFormData += FormData(
                        textValue = if (withNewValues) {
                            gridData.text.value
                        } else {
                            gridData.oldText
                        }
                    )
                }

                FormCellTypeClient.CHECKBOX, FormCellTypeClient.SWITCH -> {
                    alFormData += FormData(
                        booleanValue = if (withNewValues) {
                            gridData.bool.value
                        } else {
                            gridData.oldBool
                        }
                    )
                }

                FormCellTypeClient.DATE, FormCellTypeClient.TIME, FormCellTypeClient.DATE_TIME -> {
                    alFormData += FormData(alDateTimeValue = (if (withNewValues) {
                        gridData.alDateTime.map { it.value }
                    } else {
                        gridData.oldAlDateTime
                    }).toList())
                }

                FormCellTypeClient.COMBO -> {
                    alFormData += FormData(
                        comboValue = if (withNewValues) {
                            gridData.combo.value
                        } else {
                            gridData.oldCombo
                        }
                    )
                }

                FormCellTypeClient.RADIO -> {
                    alFormData += FormData(
                        comboValue = if (withNewValues) {
                            gridData.combo.value
                        } else {
                            gridData.oldCombo
                        }
                    )
                }

                FormCellTypeClient.FILE -> {
                    alFormData += FormData(
                        fileId = gridData.fileId,
                        hmFileAdd = if (withNewValues) {
                            gridData.hmFileAdd.mapKeys { it.key.toString() }
                        } else {
                            mapOf()
                        },
                        alFileRemovedId = if (withNewValues) {
                            gridData.alFileRemovedID
                        } else {
                            listOf()
                        }
                    )
                }

                else -> {}
            }
        }
        appControl.call(AppRequest(action = formAppParam, alFormData = alFormData))
    }

}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private enum class FormCellTypeClient { LABEL, STRING, TEXT, CHECKBOX, SWITCH, DATE, TIME, DATE_TIME, COMBO, RADIO, FILE }

private class FormTitleData(val url: String, val text: String)

private class FormGridData(
    val id: Int,

    val cellType: FormCellTypeClient,

    val style: StyleScope.() -> Unit,
    val styleAdd: StyleScope.() -> Unit,

    val isHidden: Boolean = false,
    var isVisible: MutableState<Boolean> = mutableStateOf(true),
    val error: String = "",

    aText: String = "",
    aBool: Boolean = false,
    aAlDateTime: List<String> = emptyList(),
    aCombo: Int = 0,

    val isPassword: Boolean = false,

    val colCount: Int = 20,
    val rowCount: Int = 1,
    val isReadOnly: Boolean = false,

    val alSwitchText: List<String> = emptyList(),
    val alComboData: List<FormComboData> = emptyList(),

    val fileId: Int = 0,
    var alFileData: List<FormFileData> = emptyList(),
) {
    var alSubId: MutableList<Int> = mutableListOf()

    var text: MutableState<String> = mutableStateOf(aText)
    val oldText: String = aText

    var bool: MutableState<Boolean> = mutableStateOf(aBool)
    val oldBool: Boolean = aBool

    val alDateTime: List<MutableState<String>> = aAlDateTime.map { mutableStateOf(it) }
    val oldAlDateTime: List<String> = aAlDateTime.map { it }

    val combo: MutableState<Int> = mutableStateOf(aCombo)
    val oldCombo: Int = aCombo

    var selectorSetURL: String = ""
    var selectorClearURL: String = ""

    val hmFileAdd = mutableMapOf<Int, String>()
    val alFileRemovedID = mutableListOf<Int>()
}

private class FormComboData(val value: Int, val text: String)

private class FormFileData(val id: Int, val url: String, val text: String)

private class FormCellVisibleInfo(val state: Boolean, val hsValue: Array<Int>, val id: Int)
private class FormCellCaptionInfo(val caption: String, val hsValue: Array<Int>, val id: Int)

private class FormButtonData(
    val url: String,
    val withNewData: Boolean,
    val question: String?,
    val icon: String,
    val text: String,
    val tooltip: String,
)

/*
пока не понятно, как сделать в веб-версии
        if( formCell.alComboString.isNotEmpty() ) {
            fci.comboBox = ComboBox()
            fci.comboBox.getItems().addAll( formCell.alComboString )
            //--- надо в самом конце инициализации combo-box'a, иначе не срабатывает!
            fci.comboBox.getSelectionModel().select( formCell.value )
            fci.comboBox.setDisable( !formCell.itEditable )
            fci.comboBox.setEditable( true )
            if( fci.comboBox.getEditor() != null ) fci.comboBox.getEditor().font = curFont
            if( fci.comboBox.getButtonCell() != null ) fci.comboBox.getButtonCell().font = curFont

            //fci.comboBox.setOnAction(  this  ); - это свободно-редактируемый combo-box, cell-action на его выбор не предусмотрено
            fci.comboBox.setOnKeyPressed( this )

            GridPane.setHalignment( fci.comboBox, HPos.LEFT )
            GridPane.setValignment( fci.comboBox, VPos.CENTER )

            if( focusableComponent == null && formCell.itEditable ) focusableComponent = fci.comboBox

            return fci.comboBox
        }

*/

