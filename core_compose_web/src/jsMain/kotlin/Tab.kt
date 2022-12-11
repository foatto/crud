import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

//--- Tab state ---

var currentTabIndex = mutableStateOf(0)
val alTabInfo = mutableStateListOf<TabInfo>()
val alTabComp = mutableStateListOf<TabComp>()

class TabInfo(
    val id: Int,
    var arrText: Array<String>, // try List<String> in Web, may don't work
    var tooltip: String,
)
class TabComp(val comp: AppControl)

//--- Tab non-state variables ---

var lastTabId = 0

//--- Tab style ---

var colorTabPanelBack = COLOR_MAIN_BACK_0
var styleTabPanelPaddingTop = menuTabPadMar
var styleTabPanelPaddingRight = menuTabPadMar
var styleTabPanelPaddingBottom = 0
var styleTabPanelPaddingLeft = menuTabPadMar

////--- tab-combo - только на устройствах с узким экраном
//private val tabComboMargin = menuTabPadMar
//fun styleTabComboTextLen() = scaledScreenWidth / (if (styleIsNarrowScreen) 16 else 64)
val styleTabComboFontSize = COMMON_FONT_SIZE
val styleTabComboPadding = 0.55
//fun styleTabComboMargin() = "0 0 ${tabComboMargin}rem 0"
//fun styleTabCloserButtonMargin() = "0 0 ${tabComboMargin}rem ${tabComboMargin}rem"

var colorTabCurrentBack: () -> String = { colorMainBack1 }
var styleTabCurrentTitleBorderTop: Int = 1
var styleTabCurrentTitleBorderRight: Int = 0
var styleTabCurrentTitleBorderBottom: Int = 0
var styleTabCurrentTitleBorderLeft: Int = 1
var styleTabCurrentCloserBorderTop: Int = 1
var styleTabCurrentCloserBorderRight: Int = 1
var styleTabCurrentCloserBorderBottom: Int = 1    // { "1px solid $colorMainBack1" }
var styleTabCurrentCloserBorderLeft: Int = 0

var colorTabOtherBack: () -> String = { colorMainBack2 }
var styleTabOtherTitleBorderTop: Int = 1
var styleTabOtherTitleBorderRight: Int = 0
var styleTabOtherTitleBorderBottom: Int = 1
var styleTabOtherTitleBorderLeft: Int = 1
var styleTabOtherCloserBorderTop: Int = 1
var styleTabOtherCloserBorderRight: Int = 1
var styleTabOtherCloserBorderBottom: Int = 1
var styleTabOtherCloserBorderLeft: Int = 0
    
var styleTabCurrentTitlePaddingTop = 0.7
var styleTabCurrentTitlePaddingRight = 0.6
var styleTabCurrentTitlePaddingBottom = 0.7
var styleTabCurrentTitlePaddingLeft = 0.6
var styleTabOtherTitlePaddingTop = 0.7
var styleTabOtherTitlePaddingRight = 0.6
var styleTabOtherTitlePaddingBottom = 0.7
var styleTabOtherTitlePaddingLeft = 0.6

var styleTabCurrentCloserPaddingTop = if (screenDPR <= 1.0) 1.4 else 1.2
var styleTabCurrentCloserPaddingRight = 0.4
var styleTabCurrentCloserPaddingBottom = if (screenDPR <= 1.0) 1.4 else 1.2
var styleTabCurrentCloserPaddingLeft = 0.4
var styleTabOtherCloserPaddingTop = if (screenDPR <= 1.0) 1.4 else 1.2
var styleTabOtherCloserPaddingRight = 0.4
var styleTabOtherCloserPaddingBottom = if (screenDPR <= 1.0) 1.4 else 1.2
var styleTabOtherCloserPaddingLeft = 0.4

var styleTabButtonFontSize = COMMON_FONT_SIZE

//--- Tab Panel ---

class TabPanel(
    val styleIsHiddenMenuBar: Boolean,
) {

    @Composable
    fun getTabPanel() {
        Div(
            // id="tab_panel"
            attrs = {
                style {
                    display(DisplayStyle.Flex)          // v-bind:style="[ { 'display' : ( isTabPanelVisible ? 'flex' : 'none' ) } ]
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
                    background(colorTabPanelBack)
                    paddingTop(styleTabPanelPaddingTop.cssRem)
                    paddingRight(styleTabPanelPaddingRight.cssRem)
                    paddingBottom(styleTabPanelPaddingBottom.cssRem)
                    paddingLeft(styleTabPanelPaddingLeft.cssRem)
                }
            }
        ) {

//                if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
//                    """
//                        <component v-if="menuBar" v-bind:is="menuBar"></component>
//                    """
//                } else {
//                    ""
//                } +

            if (styleIsNarrowScreen) {
//                        <select v-model="currentTabIndex"
                Select(
                    attrs = {
                        style {
                            flexGrow(1)
                            flexShrink(1)
                            background(colorButtonBack())
                            border {
                                color(colorMainBorder())
                                style(LineStyle.Solid)
                                width(1.px)
                            }
                            borderRadius(styleFormBorderRadius.cssRem)
                            fontSize(styleTabComboFontSize.cssRem)
                            padding(styleTabComboPadding.cssRem)
//                        "margin" to styleTabComboMargin()
                        }
                    }
                ) {
                    alTabInfo.forEachIndexed { tabIndex, tabInfo ->
                        Option(
                            value = tabIndex.toString(),
//                            attrs = {
//                                selected()
//                            }
                        ) {
                            Text(tabInfo.arrText.firstOrNull() ?: "(нет данных)")
                        }
                    }
                }
                Img(
//                                 v-show="arrTabInfo.length > 0"
                    src="/web/images/ic_close_black_48dp.png",
                    attrs = {
                        style {
                            flexGrow(0)
                            flexShrink(0)
                            alignSelf(AlignSelf.FlexStart)  //"flex-end" - сдвигает в правый нижний угол
                            background(colorButtonBack())
                            border {
                                color(colorMainBorder())
                                style(LineStyle.Solid)
                                width(1.px)
                            }
                            borderRadius(styleFormBorderRadius.cssRem)
                            padding(styleIconButtonPadding.cssRem)

//                        "margin" to styleTabCloserButtonMargin(),

                            cursor("pointer")
                        }
                        title("Закрыть вкладку")
                        onClick {
//                                closeTabByIndex(tabIndex)
                        }
                    }
                )
            } else {
                alTabInfo.forEachIndexed { tabIndex, tabInfo ->
                    Button(
                        // v-show="tab.arrText"
                        attrs = {
                            style {
                                background(
                                    if (currentTabIndex.value == tabIndex) {
                                        colorTabCurrentBack()
                                    } else {
                                        colorTabOtherBack()
                                    }
                                )
                                border {
                                    color(colorMainBorder())
                                    style(LineStyle.Solid)
                                    if (currentTabIndex.value == tabIndex) {
                                        top(styleTabCurrentTitleBorderTop.cssRem)
                                        right(styleTabCurrentTitleBorderRight.cssRem)
                                        bottom(styleTabCurrentTitleBorderBottom.cssRem)
                                        left(styleTabCurrentTitleBorderLeft.cssRem)
                                    } else {
                                        top(styleTabOtherTitleBorderTop.cssRem)
                                        right(styleTabOtherTitleBorderRight.cssRem)
                                        bottom(styleTabOtherTitleBorderBottom.cssRem)
                                        left(styleTabOtherTitleBorderLeft.cssRem)
                                    }
                                }
                                borderRadius(
                                    topLeft = styleFormBorderRadius.cssRem,
                                    topRight = 0.cssRem,
                                    bottomRight = 0.cssRem,
                                    bottomLeft = 0.cssRem
                                )
                                fontSize(styleTabButtonFontSize.cssRem)
                                if (currentTabIndex.value == tabIndex) {
                                    paddingTop(styleTabCurrentTitlePaddingTop.cssRem)
                                    paddingRight(styleTabCurrentTitlePaddingRight.cssRem)
                                    paddingBottom(styleTabCurrentTitlePaddingBottom.cssRem)
                                    paddingLeft(styleTabCurrentTitlePaddingLeft.cssRem)
                                } else {
                                    paddingTop(styleTabOtherTitlePaddingTop.cssRem)
                                    paddingRight(styleTabOtherTitlePaddingRight.cssRem)
                                    paddingBottom(styleTabOtherTitlePaddingBottom.cssRem)
                                    paddingLeft(styleTabOtherTitlePaddingLeft.cssRem)
                                }
                                cursor("pointer")
                            }
                            title(tabInfo.tooltip)
                            onClick {
                                currentTabIndex.value = tabIndex
                            }
                        }
                    ) {
                        tabInfo.arrText.forEachIndexed { textIndex, text ->
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
//                                 v-show="tab.arrText[0]"
                        src="/web/images/ic_close_black_16dp.png", 
                        attrs = {
                            style {
                                width(16.px)
                                height(16.px)
                                background(
                                    if (currentTabIndex.value == tabIndex) {
                                        colorTabCurrentBack()
                                    } else {
                                        colorTabOtherBack()
                                    }
                                )
                                border {
                                    color(colorMainBorder())
                                    style(LineStyle.Solid)
                                    if (currentTabIndex.value == tabIndex) {
                                        top(styleTabCurrentCloserBorderTop.cssRem)
                                        right(styleTabCurrentCloserBorderRight.cssRem)
                                        bottom(styleTabCurrentCloserBorderBottom.cssRem)
                                        left(styleTabCurrentCloserBorderLeft.cssRem)
                                    } else {
                                        top(styleTabOtherCloserBorderTop.cssRem)
                                        right(styleTabOtherCloserBorderRight.cssRem)
                                        bottom(styleTabOtherCloserBorderBottom.cssRem)
                                        left(styleTabOtherCloserBorderLeft.cssRem)
                                    }
                                }
                                borderRadius(
                                    topLeft = styleFormBorderRadius.cssRem,
                                    topRight = 0.cssRem,
                                    bottomRight = 0.cssRem,
                                    bottomLeft = 0.cssRem
                                )
                                if (currentTabIndex.value == tabIndex) {
                                    paddingTop(styleTabCurrentCloserPaddingTop.cssRem)
                                    paddingRight(styleTabCurrentCloserPaddingRight.cssRem)
                                    paddingBottom(styleTabCurrentCloserPaddingBottom.cssRem)
                                    paddingLeft(styleTabCurrentCloserPaddingLeft.cssRem)
                                } else {
                                    paddingTop(styleTabOtherCloserPaddingTop.cssRem)
                                    paddingRight(styleTabOtherCloserPaddingRight.cssRem)
                                    paddingBottom(styleTabOtherCloserPaddingBottom.cssRem)
                                    paddingLeft(styleTabOtherCloserPaddingLeft.cssRem)
                                }
                                cursor("pointer")
                            }
                            title("Закрыть вкладку")
                            onClick {
//                                closeTabByIndex(tabIndex)
                            }
                        }
                    )
                }
            }
        }
    }
}