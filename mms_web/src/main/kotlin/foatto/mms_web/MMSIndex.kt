package foatto.mms_web

import foatto.core_web.*
import kotlinx.browser.window

@Suppress("UnsafeCastFromDynamic")
fun main() {

    window.onload = {
        val index = MMSIndex()
        index.init()
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- фирменный тёмно-синий         #0C386D = hsl(212.8,80.2%,23.7%) и градиент до #209dcb = hsl(196,73%,46%)
const val MMS_FIRM_COLOR_1_H = 213
const val MMS_FIRM_COLOR_1_S = 80
const val MMS_FIRM_COLOR_1_L = 24

//--- фирменный терракотовый        #F7AA47 = hsl(33.7,91.7%,62.4%)
const val MMS_FIRM_COLOR_2_H = 34
const val MMS_FIRM_COLOR_2_S = 92
const val MMS_FIRM_COLOR_2_L = 62

//--- фирменный тёмно-красный       #BF0D0E = hsl(359.7,87.3%,40%) - logon button
const val MMS_FIRM_COLOR_3_H = 360
const val MMS_FIRM_COLOR_3_S = 87
const val MMS_FIRM_COLOR_3_L = 40

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class MMSIndex : Index() {

    init {
        //--- constantly showed menu bar
//        styleIsHiddenMenuBar = localStorage.getItem(IS_HIDDEN_MENU_BAR)?.toBooleanStrictOrNull() ?: false

        colorMainBack0 = getHSL(MMS_FIRM_COLOR_1_H, 50, 95)
        colorMainBack1 = getHSL(MMS_FIRM_COLOR_1_H, 50, 90)
        colorMainBack2 = getHSL(MMS_FIRM_COLOR_1_H, 50, 85)
        colorMainBack3 = getHSL(MMS_FIRM_COLOR_1_H, 50, 80)

        colorMainBorder = "hsl(0,0%,72%)"
        styleFormBorderRadius = "${if (screenDPR <= 1.0) 0.0 else 0.0}rem"
        styleButtonBorderRadius = "${if (screenDPR <= 1.0) 0.4 else 0.8}rem"
        styleInputBorderRadius = "${if (screenDPR <= 1.0) 0.0 else 0.0}rem"

        colorButtonBack = colorMainBack0
        colorButtonBorder = colorMainBorder

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

        colorLogonBackAround = "url('/web/images/index-bg.jpg') 50% 0"
        colorLogonBackCenter = COLOR_MAIN_BACK_0
        colorLogonBorder = "hsla(0, 0%, 0%, 0)"
        colorLogonButtonBack = getHSL(MMS_FIRM_COLOR_3_H, MMS_FIRM_COLOR_3_S, MMS_FIRM_COLOR_3_L)
        colorLogonButtonText = COLOR_MAIN_BACK_0
        styleLogonButtonText = "Войти"
        styleLogonButtonFontWeight = "bold"
        colorLogonButtonBorder = getHSL(MMS_FIRM_COLOR_3_H, MMS_FIRM_COLOR_3_S, MMS_FIRM_COLOR_3_L)
        styleLogonButtonPadding = { "1.0rem ${if (!styleIsNarrowScreen) 5 else scaledScreenWidth / 48}rem" }

        colorMenuBack = colorMainBack1
        colorMenuBorder = colorMainBorder
        colorMenuDelimiter = colorMainBack3

        colorTableRowBack1 = colorMainBack0

        colorGroupBack0 = getHSL(MMS_FIRM_COLOR_2_H, MMS_FIRM_COLOR_2_S, 80)
        colorGroupBack1 = getHSL(MMS_FIRM_COLOR_2_H, MMS_FIRM_COLOR_2_S, 90)

        colorDialogBack = getHSLA(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, MMS_FIRM_COLOR_1_L, 0.75)
        colorDialogBorder = colorMainBorder
        colorDialogBackCenter = colorMainBack1
        colorDialogButtonBack = colorButtonBack
        colorDialogButtonBorder = colorMainBorder

        colorWaitBack = getHSLA(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95, 0.75)
        colorWaitLoader0 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 80)
        colorWaitLoader1 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 85)
        colorWaitLoader2 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 90)
        colorWaitLoader3 = getHSL(MMS_FIRM_COLOR_1_H, MMS_FIRM_COLOR_1_S, 95)

    }

}