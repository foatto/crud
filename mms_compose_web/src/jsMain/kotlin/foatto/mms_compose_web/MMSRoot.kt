package foatto.mms_compose_web

import foatto.core_compose_web.*
import foatto.core_compose_web.style.*
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.hsl
import org.jetbrains.compose.web.css.hsla
import org.jetbrains.compose.web.css.px
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

private class MMSRoot : Root(
    styleIsHiddenMenuBar = true,    //false, - для начала сделаем типовой дизайн
) {
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

        colorButtonBack = colorMainBack0
        colorButtonBorder = colorMainBorder

        colorWaitBack = hsla(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95, 0.75)
        colorWaitLoader0 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 80)
        colorWaitLoader1 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 85)
        colorWaitLoader2 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 90)
        colorWaitLoader3 = hsl(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95)

        colorDialogBack = hsla(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L, 0.75)

        styleLogonTopExpanderContent =
            """
                <br>
                <img src="/web/images/logo_pla.png">
                <br>
                <span v-bind:style="{ 'color' : 'white' , 'font-size' : '${if (styleIsNarrowScreen) "1rem" else "2rem"}' }">
                    СИСТЕМА КОНТРОЛЯ
                    ТЕХНОЛОГИЧЕСКОГО ОБОРУДОВАНИЯ И ТРАНСПОРТА
                    «ПУЛЬСАР»
                </span>
                <br>
            """

        styleLogonLogo = "index-icon.png"
        styleLogonLogoContent = if (styleIsNarrowScreen) {
            ""
        } else {
            """
                <span v-bind:style="{ 'font-size' : '1.5rem' }">
                    Вход в систему
                </span>
                <br>
            """
        }

//        getColorLogonBackAround = { "url('/web/images/index-bg.jpg') 50% 0" }
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

//        styleMainMenuTop =
//            """
//                <br>
//                &nbsp;&nbsp;&nbsp;&nbsp;
//                <img src="/web/images/page-logo.png" alt="">
//                <br><br>
//            """

//        styleTopBar =
//            """
//                <div id="$TOP_BAR_ID"
//                    v-bind:style="{
//                        'width' : '100%',
//                        'min-height' : '4rem',
//                        'display' : 'flex',
//                        'flex-direction' : 'row',
//                        'justify-content' : 'space-between',
//                        'align-items' : 'center',
//                        'background': 'linear-gradient(75deg, #0c386d 30%, #209dcb 100%)'
//                    }"
//                >
//                    <span
//                        v-bind:style="{
//                            'color' : 'white',
//                            'font-size' : '1.0rem'
//                        }"
//                    >
//                        &nbsp;
//                        СИСТЕМА КОНТРОЛЯ ТЕХНОЛОГИЧЕСКОГО ОБОРУДОВАНИЯ И ТРАНСПОРТА "ПУЛЬСАР"
//                    </span>
//                    <span>
//                        &nbsp;
//                    </span>
//                    <span>
//                        <img src="/web/images/page-icon.png" alt="">
//                        &nbsp;&nbsp;
//                    </span>
//                </div>
//            """

        val colorBackGray = hsl(210, 11, 89)

//        styleMenuBar =
//            """
//                v-bind:style="{
//                    'border' : 'none',
//                    'background' : 'url(/web/images/page-menu-bg.jpg) bottom / cover no-repeat'
//                }"
//            """
//        colorMenuCloserBack = "linear-gradient(180deg, ${getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L)} 4rem, $colorBackGray 4rem)"
//        colorMenuCloserButtonBack = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L)
//        colorMenuCloserButtonText = COLOR_MAIN_BACK_0

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
        colorMenuBackHover0 = hsl(206, 56, 35)
        colorMenuTextHover0 = null
        colorMenuBackHoverN = hsla(0, 0, 0, 0.0)  // прозрачный
        colorMenuTextHoverN = hsl(0, 0, 75)

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






        super.init()
    }
}

/*
        colorTableHeaderBack = { COLOR_MAIN_BACK_0 }
        colorTableToolbarBack = { COLOR_MAIN_BACK_0 }
        colorTablePagebarBack = { COLOR_MAIN_BACK_0 }

        colorTableFindButtonBack = { COLOR_MAIN_BACK_0 }
        styleTableFindEditorBorderRadius = { "${if (getScreenDPR <= 1.0) 0.2 else 0.4}rem 0 0 ${if (getScreenDPR <= 1.0) 0.2 else 0.4}rem" }
        styleTableFindButtonBorderRadius = { "0 ${if (getScreenDPR <= 1.0) 0.2 else 0.4}rem ${if (getScreenDPR <= 1.0) 0.2 else 0.4}rem 0" }
        styleTableFindControlMargin = { "0" }

        val COLOR_BACK_ORANGE = getHSL(34, 92, 62)
        val COLOR_BACK_GREEN = getHSL(135, 54, 79)

        colorToolbarButtonBack = { COLOR_BACK_ORANGE }
        colorRefreshButtonBack = { COLOR_BACK_GREEN }
        styleToolbarButtonBorder = { "none" }

        styleTableCaptionBack = { getHSL(214, 11, 87) }
        styleTableCaptionPadding = { "1.0rem 1.0rem 1.0rem 1.0rem" }
        styleTableCaptionBorderLeft = { "none" }
        styleTableCaptionBorderTop = { "none" }
        styleTableCaptionBorderRight = { "1px solid ${getHSL(214, 6, 77)}" }
        styleTableCaptionBorderBottom = { "none" }
        styleTableCaptionAlignH = { "flex-start" }
        styleTableCaptionAlignV = { "flex-start" }
        styleTableCaptionFontSize = { "${if (!getStyleIsNarrowScreen) 0.8 else 0.6}rem" }
        styleTableCaptionFontWeight = { "bold" }

        colorGroupBack0 = { getHSL(200, 10, 94) }
        colorGroupBack1 = { getHSL(200, 10, 97) }

        colorTableRowBack1 = { getColorMainBack0 }

        colorTableRowHover = { getHSL(133, 54, 93) }

        styleTableTextFontSize = { "${if (!getStyleIsNarrowScreen) 0.8 else 0.6}rem" }

        colorTablePageBarCurrentBack = { COLOR_BACK_ORANGE }
        styleTablePageBarOtherBorder = { "none" }

        styleTablePageButtonWidth = { buttonCount ->
            if (getStyleIsNarrowScreen) {
                val buttonWidth = when (buttonCount) {
                    4 -> 3.4    // 5.2 - слишком крупно и глуповато, используется всего один раз
                    5 -> 3.4    // 4.1 - используется всего один раз
                    6 -> 3.4
                    7 -> 2.9
                    8 -> 2.5
                    else -> 2.0
                }
                "${buttonWidth}rem"
            } else {
                "3.0rem"// 6.0
            }
        }

        styleTablePageButtonFontSize = { buttonCount ->
            if (getStyleIsNarrowScreen) {
                val fontSize = when (buttonCount) {
                    4 -> 1.7    // 2.6 - слишком крупно и глуповато, используется всего один раз
                    5 -> 1.7    // 2.0 - используется всего один раз
                    6 -> 1.7
                    7 -> 1.4
                    8 -> 1.2
                    else -> 1.0
                }
                "${fontSize}rem"
            } else {
                "1.3rem"// 2.6
            }
        }

        colorFormBack = { COLOR_MAIN_BACK_0 }
        styleFormLabelWeight = { "bold" }
        colorFormButtonBack = { COLOR_MAIN_BACK_0 }
        styleFormButtonBorder = { "none" }
        colorFormActionButtonSaveBack = { COLOR_BACK_GREEN }
        colorFormActionButtonOtherBack = { COLOR_BACK_ORANGE }
        styleFormActionButtonBorder = { "none" }

        colorGraphicToolbarBack = { COLOR_MAIN_BACK_0 }

        colorXyToolbarBack = { COLOR_MAIN_BACK_0 }
    }

}
*/
