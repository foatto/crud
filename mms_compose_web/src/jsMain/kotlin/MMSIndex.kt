import org.jetbrains.compose.web.renderComposable

fun main() {
    alTabInfo.add(TabInfo(id = 0, arrText = arrayOf("aaa"), tooltip = "AAA"))
    alTabInfo.add(TabInfo(id = 1, arrText = arrayOf("bbb"), tooltip = "BBB"))

    val index = MMSIndex()
    renderComposable(rootElementId = "root") {
        index.getBody()
    }
}

private class MMSIndex : Index(
    styleIsNarrowScreen = false,
    styleIsHiddenMenuBar = true,    //false, - для начала типовой дизайн
) {
}

//import foatto.core_web.*
//import kotlinx.browser.window
//
//@Suppress("UnsafeCastFromDynamic")
//fun main() {
//
//    window.onload = {
//        val index = MMSIndex()
//        index.init()
//    }
//}
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
////--- фирменный тёмно-синий         #0C386D = hsl(212.8,80.2%,23.7%) и градиент до #209dcb = hsl(196,73%,46%)
//const val MMS_FIRM_COLOR_1_H = 213
//const val MMS_FIRM_COLOR_1_S = 80
//const val MMS_FIRM_COLOR_1_L = 24
//
////--- фирменный терракотовый        #F7AA47 = hsl(33.7,91.7%,62.4%)
//const val MMS_FIRM_COLOR_2_H = 34
//const val MMS_FIRM_COLOR_2_S = 92
//const val MMS_FIRM_COLOR_2_L = 62
//
////--- фирменный тёмно-красный       #BF0D0E = hsl(359.7,87.3%,40%) - logon button
//const val MMS_FIRM_COLOR_3_H = 360
//const val MMS_FIRM_COLOR_3_S = 87
//const val MMS_FIRM_COLOR_3_L = 40
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//
//    init {
//        styleIsHiddenMenuBar = false
//
//        colorMainBack0 = getHSL(MMS_FIRM_COLOR_1_H, 50, 95)
//        colorMainBack1 = getHSL(MMS_FIRM_COLOR_1_H, 50, 90)
//        colorMainBack2 = getHSL(MMS_FIRM_COLOR_1_H, 50, 85)
//        colorMainBack3 = getHSL(MMS_FIRM_COLOR_1_H, 50, 80)
//
//        colorMainBorder = { getHSL(0, 0, 72) }
//        styleFormBorderRadius = "0.0rem"
//        val buttonBorderRadius = if (screenDPR <= 1.0) {
//            if (styleIsNarrowScreen) {
//                0.2
//            } else {
//                0.4
//            }
//        } else {
//            if (styleIsNarrowScreen) {
//                0.4
//            } else {
//                0.8
//            }
//        }
//        styleButtonBorderRadius = "${buttonBorderRadius}rem"
//        styleInputBorderRadius = "0.0rem"
//
//        colorButtonBack = { colorMainBack0 }
//        colorButtonBorder = { colorMainBorder() }
//
//        colorWaitBack = getHSLA(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95, 0.75)
//        colorWaitLoader0 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 80)
//        colorWaitLoader1 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 85)
//        colorWaitLoader2 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 90)
//        colorWaitLoader3 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95)
//
//        colorDialogBack = getHSLA(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L, 0.75)
//
//        styleLogonTopExpanderContent =
//            """
//                <br>
//                <img src="/web/images/logo_pla.png">
//                <br>
//                <span v-bind:style="{ 'color' : 'white' , 'font-size' : '${if (styleIsNarrowScreen) "1rem" else "2rem"}' }">
//                    СИСТЕМА КОНТРОЛЯ
//                    ТЕХНОЛОГИЧЕСКОГО ОБОРУДОВАНИЯ И ТРАНСПОРТА
//                    «ПУЛЬСАР»
//                </span>
//                <br>
//            """
//        styleLogonLogo = "index-icon.png"
//        styleLogonLogoContent = if (styleIsNarrowScreen) {
//            ""
//        } else {
//            """
//                <span v-bind:style="{ 'font-size' : '1.5rem' }">
//                    Вход в систему
//                </span>
//                <br>
//            """
//        }
//
//        colorLogonBackAround = { "url('/web/images/index-bg.jpg') 50% 0" }
//        colorLogonBackCenter = { COLOR_MAIN_BACK_0 }
//        colorLogonBorder = { "hsla(0, 0%, 0%, 0)" }
//        colorLogonButtonBack = { getHSL(MMS_FIRM_COLOR_3_H, MMS_FIRM_COLOR_3_S, MMS_FIRM_COLOR_3_L) }
//        colorLogonButtonText = COLOR_MAIN_BACK_0
//        styleLogonButtonText = "Войти"
//        styleLogonButtonFontWeight = "bold"
//        colorLogonButtonBorder = { getHSL(MMS_FIRM_COLOR_3_H, MMS_FIRM_COLOR_3_S, MMS_FIRM_COLOR_3_L) }
//        styleLogonButtonPadding = { "1.0rem ${if (!styleIsNarrowScreen) 5 else scaledScreenWidth / 48}rem" }
//
//        styleMainMenuTop =
//            """
//                <br>
//                &nbsp;&nbsp;&nbsp;&nbsp;
//                <img src="/web/images/page-logo.png" alt="">
//                <br><br>
//            """
//
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
//
//        val colorBackGray = getHSL(210, 11, 89)
//
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
//        colorMainMenuBack = {
//            if (styleIsNarrowScreen) {
//                getHSL(206, 56, 40)
//            } else {
//                getHSLA(0, 0, 0, 0.0)   // прозрачный, из-за фонового рисунка
//            }
//        }
//        colorPopupMenuBack = { getHSL(206, 56, 40) }
//        colorMenuTextDefault = COLOR_MAIN_BACK_0
//        colorMenuBorder = { colorMainBorder() }
//        colorMenuDelimiter = { colorMainBack3 }
//        colorMenuBackHover0 = getHSL(206, 56, 35)
//        colorMenuTextHover0 = null
//        colorMenuBackHoverN = getHSLA(0, 0, 0, 0.0)  // прозрачный
//        colorMenuTextHoverN = getHSL(0, 0, 75)
//
//        //--- приводит к багу по расчёту ширины svg-зоны в xy/graphic-модулях
//        //styleAppControlPadding = { if (styleIsNarrowScreen) "0" else "1.0rem 1.0rem 0 1.0rem" }
//
//        colorTabPanelBack = colorBackGray
//        if (!styleIsNarrowScreen) { // not damage mobile version
//            styleTabPanelPadding = { "1.0rem 1.0rem 0 0" }
//        }
//
//        colorTabCurrentBack = { COLOR_MAIN_BACK_0 }
//        styleTabCurrentTitleBorderLeft = { "none" }
//        styleTabCurrentTitleBorderTop = { "none" }
//        styleTabCurrentTitleBorderRight = { "none" }
//        styleTabCurrentTitleBorderBottom = { "0.4rem solid $COLOR_MAIN_BACK_0" }
//        styleTabCurrentCloserBorderLeft = { "none" }
//        styleTabCurrentCloserBorderTop = { "none" }
//        styleTabCurrentCloserBorderRight = { "0.4rem solid $colorBackGray" }
//        styleTabCurrentCloserBorderBottom = { "0.4rem solid $COLOR_MAIN_BACK_0" }
//
//        colorTabOtherBack = { COLOR_MAIN_BACK_0 }
//        styleTabOtherTitleBorderLeft = { "none" }
//        styleTabOtherTitleBorderTop = { "none" }
//        styleTabOtherTitleBorderRight = { "none" }
//        styleTabOtherTitleBorderBottom = { "0.4rem solid $colorBackGray" }
//        styleTabOtherCloserBorderLeft = { "none" }
//        styleTabOtherCloserBorderTop = { "none" }
//        styleTabOtherCloserBorderRight = { "0.4rem solid $colorBackGray" }
//        styleTabOtherCloserBorderBottom = { "0.4rem solid $colorBackGray" }
//
//        val closerPaddingTopBottom = "${if (screenDPR <= 1.0) 0.8 else 0.5}rem"
//        styleTabCurrentTitlePadding = "0.1rem 0.6rem 0.1rem 1.0rem"
//        styleTabCurrentCloserPadding = "$closerPaddingTopBottom 0.4rem $closerPaddingTopBottom 0.4rem"
//        styleTabOtherTitlePadding = "0.1rem 0.6rem 0.1rem 1.0rem"
//        styleTabOtherCloserPadding = "$closerPaddingTopBottom 0.4rem $closerPaddingTopBottom 0.4rem"
//
//        colorTableHeaderBack = { COLOR_MAIN_BACK_0 }
//        colorTableToolbarBack = { COLOR_MAIN_BACK_0 }
//        colorTablePagebarBack = { COLOR_MAIN_BACK_0 }
//
//        colorTableFindButtonBack = { COLOR_MAIN_BACK_0 }
//        styleTableFindEditorBorderRadius = { "${if (screenDPR <= 1.0) 0.2 else 0.4}rem 0 0 ${if (screenDPR <= 1.0) 0.2 else 0.4}rem" }
//        styleTableFindButtonBorderRadius = { "0 ${if (screenDPR <= 1.0) 0.2 else 0.4}rem ${if (screenDPR <= 1.0) 0.2 else 0.4}rem 0" }
//        styleTableFindControlMargin = { "0" }
//
//        val COLOR_BACK_ORANGE = getHSL(34, 92, 62)
//        val COLOR_BACK_GREEN = getHSL(135, 54, 79)
//
//        colorToolbarButtonBack = { COLOR_BACK_ORANGE }
//        colorRefreshButtonBack = { COLOR_BACK_GREEN }
//        styleToolbarButtonBorder = { "none" }
//
//        styleTableCaptionBack = { getHSL(214, 11, 87) }
//        styleTableCaptionPadding = { "1.0rem 1.0rem 1.0rem 1.0rem" }
//        styleTableCaptionBorderLeft = { "none" }
//        styleTableCaptionBorderTop = { "none" }
//        styleTableCaptionBorderRight = { "1px solid ${getHSL(214, 6, 77)}" }
//        styleTableCaptionBorderBottom = { "none" }
//        styleTableCaptionAlignH = { "flex-start" }
//        styleTableCaptionAlignV = { "flex-start" }
//        styleTableCaptionFontSize = { "${if (!styleIsNarrowScreen) 0.8 else 0.6}rem" }
//        styleTableCaptionFontWeight = { "bold" }
//
//        colorGroupBack0 = { getHSL(200, 10, 94) }
//        colorGroupBack1 = { getHSL(200, 10, 97) }
//
//        colorTableRowBack1 = { colorMainBack0 }
//
//        colorTableRowHover = { getHSL(133, 54, 93) }
//
//        styleTableTextFontSize = { "${if (!styleIsNarrowScreen) 0.8 else 0.6}rem" }
//
//        colorTablePageBarCurrentBack = { COLOR_BACK_ORANGE }
//        styleTablePageBarOtherBorder = { "none" }
//
//        styleTablePageButtonWidth = { buttonCount ->
//            if (styleIsNarrowScreen) {
//                val buttonWidth = when (buttonCount) {
//                    4 -> 3.4    // 5.2 - слишком крупно и глуповато, используется всего один раз
//                    5 -> 3.4    // 4.1 - используется всего один раз
//                    6 -> 3.4
//                    7 -> 2.9
//                    8 -> 2.5
//                    else -> 2.0
//                }
//                "${buttonWidth}rem"
//            } else {
//                "3.0rem"// 6.0
//            }
//        }
//
//        styleTablePageButtonFontSize = { buttonCount ->
//            if (styleIsNarrowScreen) {
//                val fontSize = when (buttonCount) {
//                    4 -> 1.7    // 2.6 - слишком крупно и глуповато, используется всего один раз
//                    5 -> 1.7    // 2.0 - используется всего один раз
//                    6 -> 1.7
//                    7 -> 1.4
//                    8 -> 1.2
//                    else -> 1.0
//                }
//                "${fontSize}rem"
//            } else {
//                "1.3rem"// 2.6
//            }
//        }
//
//        colorFormBack = { COLOR_MAIN_BACK_0 }
//        styleFormLabelWeight = { "bold" }
//        colorFormButtonBack = { COLOR_MAIN_BACK_0 }
//        styleFormButtonBorder = { "none" }
//        colorFormActionButtonSaveBack = { COLOR_BACK_GREEN }
//        colorFormActionButtonOtherBack = { COLOR_BACK_ORANGE }
//        styleFormActionButtonBorder = { "none" }
//
//        colorGraphicToolbarBack = { COLOR_MAIN_BACK_0 }
//
//        colorXyToolbarBack = { COLOR_MAIN_BACK_0 }
//    }
//
//}