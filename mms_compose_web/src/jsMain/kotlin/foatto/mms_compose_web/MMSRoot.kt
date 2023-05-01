package foatto.mms_compose_web

import androidx.compose.runtime.Composable
import foatto.core_compose.model.MenuDataClient
import foatto.core_compose_web.*
import foatto.core_compose_web.control.*
import foatto.core_compose_web.style.*
import foatto.mms_compose_web.control.MMSAppControl
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.Color.white
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

//------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- фирменный тёмно-синий         #0C386D = hsl(212.8,80.2%,23.7%) и градиент до #209dcb = hsl(196,73%,46%)
private const val MMS_FIRM_COLOR_1_H = 213
private const val MMS_FIRM_COLOR_1_S = 80
private const val MMS_FIRM_COLOR_1_L = 24

//--- фирменный терракотовый        #F7AA47 = hsl(33.7,91.7%,62.4%)
private const val MMS_FIRM_COLOR_2_H = 34
private const val MMS_FIRM_COLOR_2_S = 92
private const val MMS_FIRM_COLOR_2_L = 62

//--- фирменный тёмно-красный       #BF0D0E = hsl(359.7,87.3%,40%) - logon button
private const val MMS_FIRM_COLOR_3_H = 360
private const val MMS_FIRM_COLOR_3_S = 87
private const val MMS_FIRM_COLOR_3_L = 40

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

fun main() {

    val root = MMSRoot()
    root.init()

    renderComposable(rootElementId = "root") {
        root.getBody()
    }

    root.start()
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private class MMSRoot : Root() {
    override fun init() {

        colorMainBack0 = hsl(MMS_FIRM_COLOR_1_H, 50, 95)
        colorMainBack1 = hsl(MMS_FIRM_COLOR_1_H, 50, 90)
        colorMainBack2 = hsl(MMS_FIRM_COLOR_1_H, 50, 85)
        colorMainBack3 = hsl(MMS_FIRM_COLOR_1_H, 50, 80)

        colorMainBorder = hsl(0, 0, 72)
        styleFormBorderRadius = 0.0.cssRem

        val buttonBorderRadius = if (screenDPR <= 1.0) {
            if (styleIsNarrowScreen) {
                0.2
            } else {
                0.4
            }
        } else {
            if (styleIsNarrowScreen) {
                0.4
            } else {
                0.8
            }
        }
        styleButtonBorderRadius = buttonBorderRadius.cssRem
        styleInputBorderRadius = 0.0.cssRem

        colorWaitBack = hsla(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95, 0.75)
        colorWaitLoader0 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 80)
        colorWaitLoader1 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 85)
        colorWaitLoader2 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 90)
        colorWaitLoader3 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95)

        colorDialogBackColor = hsla(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L, 0.95)

        styleLogonLogo = "index-icon.png"

        getColorLogonBackAround = { background("url('/web/images/index-bg.jpg') 50% 0") }
        getColorLogonBackCenter = { COLOR_MAIN_BACK_0 }
        getColorLogonBorder = { hsla(0, 0, 0, 0) }
        getColorLogonButtonBack = { hsl(MMS_FIRM_COLOR_3_H, MMS_FIRM_COLOR_3_S, MMS_FIRM_COLOR_3_L) }
        colorLogonButtonText = COLOR_MAIN_BACK_0
        styleLogonButtonText = "Войти"
        styleLogonButtonFontWeight = "bold"
        getColorLogonButtonBorder = { hsl(MMS_FIRM_COLOR_3_H, MMS_FIRM_COLOR_3_S, MMS_FIRM_COLOR_3_L) }
        getStyleLogonButtonPaddings = {
            arrayOf(
                1.0.cssRem,
                (if (!styleIsNarrowScreen) 5 else scaledScreenWidth / 48).cssRem,
                1.0.cssRem,
                (if (!styleIsNarrowScreen) 5 else scaledScreenWidth / 48).cssRem,
            )
        }

        styleIsHiddenMenuBar = false
        getStyleMenuBar = {
            background("url(/web/images/page-menu-bg.jpg) bottom / cover no-repeat")
            border {
                width = 0.px
            }
        }

        val colorBackGray = hsl(210, 11, 89)

        getColorMenuCloserBack = {
            background("linear-gradient(180deg, ${hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L)} 4rem, $colorBackGray 4rem)")
        }
        colorMenuCloserButtonBack = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L)
        colorMenuCloserButtonText = COLOR_MAIN_BACK_0

        getColorMainMenuBack = {
            if (styleIsNarrowScreen) {
                hsl(206, 56, 40)
            } else {
                hsla(0, 0, 0, 0.0)   // прозрачный, из-за фонового рисунка
            }
        }
        getColorPopupMenuBack = { hsl(206, 56, 40) }
        colorMenuTextDefault = COLOR_MAIN_BACK_0
        getColorMenuBorder = { colorMainBorder }
        getColorMenuDelimiter = { colorMainBack3 }
        getColorMenuBackHover0 = { hsl(206, 56, 35) }
        getColorMenuTextHover0 = { null }
        getColorMenuBackHoverN = { hsla(0, 0, 0, 0.0) }  // прозрачный
        getColorMenuTextHoverN = { hsl(0, 0, 75) }

        //--- приводит к багу по расчёту ширины svg-зоны в xy/graphic-модулях
        //styleAppControlPadding = { if (getStyleIsNarrowScreen) "0" else "1.0rem 1.0rem 0 1.0rem" }

        colorTabPanelBack = colorBackGray
        if (!styleIsNarrowScreen) { // not damage mobile version
            arrStyleTabPanelPadding = arrayOf(1.0.cssRem, 1.0.cssRem, 0.cssRem, 0.cssRem)
        }

        getColorTabCurrentBack = { COLOR_MAIN_BACK_0 }

        arrStyleTabCurrentTitleBorderWidth = arrayOf(0.px, 0.px, 0.4.cssRem, 0.px)
        arrStyleTabCurrentTitleBorderColor = arrayOf(colorMainBorder, colorMainBorder, COLOR_MAIN_BACK_0, colorMainBorder)
        arrStyleTabCurrentCloserBorderWidth = arrayOf(0.px, 0.4.cssRem, 0.4.cssRem, 0.px)
        arrStyleTabCurrentCloserBorderColor = arrayOf(colorMainBorder, colorBackGray, COLOR_MAIN_BACK_0, colorMainBorder)

        getColorTabOtherBack = { COLOR_MAIN_BACK_0 }

        arrStyleTabOtherTitleBorderWidth = arrayOf(0.px, 0.px, 0.4.cssRem, 0.px)
        arrStyleTabOtherTitleBorderColor = arrayOf(colorBackGray, colorBackGray, colorBackGray, colorBackGray)
        arrStyleTabOtherCloserBorderWidth = arrayOf(0.px, 0.4.cssRem, 0.4.cssRem, 0.px)
        arrStyleTabOtherCloserBorderColor = arrayOf(colorBackGray, colorBackGray, colorBackGray, colorBackGray)

        val closerPaddingTopBottom = (if (screenDPR <= 1.0) 0.8 else 0.5).cssRem

        arrStyleTabCurrentTitlePadding = arrayOf(0.1.cssRem, 0.6.cssRem, 0.1.cssRem, 1.0.cssRem)
        arrStyleTabCurrentCloserPadding = arrayOf(closerPaddingTopBottom, 0.4.cssRem, closerPaddingTopBottom, 0.4.cssRem)

        arrStyleTabOtherTitlePadding = arrayOf(0.1.cssRem, 0.6.cssRem, 0.1.cssRem, 1.0.cssRem)
        arrStyleTabOtherCloserPadding = arrayOf(closerPaddingTopBottom, 0.4.cssRem, closerPaddingTopBottom, 0.4.cssRem)

        getColorTableHeaderBack = { COLOR_MAIN_BACK_0 }
        getColorTableToolbarBack = { COLOR_MAIN_BACK_0 }
        getColorTablePagebarBack = { COLOR_MAIN_BACK_0 }

        getColorTableFindButtonBack = { COLOR_MAIN_BACK_0 }
        getStyleTableFindEditorBorderRadius = {
            arrayOf(
                (if (screenDPR <= 1.0) 0.2 else 0.4).cssRem,
                0.cssRem,
                0.cssRem,
                (if (screenDPR <= 1.0) 0.2 else 0.4).cssRem,
            )
        }
        getStyleTableFindButtonBorderRadius = {
            arrayOf(
                0.cssRem,
                (if (screenDPR <= 1.0) 0.2 else 0.4).cssRem,
                (if (screenDPR <= 1.0) 0.2 else 0.4).cssRem,
                0.cssRem,
            )
        }
        getStyleTableFindControlMargin = { arrayOf(0.cssRem, 0.cssRem, 0.cssRem, 0.cssRem) }

        val colorBackOrange = hsl(34, 92, 62)
        val colorBackGreen = hsl(135, 54, 79)

        getColorToolbarButtonBack = { colorBackOrange }
        getColorRefreshButtonBack = { colorBackGreen }
        getStyleToolbarButtonBorder = { noBorder }

        getColorTableCaptionBack = { hsl(214, 11, 87) }
        getStyleTableCaptionPadding = { arrayOf(1.0.cssRem, 1.0.cssRem, 1.0.cssRem, 1.0.cssRem) }
        getStyleTableCaptionBorderLeft = { noBorder }
        getStyleTableCaptionBorderTop = { noBorder }
        getStyleTableCaptionBorderRight = { BorderData(hsl(214, 6, 77), LineStyle.Solid, 1.px, 0.cssRem) }
        getStyleTableCaptionBorderBottom = { noBorder }
        getStyleTableCaptionAlignH = { AlignItems.FlexStart }
        getStyleTableCaptionAlignV = { JustifyContent.FlexStart }
        getStyleTableCaptionFontSize = { (if (!styleIsNarrowScreen) 0.8 else 0.6).cssRem }
        getStyleTableCaptionFontWeight = { "bold" }

        colorTableGroupBack0 = hsl(200, 10, 94)
        colorTableGroupBack1 = hsl(200, 10, 97)

        colorTableRowBack1 = colorMainBack0

        getColorTableRowHover = { hsl(133, 54, 93) }

        getStyleTableTextFontSize = { (if (!styleIsNarrowScreen) 0.8 else 0.6).cssRem }

        getColorTablePageBarCurrentBack = { colorBackOrange }
        getStyleTablePageBarOtherBorder = { noBorder }

        getStyleTablePageButtonWidth = { buttonCount ->
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
                3.0.cssRem  // 6.0
            }
        }

        getStyleTablePageButtonFontSize = { buttonCount ->
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
                1.3.cssRem  // 2.6
            }
        }

        getColorFormBack = { COLOR_MAIN_BACK_0 }
        styleFormLabelWeight = "bold"
        getColorFormButtonBack = { COLOR_MAIN_BACK_0 }
        getStyleFormButtonBorder = { noBorder }
        getColorFormActionButtonSaveBack = { colorBackGreen }
        getColorFormActionButtonOtherBack = { colorBackOrange }
        getStyleFormActionButtonBorder = { noBorder }

        getColorGraphicAndXyToolbarBack = { COLOR_MAIN_BACK_0 }

        getMenu = { root: Root,
                    arrMenuData: Array<MenuDataClient> ->
            MMSMenu(root, arrMenuData)
        }

        getAppControl = { root: Root,
                          startAppParam: String,
                          tabId: Int ->
            MMSAppControl(root, startAppParam, tabId)
        }

        super.init()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getTopBar() {
        Div(
            attrs = {
                id(TOP_BAR_ID)
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Row)
                    justifyContent(JustifyContent.SpaceBetween)
                    alignItems(AlignItems.Center)
                    width(100.percent)
                    minHeight(4.cssRem)
                    background("linear-gradient(75deg, #0c386d 30%, #209dcb 100%)")
                }
            }
        ) {
            Span(
                attrs = {
                    style {
                        color(white)
                        fontSize(1.0.cssRem)
                    }
                }
            ) {
                getPseudoNbsp(1)
                Text("СИСТЕМА КОНТРОЛЯ ТЕХНОЛОГИЧЕСКОГО ОБОРУДОВАНИЯ И ТРАНСПОРТА \"ПУЛЬСАР\"")
            }
            Span {
                getPseudoNbsp(1)
            }
            Span {
                Img(src = "/web/images/page-icon.png")
                getPseudoNbsp(2)
            }
        }
    }
}