package foatto.core_compose_web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.core_compose_web.style.*
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

//--- Tab style ---

var colorTabPanelBack: CSSColorValue = COLOR_MAIN_BACK_0
var arrStyleTabPanelPadding: Array<CSSSize> = arrayOf(menuTabPadMar, menuTabPadMar, 0.cssRem, menuTabPadMar)

//--- tab-combo - только на устройствах с узким экраном
private val tabComboMargin = menuTabPadMar

fun getStyleTabComboTextLen(): Int = scaledScreenWidth / (if (styleIsNarrowScreen) 16 else 64)
private val styleTabComboFontSize = COMMON_FONT_SIZE
private val styleTabComboPadding = 0.55.cssRem
private fun getStyleTabComboMargins(): Array<CSSSize> = arrayOf(0.cssRem, 0.cssRem, tabComboMargin, 0.cssRem)
private fun getStyleTabCloserButtonMargins(): Array<CSSSize> = arrayOf(0.cssRem, 0.cssRem, tabComboMargin, tabComboMargin)

var getColorTabCurrentBack: () -> CSSColorValue = { colorMainBack1 }
var arrStyleTabCurrentTitleBorder: Array<CSSSize> = arrayOf(1.px, 0.px, 0.px, 1.px)
var arrStyleTabCurrentCloserBorderWidth: Array<CSSSize> = arrayOf(1.px, 1.px, 1.px, 0.px)
var arrStyleTabCurrentCloserBorderColor: Array<CSSColorValue> = arrayOf(colorMainBorder, colorMainBorder, colorMainBack1, colorMainBorder)

private var colorTabOtherBack: () -> CSSColorValue = { colorMainBack2 }
private var arrStyleTabOtherTitleBorder: Array<CSSSize> = arrayOf(1.px, 0.px, 1.px, 1.px)
private var arrStyleTabOtherCloserBorder: Array<CSSSize> = arrayOf(1.px, 1.px, 1.px, 0.px)

private var arrStyleTabCurrentTitlePadding: Array<CSSSize> = arrayOf(0.7.cssRem, 0.6.cssRem, 0.7.cssRem, 0.6.cssRem)
private var arrStyleTabOtherTitlePadding: Array<CSSSize> = arrayOf(0.7.cssRem, 0.6.cssRem, 0.7.cssRem, 0.6.cssRem)

private var arrStyleTabCurrentCloserPadding: Array<CSSSize> = arrayOf(
    (if (screenDPR <= 1.0) 1.4 else 1.2).cssRem,
    0.4.cssRem,
    (if (screenDPR <= 1.0) 1.4 else 1.2).cssRem,
    0.4.cssRem,
)
private var arrStyleTabOtherCloserPadding: Array<CSSSize> = arrayOf(
    (if (screenDPR <= 1.0) 1.4 else 1.2).cssRem,
    0.4.cssRem,
    (if (screenDPR <= 1.0) 1.4 else 1.2).cssRem,
    0.4.cssRem,
)

private var styleTabButtonFontSize = COMMON_FONT_SIZE

//--- Tab Panel ---

class TabPanel(
    private val root: Root,
    private val styleIsHiddenMenuBar: Boolean,
) {

    //--- Tab state ---

    val isTabPanelVisible: MutableState<Boolean> = mutableStateOf(false)
    val currentTabIndex: MutableState<Int> = mutableStateOf(0)
    val alTabInfo: SnapshotStateList<TabInfo> = mutableStateListOf()

    //--- Tab non-state variables ---

    var lastTabId: Int = 0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    fun getBody() {
        Div(
            // id="tab_panel"
            attrs = {
                style {
                    display(
                        if (isTabPanelVisible.value) {
                            DisplayStyle.Flex
                        } else {
                            DisplayStyle.None
                        }
                    )
                    flexDirection(FlexDirection.Row)
                    flexWrap(FlexWrap.Wrap)
                    flexGrow(0)
                    flexShrink(0)
                    justifyContent(
                        if (!styleIsNarrowScreen) {
                            JustifyContent.FlexStart
                        } else {
                            JustifyContent.SpaceBetween
                        }

                    )
                    //--- необязательно - пусть лучше по высоте равны кнопке меню
                    //"align-items" to "flex-end",            // прижимаем вкладки к нижнему контролу
                    backgroundColor(colorTabPanelBack)
                    setPaddings(arrStyleTabPanelPadding)
                }
            }
        ) {
            if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
                root.menuBar.value?.getBody()
            }

            if (styleIsNarrowScreen) {
                Select(
                    attrs = {
                        style {
                            flexGrow(1)
                            flexShrink(1)
                            backgroundColor(colorButtonBack)
                            setBorder(color = colorMainBorder, radius = styleFormBorderRadius)
                            fontSize(styleTabComboFontSize)
                            padding(styleTabComboPadding)
                            setMargins(getStyleTabComboMargins())
                        }
                    }
                ) {
                    alTabInfo.forEachIndexed { tabIndex, tabInfo ->
                        Option(
                            value = tabIndex.toString(),
                            attrs = {
                                if (tabIndex == currentTabIndex.value) {
                                    selected()
                                }
                            }
                        ) {
                            Text(tabInfo.alText.firstOrNull() ?: "(нет данных)")
                        }
                    }
                }
                if (alTabInfo.isNotEmpty()) {
                    Img(
                        src = "/web/images/ic_close_black_48dp.png",
                        attrs = {
                            style {
                                flexGrow(0)
                                flexShrink(0)
                                alignSelf(AlignSelf.FlexStart)  //"flex-end" - сдвигает в правый нижний угол
                                backgroundColor(colorButtonBack)
                                setBorder(color = colorMainBorder, radius = styleFormBorderRadius)
                                padding(styleIconButtonPadding)
                                setMargins(getStyleTabCloserButtonMargins())
                                cursor("pointer")
                            }
                            title("Закрыть вкладку")
                            onClick {
                                root.closeTabByIndex(currentTabIndex.value)
                            }
                        }
                    )
                }
            } else {
                alTabInfo.forEachIndexed { tabIndex, tabInfo ->
                    Button(
                        attrs = {
                            style {
                                backgroundColor(
                                    if (currentTabIndex.value == tabIndex) {
                                        getColorTabCurrentBack()
                                    } else {
                                        colorTabOtherBack()
                                    }
                                )
                                setBorder(
                                    color = colorMainBorder,
                                    arrWidth = if (currentTabIndex.value == tabIndex) {
                                        arrStyleTabCurrentTitleBorder
                                    } else {
                                        arrStyleTabOtherTitleBorder
                                    },
                                    arrRadius = arrayOf(styleFormBorderRadius, 0.cssRem, 0.cssRem, 0.cssRem)
                                )
                                fontSize(styleTabButtonFontSize)
                                setPaddings(
                                    if (currentTabIndex.value == tabIndex) {
                                        arrStyleTabCurrentTitlePadding
                                    } else {
                                        arrStyleTabOtherTitlePadding
                                    }
                                )
                                cursor("pointer")
                            }
                            title(tabInfo.tooltip)
                            onClick {
                                currentTabIndex.value = tabIndex
                            }
                        }
                    ) {
                        tabInfo.alText.forEachIndexed { textIndex, text ->
                            Span(
                                attrs = {
                                    style {
                                        fontWeight(
                                            if (currentTabIndex.value == tabIndex && textIndex == 0) {
                                                "bold"
                                            } else {
                                                "normal"
                                            }
                                        )
                                    }
                                }
                            ) {
                                Text(text)
                                Br()
                            }
                        }
                    }
                    Img(
                        src = "/web/images/ic_close_black_16dp.png",
                        attrs = {
                            style {
                                width(16.px)
                                height(16.px)
                                backgroundColor(
                                    if (currentTabIndex.value == tabIndex) {
                                        getColorTabCurrentBack()
                                    } else {
                                        colorTabOtherBack()
                                    }
                                )
                                //--- переключаемые стили должны заменять друг друга полностью
                                if (currentTabIndex.value == tabIndex) {
                                    borderTop(
                                        width = arrStyleTabCurrentCloserBorderWidth[0],
                                        lineStyle = LineStyle.Solid,
                                        color = arrStyleTabCurrentCloserBorderColor[0],
                                    )
                                    borderRight(
                                        width = arrStyleTabCurrentCloserBorderWidth[1],
                                        lineStyle = LineStyle.Solid,
                                        color = arrStyleTabCurrentCloserBorderColor[1],
                                    )
                                    borderBottom(
                                        width = arrStyleTabCurrentCloserBorderWidth[2],
                                        lineStyle = LineStyle.Solid,
                                        color = arrStyleTabCurrentCloserBorderColor[2],
                                    )
                                    borderLeft(
                                        width = arrStyleTabCurrentCloserBorderWidth[3],
                                        lineStyle = LineStyle.Solid,
                                        color = arrStyleTabCurrentCloserBorderColor[3],
                                    )
                                } else {
                                    borderTop(
                                        width = arrStyleTabOtherCloserBorder[0],
                                        lineStyle = LineStyle.Solid,
                                        color = colorMainBorder,
                                    )
                                    borderRight(
                                        width = arrStyleTabOtherCloserBorder[1],
                                        lineStyle = LineStyle.Solid,
                                        color = colorMainBorder,
                                    )
                                    borderBottom(
                                        width = arrStyleTabOtherCloserBorder[2],
                                        lineStyle = LineStyle.Solid,
                                        color = colorMainBorder,
                                    )
                                    borderLeft(
                                        width = arrStyleTabOtherCloserBorder[3],
                                        lineStyle = LineStyle.Solid,
                                        color = colorMainBorder,
                                    )
                                }
                                borderRadius(
                                    topLeft = 0.cssRem,
                                    topRight = styleFormBorderRadius,
                                    bottomRight = 0.cssRem,
                                    bottomLeft = 0.cssRem,
                                )
                                setPaddings(
                                    if (currentTabIndex.value == tabIndex) {
                                        arrStyleTabCurrentCloserPadding
                                    } else {
                                        arrStyleTabOtherCloserPadding
                                    }
                                )
                                cursor("pointer")
                            }
                            title("Закрыть вкладку")
                            onClick {
                                root.closeTabByIndex(tabIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}

class TabInfo(
    val id: Int,
    var alText: List<String>,
    var tooltip: String,
)
