package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.core_compose_web.control.model.TitleData
import foatto.core_compose_web.style.*
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.borderBottom
import org.jetbrains.compose.web.css.properties.borderTop
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import kotlin.js.Date
import kotlin.math.roundToInt

abstract class AbstractControl(
    protected val tabId: Int,
) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected val alTitleData: SnapshotStateList<TitleData> = mutableStateListOf()

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    abstract fun getBody()

    abstract fun start()

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    protected fun getMainDiv(content: ContentBuilder<HTMLDivElement>): Unit =
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
            content()
        }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    protected fun getTableAndFormHeader(
        withBottomBorder: Boolean,
        call: (url: String) -> Unit,
    ): Unit =
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
                        width = if (!styleIsNarrowScreen) 0.px else 1.px,
                        lineStyle = LineStyle.Solid,
                        color = colorMainBorder,
                    )
                    if (withBottomBorder) {
                        borderBottom(width = 1.px, lineStyle = LineStyle.Solid, color = colorMainBorder)
                    }
                    padding(styleControlPadding)
                    backgroundColor(getColorTableHeaderBack())
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
                                call(titleData.url)
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

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    protected fun getGraphicAndXyHeader(prefix: String) =
        Div(
            attrs = {
                id("${prefix}_title_$tabId")
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Row)
                    flexWrap(FlexWrap.Wrap)
                    justifyContent(JustifyContent.SpaceBetween)
                    alignItems(AlignItems.Center)
                    padding(styleControlPadding)
                    backgroundColor(getColorGraphicAndXyToolbarBack())
                    borderTop(
                        width = (if (!styleIsNarrowScreen) {
                            0
                        } else {
                            1
                        }).px,
                        lineStyle = LineStyle.Solid,
                        color = colorMainBorder,
                    )
                }
            }
        ) {
            getToolBarSpan {}
            Span(
                attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Row)
                        flexWrap(FlexWrap.Nowrap)
                        justifyContent(JustifyContent.Center)
                        alignItems(AlignItems.Center)
                        fontSize(styleControlTitleTextFontSize)
                        setPaddings(arrStyleControlTitlePadding)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                    }
                }
            ) {
                alTitleData.forEachIndexed { index, titleData ->
                    Span(
                        attrs = {
                            style {
                                fontWeight(
                                    if (alTitleData.size > 1 && index == 0) {
                                        "bold"
                                    } else {
                                        "normal"
                                    }
                                )
                            }
                        }
                    ) {
                        Text(titleData.text)
                    }
                }
            }
            getToolBarSpan {}
        }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    protected fun getGraphicAndXyToolbar(prefix: String, content: ContentBuilder<HTMLDivElement>) =
        Div(
            attrs = {
                id("${prefix}_toolbar_$tabId")
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Row)
                    flexWrap(FlexWrap.Wrap)
                    justifyContent(JustifyContent.SpaceBetween)
                    alignItems(AlignItems.Center)
                    padding(styleControlPadding)
                    backgroundColor(getColorGraphicAndXyToolbarBack())
                }
            }
        ) {
            content()
        }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    protected fun getToolBarSpan(content: ContentBuilder<HTMLSpanElement>) =
        Span(
            attrs = {
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Row)
                    flexWrap(FlexWrap.Nowrap)
                    justifyContent(JustifyContent.Center)
                    alignItems(AlignItems.Center)
                }
            }
        ) {
            content()
        }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    protected fun getToolBarIconButton(src: String, title: String, onClick: () -> Unit) =
        Img(
            src = src,
            attrs = {
                style {
                    backgroundColor(getColorToolbarButtonBack())
                    setBorder(getStyleToolbarButtonBorder())
                    fontSize(styleCommonButtonFontSize)
                    padding(styleIconButtonPadding)
                    setMargins(arrStyleCommonMargin)
                    cursor("pointer")
                }
                title(title)
                onClick {
                    onClick()
                }
            }
        )

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun getGraphixAndXyTooltipCoord(x: Int, y: Int): Pair<Int, Int> =
        Pair(
            x - (0 * scaleKoef).roundToInt(),
            y - (32 * scaleKoef).roundToInt(),
        )

    protected fun setGraphicAndXyTooltipOffTimeout(tooltipOffTime: Double, tooltipVisible: MutableState<Boolean>) {
        //--- через 3 сек выключить тултип, если не было других активаций тултипов
        //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
        //--- причём после ухода с графика других mouseleave не вызывается.
        window.setTimeout({
            if (Date().getTime() > tooltipOffTime) {
                tooltipVisible.value = false
            }
        }, 3000)
    }

}


