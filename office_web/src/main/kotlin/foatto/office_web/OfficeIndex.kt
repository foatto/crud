package foatto.office_web

import foatto.core_web.*
import kotlinx.browser.window

@Suppress("UnsafeCastFromDynamic")
fun main() {

    window.onload = {
        val index = OfficeIndex()
        index.init()
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- светло-серый (с сайта)            #E3EEF4 = hsl(201.2,43.6%,92.4%)
private const val OFFICE_FIRM_COLOR_1_H = 201
private const val OFFICE_FIRM_COLOR_1_S = 44
private const val OFFICE_FIRM_COLOR_1_L = 92

//--- серо-стальной                     #B4CCDD = hsl(204.9,37.6%,78.6%)
private const val OFFICE_FIRM_COLOR_2_H = 205
private const val OFFICE_FIRM_COLOR_2_S = 38
private const val OFFICE_FIRM_COLOR_2_L = 79

//--- фирменный светло-красный в двух вариантах:
//--- название фирмы и эмблема          #F65146 = hsl(3.7,90.7%,62%)
//--- активный пункт меню               #FC454F = hsl(356.7,96.8%,62.9%)
//--- итого средний цвет:               #F95046 = hsl(3.5,93.8%,62.5%)
private const val OFFICE_FIRM_COLOR_3_H = 4
private const val OFFICE_FIRM_COLOR_3_S = 94
private const val OFFICE_FIRM_COLOR_3_L = 63

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class OfficeIndex : Index() {

    init {
        colorMainBack0 = getHSL(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 95)
        colorMainBack1 = getHSL(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 90)
        colorMainBack2 = getHSL(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 85)
        colorMainBack3 = getHSL(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 80)

        colorMainBorder = getHSL(OFFICE_FIRM_COLOR_3_H, OFFICE_FIRM_COLOR_3_S, OFFICE_FIRM_COLOR_3_L)

        colorButtonBack = colorMainBack0
        colorButtonBorder = colorMainBorder

        colorLogonBackAround = colorMainBack1
        colorLogonBackCenter = colorMainBack2
        colorLogonBorder = colorMainBorder
        colorLogonButtonBack = colorButtonBack
        colorLogonButtonBorder = colorButtonBorder

        colorMainMenuBack = colorMainBack1
        colorPopupMenuBack = colorMainBack1
        colorMenuBorder = colorMainBorder
        colorMenuDelimiter = colorMainBack3
/*
var colorMenuBack0 = colorMainBack1
var colorMenuBackHover0 = colorCurrentAndHover
var styleMenuBold0 = false
var colorMenuTextHover0: String? = null

var colorMenuBackN = colorMainBack1
var colorMenuBackHoverN = colorCurrentAndHover
var styleMenuBoldN = false
var colorMenuTextHoverN: String? = null
 */

        colorTableRowBack1 = colorMainBack0

        colorGroupBack0 = getHSL(OFFICE_FIRM_COLOR_3_H, 80, 90)
        colorGroupBack1 = getHSL(OFFICE_FIRM_COLOR_3_H, 80, 95)

        colorDialogBack = getHSLA(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, OFFICE_FIRM_COLOR_1_L, 0.75)
        colorDialogBorder = colorMainBorder
        colorDialogBackCenter = colorMainBack1
        colorDialogButtonBack = colorButtonBack
        colorDialogButtonBorder = colorMainBorder

        colorWaitBack = getHSLA(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 95, 0.75)
        colorWaitLoader0 = getHSL(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 80)
        colorWaitLoader1 = getHSL(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 85)
        colorWaitLoader2 = getHSL(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 90)
        colorWaitLoader3 = getHSL(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 95)
    }
}
