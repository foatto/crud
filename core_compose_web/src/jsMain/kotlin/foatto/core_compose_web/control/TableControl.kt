package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import foatto.core.link.*
import foatto.core_compose_web.*
import foatto.core_compose_web.control.composable.getToolBarSpan
import foatto.core_compose_web.control.model.TitleData
import foatto.core_compose_web.style.*
import foatto.core_compose_web.util.getColorFromInt
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.readOnly
import org.jetbrains.compose.web.attributes.size
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.appearance
import org.jetbrains.compose.web.css.properties.borderBottom
import org.jetbrains.compose.web.css.properties.borderLeft
import org.jetbrains.compose.web.css.properties.borderRight
import org.jetbrains.compose.web.css.properties.borderTop
import org.jetbrains.compose.web.css.properties.userSelect
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.math.max

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

var getColorTableHeaderBack: () -> CSSColorValue = { colorMainBack1 }
var getColorTableToolbarBack: () -> CSSColorValue = { colorMainBack1 }
var getColorTablePagebarBack: () -> CSSColorValue = { colorMainBack1 }

var getColorTableFindButtonBack: () -> CSSColorValue = { getColorButtonBack() }
var getStyleTableFindEditorBorderRadius: () -> Array<CSSSize> = {
    arrayOf(
        styleInputBorderRadius,
        styleInputBorderRadius,
        styleInputBorderRadius,
        styleInputBorderRadius,
    )
}
var getStyleTableFindButtonBorderRadius: () -> Array<CSSSize> = {
    arrayOf(
        styleButtonBorderRadius,
        styleButtonBorderRadius,
        styleButtonBorderRadius,
        styleButtonBorderRadius,
    )
}
var getStyleTableFindControlMargin: () -> Array<CSSSize> = { arrStyleCommonMargin }
private fun getStyleTableFindEditLength() = scaledScreenWidth / (if (screenDPR <= 1.0) 64 else 24)
private val styleTableFindEditorFontSize = COMMON_FONT_SIZE
private fun getStyleTableFindEditorPadding(): CSSSize = when (styleIconSize) {
    36 -> 0.56.cssRem
    48 -> 0.95.cssRem
    else -> 0.56.cssRem   // пусть лучше будет поменьше, чем раздирать тулбар
}

var getColorToolbarButtonBack: () -> CSSColorValue = { getColorButtonBack() }
var getColorRefreshButtonBack: () -> CSSColorValue = { getColorButtonBack() }
var getStyleToolbarButtonBorder: () -> BorderData = { BorderData(getColorButtonBorder(), LineStyle.Solid, 1.px, styleButtonBorderRadius) }

var getColorTableCaptionBack: () -> CSSColorValue = { colorMainBack1 }
var getStyleTableCaptionPadding: () -> Array<CSSSize> = { arrayOf(styleControlPadding, styleControlPadding, styleControlPadding, styleControlPadding) }
var getStyleTableCaptionBorderLeft: () -> BorderData = { BorderData(colorMainBorder, LineStyle.Solid, 0.5.px, 0.cssRem) }
var getStyleTableCaptionBorderTop: () -> BorderData = { BorderData(colorMainBorder, LineStyle.Solid, 1.px, 0.cssRem) }
var getStyleTableCaptionBorderRight: () -> BorderData = { BorderData(colorMainBorder, LineStyle.Solid, 0.5.px, 0.cssRem) }
var getStyleTableCaptionBorderBottom: () -> BorderData = { BorderData(colorMainBorder, LineStyle.Solid, 1.px, 0.cssRem) }
private var getStyleTableCaptionJustifyH: () -> JustifyContent = { JustifyContent.Center }    // flex-start
var getStyleTableCaptionAlignH: () -> AlignItems = { AlignItems.Center }    // flex-start
var getStyleTableCaptionAlignV: () -> JustifyContent = { JustifyContent.Center }    // flex-start
var getStyleTableCaptionFontSize: () -> CSSSize = { getStyleTableTextFontSize() }
var getStyleTableCaptionFontWeight: () -> String = { "normal" }

var colorTableGroupBack0: CSSColorValue = COLOR_MAIN_BACK_0
var colorTableGroupBack1: CSSColorValue = COLOR_MAIN_BACK_0

private var colorTableRowBack0: CSSColorValue = COLOR_MAIN_BACK_0
var colorTableRowBack1: CSSColorValue = COLOR_MAIN_BACK_0

var getColorTableRowHover: () -> CSSColorValue = { colorCurrentAndHover }

var getStyleTableTextFontSize: () -> CSSSize = { (if (!styleIsNarrowScreen) 1.0 else 0.8).cssRem }

private val arrStyleTableGridCellTypePadding: Array<CSSSize> = arrayOf(
    CONTROL_TOP_DOWN_SIDE_PADDING,
    CONTROL_PADDING,
    CONTROL_TOP_DOWN_SIDE_PADDING,
    CONTROL_PADDING
)

var getColorTablePageBarCurrentBack: () -> CSSColorValue = { getColorButtonBack() }
var getStyleTablePageBarOtherBorder: () -> BorderData = { BorderData(getColorButtonBorder(), LineStyle.Solid, 1.px, styleButtonBorderRadius) }
private val styleTablePageBarTopBottomPadding = 0.3.cssRem
private val arrStyleTablePageBarPadding: Array<CSSSize> = arrayOf(
    styleTablePageBarTopBottomPadding,
    CONTROL_PADDING,
    styleTablePageBarTopBottomPadding,
    CONTROL_PADDING,
)

var getStyleTablePageButtonWidth: (Int) -> CSSSize = { buttonCount ->
    if (styleIsNarrowScreen) {
        when (buttonCount) {
            4 -> 3.4    // 5.2 - слишком крупно и глуповато, используется всего один раз
            5 -> 3.4    // 4.1 - используется всего один раз
            6 -> 3.4
            7 -> 2.9
            8 -> 2.5
            else -> 2.0
        }.cssRem
    } else {
        6.0.cssRem
    }
}
var getStyleTablePageButtonFontSize: (Int) -> CSSSize = { buttonCount ->
    if (styleIsNarrowScreen) {
        when (buttonCount) {
            4 -> 1.7    // 2.6 - слишком крупно и глуповато, используется всего один раз
            5 -> 1.7    // 2.0 - используется всего один раз
            6 -> 1.7
            7 -> 1.4
            8 -> 1.2
            else -> 1.0
        }.cssRem
    } else {
        2.6.cssRem
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

open class TableControl(
    private val root: Root,
    private val appControl: AppControl,
    private val appParam: String,
    private val tableResponse: TableResponse,
    tabId: Int,
) : AbstractControl(tabId), iClickableMenu {

    companion object {
        val hmTableIcon: MutableMap<String, String> = mutableMapOf()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected var tableClientActionFun: (
        action: String,
        alParam: List<Pair<String, String>>,
        tableControl: TableControl
    ) -> Unit = { _: String, _: List<Pair<String, String>>, _: TableControl ->
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val selectorCancelUrl = mutableStateOf("")
    private val isFindTextVisible = mutableStateOf(!styleIsNarrowScreen)
    private val findUrl = mutableStateOf("")
    private val findText = mutableStateOf("")

    private val isFormButtonVisible = mutableStateOf(false)
    private val isGotoButtonVisible = mutableStateOf(false)
    private val isPopupButtonVisible = mutableStateOf(false)

    private val alAddButton = mutableStateListOf<AddActionButtonClient>()
    private val alServerButton = mutableStateListOf<ServerActionButtonClient>()
    private val alClientButton = mutableStateListOf<ClientActionButtonClient>()
    private val alPageButton = mutableStateListOf<PageButton>()

    private val gridMaxRow = mutableStateOf(0)
    private val gridMaxCol = mutableStateOf(0)
    private val alGridData = mutableStateListOf<TableGridData>()

    private val pageUpUrl = mutableStateOf("")
    private val pageDownUrl = mutableStateOf("")

    private val alRowData = mutableStateListOf<TableRowData>()

    private val arrCurPopupData = mutableStateOf<Array<MenuData>?>(null)
    private val isShowPopupMenu = mutableStateOf(false)
    private val currentRow = mutableStateOf(-1)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var popupMenuPosFun: StyleScope.() -> Unit = {}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {

            //--- Table Header
            getTableAndFormHeader(false) { url: String ->
                call(url, false)
            }

            //--- Table Toolbar
            Div(
                attrs = {
                    style {
                        flexGrow(0)
                        flexShrink(0)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        flexWrap(FlexWrap.Wrap)
                        justifyContent(JustifyContent.SpaceBetween)
                        alignItems(AlignItems.Center)
                        borderTop(
                            width = getStyleTableCaptionBorderTop().width,
                            lineStyle = getStyleTableCaptionBorderTop().style,
                            color = getStyleTableCaptionBorderTop().color,
                        )
                        borderBottom(
                            width = getStyleTableCaptionBorderTop().width,
                            lineStyle = getStyleTableCaptionBorderTop().style,
                            color = getStyleTableCaptionBorderTop().color,
                        )
                        padding(styleControlPadding)
                        backgroundColor(getColorTableToolbarBack())
                    }
                }
            ) {
                getToolBarSpan {
                    getToolBarIconButton(
                        isVisible = selectorCancelUrl.value.isNotEmpty(),
                        src = "/web/images/ic_reply_all_${getStyleIconNameSuffix()}dp.png",
                        title = "Отменить выбор",
                    ) {
                        call(selectorCancelUrl.value, false)
                    }
                    if (isFindTextVisible.value) {
                        Input(InputType.Text) {
                            style {
                                setBorder(color = colorMainBorder, arrRadius = getStyleTableFindEditorBorderRadius())
                                fontSize(styleTableFindEditorFontSize)
                                padding(getStyleTableFindEditorPadding())
                                setMargins(getStyleTableFindControlMargin())
                            }
                            size(getStyleTableFindEditLength())
                            placeholder("Поиск...")
                            value(findText.value)
                            onInput { event ->
                                findText.value = event.value
                            }
                            onKeyUp { event ->
                                if (event.key == "Enter") {
                                    doFind(false)
                                }
                            }
                        }
                    }
                    Img(
                        src = "/web/images/ic_search_${getStyleIconNameSuffix()}dp.png",
                        attrs = {
                            style {
                                backgroundColor(getColorTableFindButtonBack())
                                setBorder(color = getColorButtonBorder(), arrRadius = getStyleTableFindButtonBorderRadius())
                                fontSize(styleCommonButtonFontSize)
                                padding(styleIconButtonPadding)
                                setMargins(getStyleTableFindControlMargin())
                                cursor("pointer")
                            }
                            title("Искать")
                            onClick {
                                doFind(false)
                            }
                        }
                    )
                    if (findText.value.isNotEmpty()) {
                        Img(
                            src = "/web/images/ic_youtube_searched_for_${getStyleIconNameSuffix()}dp.png",
                            attrs = {
                                style {
                                    backgroundColor(getColorTableFindButtonBack())
                                    setBorder(color = getColorButtonBorder(), arrRadius = getStyleTableFindButtonBorderRadius())
                                    fontSize(styleCommonButtonFontSize)
                                    padding(styleIconButtonPadding)
                                    setMargins(getStyleTableFindControlMargin())
                                    cursor("pointer")
                                }
                                title("Отключить поиск")
                                onClick {
                                    doFind(true)
                                }
                            }
                        )
                    }
                }
                getToolBarSpan {
                    for (addButton in alAddButton) {
                        getToolBarIconButton(
                            isVisible = !styleIsNarrowScreen || !isFindTextVisible.value,
                            src = addButton.icon,
                            title = addButton.tooltip
                        ) {
                            call(addButton.url, false)
                        }
                    }
                    getToolBarIconButton(
                        isVisible = (!styleIsNarrowScreen || !isFindTextVisible.value) && isFormButtonVisible.value,
                        src = "/web/images/ic_mode_edit_${getStyleIconNameSuffix()}dp.png",
                        title = "Открыть форму"
                    ) {
                        doForm()
                    }
                    getToolBarIconButton(
                        isVisible = (!styleIsNarrowScreen || !isFindTextVisible.value) && isGotoButtonVisible.value,
                        src = "/web/images/ic_exit_to_app_${getStyleIconNameSuffix()}dp.png",
                        title = "Перейти"
                    ) {
                        doGoto()
                    }
                    getToolBarIconButton(
                        isVisible = (!styleIsNarrowScreen || !isFindTextVisible.value) && isPopupButtonVisible.value,
                        src = "/web/images/ic_menu_${getStyleIconNameSuffix()}dp.png",
                        title = "Показать операции по строке"
                    ) {
                        doPopup()
                    }
                }
                getToolBarSpan {
                    for (serverButton in alServerButton) {
                        getToolBarIconButton(
                            isVisible = !styleIsNarrowScreen || (!isFindTextVisible.value && !serverButton.isForWideScreenOnly),
                            src = serverButton.icon,
                            title = serverButton.tooltip
                        ) {
                            call(serverButton.url, serverButton.inNewWindow)
                        }
                    }
                }
                getToolBarSpan {
                    for (clientButton in alClientButton) {
                        getToolBarIconButton(
                            isVisible = !styleIsNarrowScreen || (!isFindTextVisible.value && !clientButton.isForWideScreenOnly),
                            src = clientButton.icon,
                            title = clientButton.tooltip
                        ) {
                            clientAction(clientButton.action, clientButton.params)
                        }
                    }
                }
                getToolBarSpan {
                    Input(InputType.Text) {
                        style {
                            border(width = 0.px)
                            outline("none")
                            backgroundColor(hsla(0, 0, 0, 0))
                            color(hsla(0, 0, 0, 0))
                        }
                        id("table_cursor_$tabId")
                        readOnly()
                        size(1)
                        onInput { event ->
                            findText.value = event.value
                        }
                        onKeyUp { event ->
                            if (event.key == "Enter") {
                                doFind(false)
                            }
//                                       v-on:keyup.up="doKeyUp()"
//                                       v-on:keyup.down="doKeyDown()"
//                                       v-on:keyup.home="doKeyHome()"
//                                       v-on:keyup.end="doKeyEnd()"
//                                       v-on:keyup.page-up="doKeyPageUp()"
//                                       v-on:keyup.page-down="doKeyPageDown()"
//                                       v-on:keyup.enter="doKeyEnter()"
//                                       v-on:keyup.esc="doKeyEsc()"
//                                       v-on:keyup.f4="closeTabById()"
                        }
                    }
                    if (!styleIsNarrowScreen || !isFindTextVisible.value) {
                        Img(
                            src = "/web/images/ic_sync_${getStyleIconNameSuffix()}dp.png",
                            attrs = {
                                style {
                                    backgroundColor(getColorRefreshButtonBack())
                                    fontSize(styleCommonButtonFontSize)
                                    setBorder(getStyleToolbarButtonBorder())
                                    padding(styleIconButtonPadding)
                                    setMargins(arrStyleCommonMargin)
                                    cursor("pointer")
                                }
                                title("Обновить")
                                onClick {
                                    call(appParam, false)
                                }
                            }
                        )
                    }
                }
            }

            //--- Table Grid
            Div(
                attrs = {
                    style {
                        flexGrow(1)
                        flexShrink(1)
                        height(100.percent)     //- необязательно - ???
                        overflow("auto")
                        display(DisplayStyle.Grid)
                        gridTemplateRows("repeat(${gridMaxRow.value},max-content)")
                        gridTemplateColumns("repeat(${gridMaxCol.value},auto)")
                        //--- полностью запретить выделение текста - простейший способ победить паразитное выделение текста вместо лонг-тача на больших сенсорных экранах
                        userSelect("none")     // if( styleIsNarrowScreen ) "none" else "auto"
                    }
                }
            ) {
                for (gridData in alGridData) {
                    Div(
                        attrs = {
                            style {
                                gridData.cellStyleCommon(this)
                                gridData.cellStyleAdd(this)
                                backgroundColor(
                                    if (gridData.dataRow >= 0 && gridData.dataRow == currentRow.value) {
                                        getColorTableRowHover()
                                    } else {
                                        gridData.backColor
                                    }
                                )
                            }
                            onDoubleClick {
                                if (gridData.dataRow >= 0 && alRowData[gridData.dataRow].rowURL.isNotEmpty()) {
                                    call(alRowData[gridData.dataRow].rowURL, alRowData[gridData.dataRow].itRowURLInNewWindow)
                                }
                            }
                            onClick {
                                if (gridData.cellURL.isNotEmpty()) {
                                    call(gridData.cellURL, false)
                                } else {
                                    setCurrentRow(gridData.dataRow)
                                }
                            }
                            onContextMenu { mouseEvent ->
                                if (gridData.dataRow >= 0) {
                                    showPopupMenu(gridData.dataRow, mouseEvent)
                                }
                                mouseEvent.preventDefault()
                            }
                        }
                    ) {
                        when (gridData.cellType) {
                            TableCellType.CHECKBOX -> {
                                Input(InputType.Checkbox) {
                                    checked(gridData.booleanValue!!)
                                    title(gridData.tooltip)
                                    style {
                                        gridData.elementStyle(this)
                                    }
                                    onClick {
                                        isShowPopupMenu.value = false
                                    }
                                }
                            }

                            TableCellType.TEXT -> {
                                if (gridData.textCellData?.icon?.isNotEmpty() == true) {
                                    Img(
                                        src = gridData.textCellData.icon,
                                        attrs = {
                                            title(gridData.tooltip)
                                            //--- style_toolbar_button
                                            style {
                                                gridData.elementStyle(this)
                                            }
                                        }
                                    )
                                } else if (gridData.textCellData?.image?.isNotEmpty() == true) {
                                    Img(
                                        src = gridData.textCellData.image,
                                        attrs = {
                                            title(gridData.tooltip)
                                            //--- style_toolbar_button
                                            style {
                                                gridData.elementStyle(this)
                                            }
                                        }
                                    )
                                } else {
                                    Span(
                                        attrs = {
                                            title(gridData.tooltip)
                                            style {
                                                gridData.elementStyle(this)
                                            }
                                        }
                                    ) {
                                        Text(gridData.textCellData!!.text)
                                    }
                                }
                            }

                            TableCellType.BUTTON -> {
                                for (cellData in gridData.alButtonCellData!!) {
                                    if (cellData.icon.isNotEmpty()) {
                                        Img(
                                            src = cellData.icon,
                                            attrs = {
                                                title(gridData.tooltip)
                                                //--- style_toolbar_button
                                                style {
                                                    cellData.style(this)
                                                }
                                                onClick {
                                                    call(cellData.url, cellData.inNewWindow)
                                                }
                                            }
                                        )
                                    } else if (cellData.image.isNotEmpty()) {
                                        Img(
                                            src = cellData.image,
                                            attrs = {
                                                title(gridData.tooltip)
                                                //--- style_toolbar_button
                                                style {
                                                    cellData.style(this)
                                                }
                                                onClick {
                                                    call(cellData.url, cellData.inNewWindow)
                                                }
                                            }
                                        )
                                    } else {
                                        Button(
                                            attrs = {
                                                title(gridData.tooltip)
                                                style {
                                                    cellData.style(this)
                                                }
                                                onClick {
                                                    call(cellData.url, cellData.inNewWindow)
                                                }
                                            }
                                        ) {
                                            Text(cellData.text)
                                        }
                                    }
                                }
                            }

                            TableCellType.GRID -> {
                                for (alRow in gridData.alGridCellData!!) {
                                    for (cellData in alRow) {
                                        Div(
                                            attrs = {
                                                title(gridData.tooltip)
                                                style {
                                                    cellData.style(this)
                                                }
                                            }
                                        ) {
                                            if (cellData.icon.isNotEmpty()) {
                                                Img(src = cellData.icon)
                                            } else if (cellData.image.isNotEmpty()) {
                                                Img(src = cellData.image)
                                            } else {
                                                Text(cellData.text)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //--- Page Bar
            Div(
                attrs = {
                    style {
                        flexGrow(0)
                        flexShrink(0)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        flexWrap(FlexWrap.Wrap)
                        justifyContent(JustifyContent.Center)
                        alignItems(AlignItems.Center)
                        borderTop(
                            width = getStyleTableCaptionBorderTop().width,
                            lineStyle = getStyleTableCaptionBorderTop().style,
                            color = getStyleTableCaptionBorderTop().color,
                        )
                        setPaddings(arrStyleTablePageBarPadding)
                        backgroundColor(getColorTablePagebarBack())
                    }
                }
            ) {
                for (pageButton in alPageButton) {
                    Button(
                        attrs = {
                            style {
                                backgroundColor(
                                    if (pageButton.url.isNotEmpty()) {
                                        getColorButtonBack()
                                    } else {
                                        getColorTablePageBarCurrentBack()
                                    }
                                )
                                if (pageButton.url.isNotEmpty()) {
                                    setBorder(getStyleTablePageBarOtherBorder())
                                } else {
                                    border(width = 0.px)
                                }
                                width(getStyleTablePageButtonWidth(tableResponse.arrPageButton.size))
                                fontSize(getStyleTablePageButtonFontSize(tableResponse.arrPageButton.size))
                                padding(styleIconButtonPadding)
                                setMargins(arrStyleCommonMargin)
                                cursor("pointer")
                            }
                            onClick {
                                if (pageButton.url.isNotEmpty()) {
                                    call(pageButton.url, false)
                                }
                            }
                        }
                    ) {
                        Text(pageButton.text)
                    }
                }
            }

            //--- Popup Menu
            if (isShowPopupMenu.value) {
                Div(
                    attrs = {
                        style {
                            zIndex(Z_INDEX_TABLE_POPUP)
                            position(Position.Absolute)
                            top(20.percent)
                            bottom(
                                if (styleIsNarrowScreen) {
                                    20.percent
                                } else {
                                    10.percent
                                }
                            )
                            //"min-width" to styleMenuWidth(), - не уверен, что потребуется
                            setMenuWidth()
                            backgroundColor(getColorPopupMenuBack())
                            color(colorMenuTextDefault)
                            setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                            fontSize(arrStyleMenuFontSize[0])
                            setPaddings(arrStyleMenuStartPadding)
                            overflow("auto")
                            cursor("pointer")
                            popupMenuPosFun()
                        }
                        onMouseLeave {
                            isShowPopupMenu.value = false
                        }
                    }
                ) {
                    Menu.generateMenuBody(this@TableControl, false, arrCurPopupData.value!!, 0)
                }
            }

            getAdditionalBody()
        }
    }

    @Composable
    open fun getAdditionalBody() {
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        readHeader()
        selectorCancelUrl.value = tableResponse.selectorCancelURL

        findUrl.value = tableResponse.findURL
        findText.value = tableResponse.findText

        readAddButtons()
        readServerButtons()
        readClientButtons()
        readPageButtons()
        readTable()

        //--- установка текущей строки
        setCurrentRow(tableResponse.selectedRow)

        //--- запоминаем текущий appParam для возможной установки в виде стартовой
        root.curAppParam = appParam

        focusToCursorField(tabId)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun readHeader() {
        var tabToolTip = ""

        //--- загрузка заголовка таблицы/формы
        alTitleData.clear()
        for ((url, text) in tableResponse.arrHeader) {
            tabToolTip += (
                if (tabToolTip.isEmpty()) {
                    ""
                } else {
                    " | "
                }
                ) + text
            alTitleData.add(TitleData(url, text))
            //--- запомним последнюю кнопку заголовка в табличном режиме как кнопку отмены или возврата на уровень выше
            //butTableCancel = button - not used yet
        }
        root.setTabInfo(tabId, tableResponse.tab, tabToolTip)
    }

    private fun readAddButtons() {
        alAddButton.clear()
        for (aab in tableResponse.arrAddActionButton) {
            val icon = hmTableIcon[aab.icon] ?: ""
            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
            val tooltip = if (aab.icon.isNotBlank() && icon.isBlank()) {
                aab.icon
            } else {
                aab.tooltip
            }
            alAddButton.add(
                AddActionButtonClient(
                    tooltip = tooltip,
                    icon = icon,
                    url = aab.url
                )
            )
        }
    }

    private fun readServerButtons() {
        alServerButton.clear()

        for (sab in tableResponse.arrServerActionButton) {
            val icon = hmTableIcon[sab.icon] ?: ""
            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
            val tooltip = if (sab.icon.isNotBlank() && icon.isBlank()) {
                sab.icon
            } else {
                sab.tooltip
            }
            alServerButton.add(
                ServerActionButtonClient(
                    tooltip = tooltip,
                    icon = icon,
                    url = sab.url,
                    inNewWindow = sab.inNewWindow,
                    isForWideScreenOnly = sab.isForWideScreenOnly,
                )
            )
        }
    }

    private fun readClientButtons() {
        alClientButton.clear()
        for (cab in tableResponse.arrClientActionButton) {
            val icon = hmTableIcon[cab.icon] ?: ""
            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
            val tooltip = if (cab.icon.isNotBlank() && icon.isBlank()) {
                cab.icon
            } else {
                cab.tooltip
            }
            alClientButton.add(
                ClientActionButtonClient(
                    tooltip = tooltip,
                    icon = icon,
                    action = cab.action,
                    params = cab.params.toList(),
                    isForWideScreenOnly = cab.isForWideScreenOnly,
                )
            )
        }
    }

    private fun readPageButtons() {
        pageUpUrl.value = ""
        pageDownUrl.value = ""
        alPageButton.clear()

        var isEmptyPassed = false
        //--- вывести новую разметку страниц
        for ((url, text) in tableResponse.arrPageButton) {

            alPageButton.add(PageButton(url, text))

            if (url.isEmpty()) {
                isEmptyPassed = true
            } else {
                if (!isEmptyPassed) {
                    pageUpUrl.value = url
                }
                if (isEmptyPassed && pageDownUrl.value.isEmpty()) {
                    pageDownUrl.value = url
                }
            }
        }
    }

    private fun readTable() {
        alGridData.clear()
        //--- заголовки столбцов таблицы
        for ((index, value) in tableResponse.arrColumnCaption.withIndex()) {
            val url = value.first
            val text = value.second
            val captionCell = TableGridData(
                cellType = TableCellType.TEXT,
                cellStyleCommon = {
                    gridArea("1", "${index + 1}", "2", "${index + 2}")
                    justifySelf("stretch")
                    alignSelf(AlignSelf.Stretch)
                    //--- use toolbar bottom border instead
                    borderTop(width = 0.px, lineStyle = LineStyle.None, color = COLOR_MAIN_BACK_0)
                    borderRight(
                        width = getStyleTableCaptionBorderRight().width,
                        lineStyle = getStyleTableCaptionBorderRight().style,
                        color = getStyleTableCaptionBorderRight().color,
                    )
                    borderBottom(
                        width = getStyleTableCaptionBorderBottom().width,
                        lineStyle = getStyleTableCaptionBorderBottom().style,
                        color = getStyleTableCaptionBorderBottom().color,
                    )
                    borderLeft(
                        width = getStyleTableCaptionBorderLeft().width,
                        lineStyle = getStyleTableCaptionBorderLeft().style,
                        color = getStyleTableCaptionBorderLeft().color,
                    )
                    cursor(
                        if (url.isBlank()) {
                            "default"
                        } else {
                            "pointer"
                        }
                    )
                    display(DisplayStyle.Flex)
                    justifyContent(getStyleTableCaptionJustifyH())
                    alignItems(getStyleTableCaptionAlignH())
                    fontSize(getStyleTableCaptionFontSize())
                    fontWeight(getStyleTableCaptionFontWeight())
                    setPaddings(getStyleTableCaptionPadding())
                    //--- sticky header
                    position(Position.Sticky)
                    top(0.px)
                    //--- workaround for bug with CheckBoxes in table, which above, than typical cell, include "sticky" table headers
                    zIndex(Z_INDEX_TABLE_CAPTION)
                },
                elementStyle = {},
                //rowSpan = 1, - not used?
                backColor = getColorTableCaptionBack(),
                tooltip = if (url.isBlank()) {
                    ""
                } else {
                    "Сортировать по этому столбцу"
                },
                cellStyleAdd = {},
                textCellData = TableTextCellDataClient(text = text), // the parameter name is urgent!
                // special row number used as flag for table header row
                dataRow = -1,
            )
            captionCell.cellURL = url
            alGridData.add(captionCell)
        }

        val startRow = 1
        var maxRow = 0
        var maxCol = tableResponse.arrColumnCaption.size

        for (tc in tableResponse.arrTableCell) {
            val backColor = when (tc.backColorType.toString()) {
                TableCellBackColorType.DEFINED.toString() -> getColorFromInt(tc.backColor)
                TableCellBackColorType.GROUP_0.toString() -> colorTableGroupBack0
                TableCellBackColorType.GROUP_1.toString() -> colorTableGroupBack1
                else -> if (tc.dataRow % 2 == 0) {
                    colorTableRowBack0
                } else {
                    colorTableRowBack1
                }
            }
            val textColor = when (tc.foreColorType.toString()) {
                TableCellForeColorType.DEFINED.toString() -> getColorFromInt(tc.foreColor)
                else -> COLOR_MAIN_TEXT
            }
            val align = when (tc.cellType.toString()) {
                TableCellType.BUTTON.toString() -> {
                    JustifyContent.Center
                }

                TableCellType.CHECKBOX.toString() -> {
                    JustifyContent.Center
                }
                //--- на самом деле нет других вариантов
                //TableCellType.TEXT.toString() -> {
                else -> {
                    when (tc.align.toString()) {
                        TableCellAlign.LEFT.toString() -> JustifyContent.FlexStart
                        TableCellAlign.CENTER.toString() -> JustifyContent.Center
                        TableCellAlign.RIGHT.toString() -> JustifyContent.FlexEnd
                        else -> JustifyContent.Center
                    }
                }
            }

            val cellStyleCommon: StyleScope.() -> Unit = {
                gridArea("${startRow + tc.row + 1}", "${tc.col + 1}", "${startRow + tc.row + 1 + tc.rowSpan}", "${tc.col + 1 + tc.colSpan}")
                justifySelf("stretch")
                alignSelf(AlignSelf.Stretch)
                fontWeight(
                    if (tc.fontStyle == 0) {
                        "normal"
                    } else {
                        "bold"
                    }
                )
                padding(styleControlPadding)
            }

            var textCellData: TableTextCellDataClient? = null
            val alButtonCellData = mutableListOf<TableButtonCellDataClient>()
            val alGridCellData = mutableListOf<MutableList<TableGridCellDataClient>>()

            var cellStyleAdd: StyleScope.() -> Unit = {}
            var elementStyle: StyleScope.() -> Unit = {}

            when (tc.cellType.toString()) {
                TableCellType.CHECKBOX.toString() -> {
                    cellStyleAdd = {
                        display(DisplayStyle.Flex)
                        justifyContent(align)
                        alignItems(AlignItems.Center)
                    }
                    //--- for checkbox only
                    elementStyle = {
                        color(textColor)
                        appearance("none")
                        width(styleCheckBoxWidth)
                        height(styleCheckBoxHeight)
                        setBorder(getStyleCheckBoxBorder())
                    }
                }

                TableCellType.TEXT.toString() -> {
                    val icon = hmTableIcon[tc.textCellData.icon] ?: ""
                    //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                    val text = if (tc.textCellData.icon.isNotBlank() && icon.isBlank()) {
                        tc.textCellData.icon
                    } else {
                        tc.textCellData.text    //.replace("\n", "<br>")
                    }
                    //--- restore or finally remove word wrap?
                    //if (!tc.isWordWrap) {
                    //    text = text.replace(" ", "&nbsp;")
                    //}
                    textCellData = TableTextCellDataClient(
                        icon = icon,
                        image = tc.textCellData.image,
                        text = text,
                    )
                    cellStyleAdd = {
                        display(DisplayStyle.Flex)
                        justifyContent(align)
                        alignItems(AlignItems.Center)
                    }
                    elementStyle = {
                        color(textColor)
                        fontSize(getStyleTableTextFontSize())
                        userSelect(
                            if (getStyleIsTouchScreen()) {
                                "none"
                            } else {
                                "auto"
                            }
                        )
                    }
                }

                TableCellType.BUTTON.toString() -> {
                    tc.arrButtonCellData.forEachIndexed { index, cellData ->
                        val icon = hmTableIcon[cellData.icon] ?: ""
                        //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                        val text = if (cellData.icon.isNotBlank() && icon.isBlank()) {
                            cellData.icon
                        } else {
                            cellData.text   //.replace("\n", "<br>")
                        }
                        alButtonCellData.add(
                            TableButtonCellDataClient(
                                icon = icon,
                                image = cellData.image,
                                text = text,
                                url = cellData.url,
                                inNewWindow = cellData.inNewWindow,
                                style = {
                                    setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                    backgroundColor(getColorButtonBack())
                                    color(textColor)
                                    fontSize(styleCommonButtonFontSize)
                                    padding(styleTextButtonPadding)
                                    cursor("pointer")
                                    gridArea("${index + 1}", "1", "${index + 2}", "2")
                                    justifySelf("center")
                                    alignSelf(AlignSelf.Center)
                                }
                            )
                        )
                    }
                    if (tc.arrButtonCellData.isNotEmpty()) {
                        cellStyleAdd = {
                            display(DisplayStyle.Grid)
                            gridTemplateRows("repeat(${tc.arrButtonCellData.size},auto)")
                            gridTemplateColumns("repeat(1,auto)")
                        }
                    }
                }

                TableCellType.GRID.toString() -> {
                    tc.arrGridCellData.forEachIndexed { rowIndex, cellRow ->
                        alGridCellData.add(mutableListOf())

                        cellRow.forEachIndexed { colIndex, cellData ->
                            val icon = hmTableIcon[cellData.icon] ?: ""
                            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                            var text = if (cellData.icon.isNotBlank() && icon.isBlank()) {
                                cellData.icon
                            } else {
                                cellData.text   //.replace("\n", "<br>")
                            }
                            //--- restore or finally remove word wrap?
                            //if (!tc.isWordWrap) {
                            //    text = text.replace(" ", "&nbsp;")
                            //}
                            alGridCellData.last().add(
                                TableGridCellDataClient(
                                    icon = icon,
                                    image = cellData.image,
                                    text = text,
                                    style = {
                                        color(textColor)
                                        fontSize(getStyleTableTextFontSize())
                                        userSelect(
                                            if (getStyleIsTouchScreen()) {
                                                "none"
                                            } else {
                                                "auto"
                                            }
                                        )
                                        gridArea("${rowIndex + 1}", "${colIndex + 1}", "${rowIndex + 2}", "${colIndex + 2}")
                                        justifySelf("stretch")
                                        alignSelf(AlignSelf.Stretch)
                                        display(DisplayStyle.Flex)
                                        justifyContent(align)
                                        alignItems(AlignItems.Center)
                                        setPaddings(arrStyleTableGridCellTypePadding)
                                    }
                                )
                            )
                        }
                    }
                    if (tc.arrGridCellData.isNotEmpty()) {
                        cellStyleAdd = {
                            display(DisplayStyle.Grid)
                            gridTemplateRows("repeat(${tc.arrGridCellData.size},auto)")
                            gridTemplateColumns("repeat(${tc.arrGridCellData.first().size},auto)")
                        }
                    }
                }
            }
            alGridData.add(
                TableGridData(
                    cellType = tc.cellType,
                    cellStyleCommon = cellStyleCommon,
                    elementStyle = elementStyle,
                    //rowSpan = tc.rowSpan, - not used?
                    backColor = backColor,
                    tooltip = tc.tooltip,
                    cellStyleAdd = cellStyleAdd,
                    booleanValue = tc.booleanValue,
                    textCellData = textCellData,
                    alButtonCellData = alButtonCellData,
                    alGridCellData = alGridCellData,
                    dataRow = tc.dataRow,
                )
            )
            maxRow = max(maxRow, startRow + tc.row + tc.rowSpan)
            maxCol = max(maxCol, tc.col + tc.colSpan)
        }
        maxRow++

        gridMaxRow.value = maxRow
        gridMaxCol.value = maxCol

        alRowData.clear()
        alRowData.addAll(tableResponse.arrTableRowData)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun closeTabById() {
        root.closeTabById(tabId)
    }

    private fun doFind(isClear: Boolean) {
        if (isClear) {
            findText.value = ""
        }

        if (!isClear && !isFindTextVisible.value) {
            isFindTextVisible.value = true
        } else {
            appControl.call(AppRequest(action = findUrl.value, find = findText.value.trim()))
        }
    }

    private fun doForm() {
        //--- проверка лишней не будет
        if (currentRow.value >= 0 && alRowData[currentRow.value].formURL.isNotEmpty()) {
            call(alRowData[currentRow.value].formURL, false)
        }
    }

    private fun doGoto() {
        //--- проверка лишней не будет
        if (currentRow.value >= 0 && alRowData[currentRow.value].gotoURL.isNotEmpty()) {
            call(alRowData[currentRow.value].gotoURL, alRowData[currentRow.value].itGotoURLInNewWindow)
        }
    }

    private fun doPopup() {
        //--- проверка лишней не будет
        if (currentRow.value >= 0 && alRowData[currentRow.value].alPopupData.isNotEmpty()) {
            showPopupMenu(currentRow.value, null)
        }
    }

    private fun setCurrentRow(rowNo: Int) {
        isFormButtonVisible.value = rowNo >= 0 && alRowData[rowNo].formURL.isNotEmpty()
        isGotoButtonVisible.value = rowNo >= 0 && alRowData[rowNo].gotoURL.isNotEmpty()
        isPopupButtonVisible.value = rowNo >= 0 && alRowData[rowNo].alPopupData.isNotEmpty()

        currentRow.value = rowNo
        isShowPopupMenu.value = false

        focusToCursorField(tabId)
    }

//        "doKeyUp" to {
//            var currentRow = that().currentRow.unsafeCast<Int>()
//            if (currentRow > 0) {
//                currentRow--
//                setCurrentRow(that(), currentRow)
//            }
//        },
//        "doKeyDown" to {
//            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
//            var currentRow = that().currentRow.unsafeCast<Int>()
//            if (currentRow < arrRowData.lastIndex) {
//                currentRow++
//                setCurrentRow(that(), currentRow)
//            }
//        },
//        "doKeyHome" to {
//            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
//            if (arrRowData.isNotEmpty()) {
//                setCurrentRow(that(), 0)
//            }
//        },
//        "doKeyEnd" to {
//            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
//            if (arrRowData.isNotEmpty()) {
//                setCurrentRow(that(), arrRowData.lastIndex)
//            }
//        },
//        "doKeyPageUp" to {
//            val pageUpUrl = that().pageUpUrl.unsafeCast<String>()
//            if (pageUpUrl.isNotEmpty()) {
//                that().invoke(pageUpUrl, false)
//            }
//        },
//        "doKeyPageDown" to {
//            val pageDownUrl = that().pageDownUrl.unsafeCast<String>()
//            if (pageDownUrl.isNotEmpty()) {
//                that().invoke(pageDownUrl, false)
//            }
//        },
//        "doKeyEnter" to {
//            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
//            val currentRow = that().currentRow.unsafeCast<Int>()
//            if (currentRow >= 0 && currentRow < arrRowData.size) {
//                val curRowData = arrRowData[currentRow]
//                if (curRowData.rowURL.isNotEmpty()) {
//                    that().invoke(curRowData.rowURL, curRowData.itRowURLInNewWindow)
//                }
//            }
//        },
//        "doKeyEsc" to {
//            val selectorCancelURL = that().selectorCancelURL.unsafeCast<String>()
//            if (selectorCancelURL.isNotEmpty()) {
//                that().invoke(selectorCancelURL, false)
//            }
//        },

    protected fun call(newAppParam: String, inNewWindow: Boolean) {
        if (inNewWindow) {
            root.openTab(newAppParam)
        } else {
            appControl.call(AppRequest(action = newAppParam))
        }
    }

    private fun clientAction(action: String, params: List<Pair<String, String>>) {
        tableClientActionFun(action, params, this)
    }

    private fun showPopupMenu(row: Int, mouseEvent: SyntheticMouseEvent?) {
        //--- чтобы строчка выделялась и по правой кнопке мыши тоже
        setCurrentRow(row)

        val mouseX = mouseEvent?.pageX ?: (window.innerWidth.toDouble() / 3)

        if (alRowData[row].alPopupData.isNotEmpty()) {
            //--- в данной ситуации clientX/Y == pageX/Y, offsetX/Y идёт от текущего элемента (ячейки таблицы), screenX/Y - от начала экрана
            popupMenuPosFun = if (styleIsNarrowScreen) {
                {
                    left(5.percent)
                }
            } else if (mouseX <= window.innerWidth / 2) {
                {
                    left(mouseX.px)
                }
            } else {
                {
                    right((window.innerWidth - mouseX).px)
                }
            }
            convertPopupMenuData(alRowData[row].alPopupData)
            isShowPopupMenu.value = true
        } else {
            isShowPopupMenu.value = false
        }
    }

    override fun menuClick(url: String, inNewWindow: Boolean) {
        isShowPopupMenu.value = false
        call(url, inNewWindow)
    }

    private fun convertPopupMenuData(arrMenuData: Array<TablePopupData>) {
        val alCurPopupData = mutableListOf<MenuData>()

        var i = 0
        while (i < arrMenuData.size) {
            val menuData = arrMenuData[i]

            if (menuData.group.isEmpty()) {
                alCurPopupData.add(
                    MenuData(
                        url = menuData.url,
                        text = menuData.text,
                        arrSubMenu = null,
                        inNewWindow = menuData.inNewWindow
                    )
                )
                i++
            } else {
                val groupName = menuData.group

                val alPopupSubMenuData = mutableListOf<MenuData>()
                while (i < arrMenuData.size) {
                    val subMenuData = arrMenuData[i]
                    if (subMenuData.group.isEmpty() || subMenuData.group != groupName) {
                        break
                    }

                    alPopupSubMenuData.add(
                        MenuData(
                            url = subMenuData.url,
                            text = subMenuData.text,
                            arrSubMenu = null,
                            inNewWindow = subMenuData.inNewWindow
                        )
                    )
                    i++
                }
                alCurPopupData.add(
                    MenuData(
                        url = "",
                        text = groupName,
                        arrSubMenu = alPopupSubMenuData.toTypedArray(),
                        inNewWindow = false
                    )
                )
            }
        }

        arrCurPopupData.value = alCurPopupData.toTypedArray()
    }

    private fun focusToCursorField(tabId: Int) {
        val element = document.getElementById("table_cursor_$tabId")
        if (element is HTMLElement) {
            element.focus()
        }
    }

}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private class AddActionButtonClient(
    val icon: String,
    val tooltip: String,
    val url: String
)

private class ServerActionButtonClient(
    val icon: String,
    val tooltip: String,
    val url: String,
    val inNewWindow: Boolean,
    val isForWideScreenOnly: Boolean,
)

private class ClientActionButtonClient(
    val isForWideScreenOnly: Boolean,
    val icon: String,
    val tooltip: String,
    val action: String,
    val params: List<Pair<String, String>>,
)

private class PageButton(val url: String, val text: String)

private class TableGridData(
    val cellType: TableCellType,
    val cellStyleCommon: StyleScope.() -> Unit,
    //val rowSpan: Int, - not used?
    val backColor: CSSColorValue,
    val elementStyle: StyleScope.() -> Unit,
    val tooltip: String,

    val cellStyleAdd: StyleScope.() -> Unit,

    //--- CHECKBOX
    val booleanValue: Boolean? = null,

    //--- TEXT
    val textCellData: TableTextCellDataClient? = null,

    //--- BUTTON
    val alButtonCellData: List<TableButtonCellDataClient>? = null,

    //--- общие данные для BUTTON
    val alGridCellData: List<List<TableGridCellDataClient>>? = null,

    //--- для работы с row data popup menu
    val dataRow: Int,

    //var minWidth = 0 - not used yet
) {
    //--- для работы caption-click
    var cellURL = ""
}

private class TableTextCellDataClient(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
)

private class TableButtonCellDataClient(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
    val url: String = "",
    val inNewWindow: Boolean = false,
    val style: StyleScope.() -> Unit
)

private class TableGridCellDataClient(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
    //--- not used yet
    //val url: String = "",
    //val inNewWindow: Boolean = false,
    val style: StyleScope.() -> Unit
)

