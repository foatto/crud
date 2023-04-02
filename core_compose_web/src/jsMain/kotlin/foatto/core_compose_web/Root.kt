package foatto.core_compose_web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import foatto.core.app.*
import foatto.core_compose.model.MenuDataClient
import foatto.core_compose_web.control.TableControl.Companion.hmTableIcon
import foatto.core_compose.model.TabInfo
import foatto.core_compose_web.style.*
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.borderBottom
import org.jetbrains.compose.web.css.properties.borderLeft
import org.jetbrains.compose.web.css.properties.borderRight
import org.jetbrains.compose.web.css.properties.borderTop
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLSpanElement

//--- Element Ids ----------------------------------------------------------------------------------------------------------------------------------------------

const val TOP_BAR_ID = "top_bar_id"
const val MENU_CLOSER_BUTTON_ID = "menu_closer_button_id"

//--- WAIT -----------------------------------------------------------------------------------------------------------------------------------------------------

var colorWaitBack: CSSColorValue = hsla(0, 0, 100, 0.7)
var colorWaitLoader0: CSSColorValue = hsl(60, 100, 80)
var colorWaitLoader1: CSSColorValue = hsl(60, 100, 85)
var colorWaitLoader2: CSSColorValue = hsl(60, 100, 90)
var colorWaitLoader3: CSSColorValue = hsl(60, 100, 95)

//--- DIALOG ---------------------------------------------------------------------------------------------------------------------------------------------------

var colorDialogBack: CSSColorValue = hsla(0, 0, 0, 0.95)
val getColorDialogBorder: () -> CSSColorValue = { colorMainBorder }
val getColorDialogBackCenter: () -> CSSColorValue = { colorMainBack1 }
private val getColorDialogButtonBack: () -> CSSColorValue = { getColorButtonBack() }
private val getColorDialogButtonBorder: () -> CSSColorValue = { colorMainBorder }

val styleDialogTextFontSize = (COMMON_FONT_SIZE.value + 2).cssRem

val styleDialogCellPadding: CSSSize = 1.0.cssRem
private val arrStyleDialogControlPadding: Array<CSSSize> = arrayOf(0.4.cssRem, 0.cssRem, 0.4.cssRem, 0.cssRem)
private val arrStyleDialogButtonPadding: Array<CSSSize> = arrayOf(
    1.0.cssRem,
    (if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48).cssRem,
    1.0.cssRem,
    (if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48).cssRem,
)
private val arrStyleDialogButtonMargin: Array<CSSSize> = arrayOf(1.0.cssRem, 0.cssRem, 0.cssRem, 0.cssRem)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

const val LOCAL_STORAGE_APP_PARAM: String = "app_param"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

var getMenu: (
    root: Root,
    arrMenuData: Array<MenuDataClient>,
) -> Menu = { root: Root,
              arrMenuData: Array<MenuDataClient> ->
    Menu(root, arrMenuData)
}

var getAppControl: (
    root: Root,
    startAppParam: String,
    tabId: Int,
) -> AppControl = { root: Root,
                    startAppParam: String,
                    tabId: Int ->
    AppControl(root, startAppParam, tabId)
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

open class Root {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    val menuBar: MutableState<Menu?> = mutableStateOf(null)

    val isShowMainMenu: MutableState<Boolean> = mutableStateOf(false)

    private val alControl = mutableStateListOf<AppControl>()
    private val waitCount = mutableStateOf(0)

    var dialogActionFun: () -> Unit = {}
    var dialogQuestion: MutableState<String> = mutableStateOf("")
    var showDialogCancel: MutableState<Boolean> = mutableStateOf(false)
    var showDialog: MutableState<Boolean> = mutableStateOf(false)
    private val dialogButtonOkText = mutableStateOf("OK")
    private val dialogButtonCancelText = mutableStateOf("Отмена")

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    var currentUserName: String = ""
    var timeOffset: Int = 0
    var curAppParam: String = ""

    private lateinit var tabPanel: TabPanel

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun init() {
        //--- styleIsHiddenMenuBar may be redefined in derived classes
        isShowMainMenu.value = !(styleIsNarrowScreen || styleIsHiddenMenuBar)

        hmTableIcon[ICON_NAME_SELECT] = "/web/images/ic_reply_${getStyleIconNameSuffix()}.png"

        hmTableIcon[ICON_NAME_ADD_FOLDER] = "/web/images/ic_create_new_folder_${getStyleIconNameSuffix()}.png"
        hmTableIcon[ICON_NAME_ADD_ITEM] = "/web/images/ic_add_${getStyleIconNameSuffix()}.png"

        //--- подразделение
        hmTableIcon[ICON_NAME_DIVISION] = "/web/images/ic_folder_shared_${getStyleIconNameSuffix()}.png"
        //--- руководитель
        hmTableIcon[ICON_NAME_BOSS] = "/web/images/ic_account_box_${getStyleIconNameSuffix()}.png"
        //--- работник
        hmTableIcon[ICON_NAME_WORKER] = "/web/images/ic_account_circle_${getStyleIconNameSuffix()}.png"

        //--- подраздел
        hmTableIcon[ICON_NAME_FOLDER] = "/web/images/ic_folder_open_${getStyleIconNameSuffix()}.png"

        //--- печать
        hmTableIcon[ICON_NAME_PRINT] = "/web/images/ic_print_${getStyleIconNameSuffix()}.png"

        tabPanel = TabPanel(this)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    fun getBody() {
        if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
            getMainContainer()
        } else {
            //--- участок дизайна для широкого экрана с постоянным меню слева
            Div(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        width(100.percent)
                        height(100.percent)
                    }
                }
            ) {
                if (menuBar.value != null && isShowMainMenu.value) {
                    menuBar.value?.getBody()
                }
                Div(
                    attrs = {
                        id(MENU_CLOSER_BUTTON_ID)
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            justifyContent(JustifyContent.Center)
                            alignItems(AlignItems.Center)
                            getColorMenuCloserBack()
                        }
                    }
                ) {
                    Button(
                        attrs = {
                            style {
                                backgroundColor(colorMenuCloserButtonBack)
                                color(colorMenuCloserButtonText)
                                border {
                                    width(0.px)
                                }
                                height(8.cssRem)
                                setPaddings(arrayOf(0.cssRem, 0.1.cssRem, 0.cssRem, 0.1.cssRem))
                                fontSize(1.0.cssRem)
                                fontWeight("bold")
                            }
                            onClick {
                                isShowMainMenu.value = !isShowMainMenu.value
                            }
                        }
                    ) {
                        Text(if (isShowMainMenu.value) "<" else ">")
                    }
                }
                getMainContainer()
            }
        }
    }

    @Composable
    fun getMainContainer() {
        Div(
            attrs = {
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    width(100.percent)
                    height(100.percent)
                }
            }
        ) {
            if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
            } else {
                getTopBar()
            }

            tabPanel.getBody()

            alControl.forEachIndexed { tabIndex, control ->
                if (tabPanel.currentTabIndex.value == tabIndex) {
                    control.getBody()
                }
            }

            if (waitCount.value > 0) {
                Div(
                    attrs = {
                        style {
                            position(Position.Fixed)
                            top(0.px)
                            left(0.px)
                            width(100.percent)
                            height(100.percent)
                            zIndex(Z_INDEX_WAIT)
                            backgroundColor(colorWaitBack)
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
                                width(16.cssRem)
                                height(16.cssRem)
                                zIndex(Z_INDEX_LOADER)
                                borderTop(width = 2.cssRem, lineStyle = LineStyle.Solid, color = colorWaitLoader3)
                                borderRight(width = 2.cssRem, lineStyle = LineStyle.Solid, color = colorWaitLoader2)
                                borderBottom(width = 2.cssRem, lineStyle = LineStyle.Solid, color = colorWaitLoader1)
                                borderLeft(width = 2.cssRem, lineStyle = LineStyle.Solid, color = colorWaitLoader0)
                                borderRadius(50.percent)
                                animation("spin") {
                                    duration(2.s)
                                    timingFunction(AnimationTimingFunction.Linear)
                                    iterationCount(null)
                                }
                            }
                        }
                    )
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

            if (showDialog.value) {
                Div(
                    attrs = {
                        style {
                            position(Position.Fixed)
                            top(0.px)
                            left(0.px)
                            width(100.percent)
                            height(100.percent)
                            zIndex(Z_INDEX_DIALOG)
                            backgroundColor(colorDialogBack)
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
                                padding(styleDialogCellPadding)
                                setBorder(color = getColorDialogBorder(), radius = styleFormBorderRadius)
                                backgroundColor(getColorDialogBackCenter())
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                alignItems(AlignItems.Center)
                            }
                        }
                    ) {
                        Div(
                            attrs = {
                                style {
                                    alignSelf(AlignSelf.Center)
                                    fontSize(styleDialogTextFontSize)
                                    fontWeight("bold")
                                    color(COLOR_MAIN_TEXT)
                                }
                            }
                        ) {
                            Text(dialogQuestion.value)
                        }
                        Br()
                        Div(
                            attrs = {
                                style {
                                    fontSize(styleControlTextFontSize)
                                    setPaddings(arrStyleDialogControlPadding)
                                }
                            }
                        ) {
                            Button(
                                attrs = {
                                    style {
                                        backgroundColor(getColorDialogButtonBack())
                                        setBorder(color = getColorDialogButtonBorder(), radius = styleButtonBorderRadius)
                                        fontSize(styleCommonButtonFontSize)
                                        setPaddings(arrStyleDialogButtonPadding)
                                        setMargins(arrStyleDialogButtonMargin)
                                        cursor("pointer")
                                    }
                                    onClick {
                                        dialogOk()
                                    }
                                }
                            ) {
                                Text(dialogButtonOkText.value)
                            }
                            getPseudoNbsp(1)
                            if (showDialogCancel.value) {
                                Button(
                                    attrs = {
                                        style {
                                            backgroundColor(getColorDialogButtonBack())
                                            setBorder(color = getColorDialogButtonBorder(), radius = styleButtonBorderRadius)
                                            fontSize(styleCommonButtonFontSize)
                                            setPaddings(arrStyleDialogButtonPadding)
                                            setMargins(arrStyleDialogButtonMargin)
                                            cursor("pointer")
                                        }
                                        onClick {
                                            dialogCancel()
                                        }
                                    }
                                ) {
                                    Text(dialogButtonCancelText.value)
                                }
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
    }

    @Composable
    open fun getTopBar() {
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun start() {
        val localStartAppParam = localStorage.getItem(LOCAL_STORAGE_APP_PARAM)
        addTabComp(
            if (localStartAppParam.isNullOrBlank()) {
                (document.getElementById("startAppParam") as HTMLSpanElement).innerText
            } else {
                localStartAppParam
            }
        )
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun openTab(newAppParam: String) {
        //--- файловое урло
        if (newAppParam[0] == '/') {
            window.open(newAppParam)
        } else {
            addTabComp(newAppParam)
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun addTabComp(appParam: String) {
        tabPanel.lastTabId++

        val appControl = getAppControl(this, appParam, tabPanel.lastTabId)

        tabPanel.alTabInfo += TabInfo(tabPanel.lastTabId, listOf(), "")
        alControl += appControl

        tabPanel.currentTabIndex.value = tabPanel.alTabInfo.lastIndex

        appControl.start()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setTabInfo(tabId: Int, tabText: String, tabToolTip: String) {
        tabPanel.alTabInfo.find { tabInfo ->
            tabInfo.id == tabId
        }?.let { tabInfo ->
            tabInfo.alText = tabText.split('\n').filter { tabWord ->
                tabWord.isNotBlank()
            }.map { tabWord ->
                if (tabWord.length > getStyleTabComboTextLen()) {
                    tabWord.substring(0, getStyleTabComboTextLen()) + "..."
                } else {
                    tabWord
                }
            }
            tabInfo.tooltip = tabToolTip
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun closeTabById(tabId: Int) {
        val indexForClose = tabPanel.alTabInfo.indexOfFirst { tabInfo ->
            tabInfo.id == tabId
        }
        closeTabByIndex(indexForClose)
    }

    fun closeTabByIndex(indexForClose: Int) {
        //--- for last tab removing case
        if (tabPanel.currentTabIndex.value == tabPanel.alTabInfo.lastIndex) {
            tabPanel.currentTabIndex.value--
        }

        alControl.removeAt(indexForClose)
        tabPanel.alTabInfo.removeAt(indexForClose)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setMenuBarData(arrMenuData: Array<MenuDataClient>) {
        menuBar.value = getMenu(this, arrMenuData)
        tabPanel.isTabPanelVisible.value = true
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setWait(isWait: Boolean) {
        waitCount.value = waitCount.value + (if (isWait) 1 else -1)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun dialogOk() {
        showDialog.value = false
        dialogActionFun()
    }

    private fun dialogCancel() {
        showDialog.value = false
    }
}
