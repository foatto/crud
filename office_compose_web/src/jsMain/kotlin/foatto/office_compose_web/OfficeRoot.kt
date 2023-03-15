package foatto.office_compose_web

import foatto.core_compose_web.*
import foatto.core_compose_web.control.*
import foatto.core_compose_web.style.*
import org.jetbrains.compose.web.css.hsl
import org.jetbrains.compose.web.css.hsla
import org.jetbrains.compose.web.renderComposable

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

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

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

fun main() {

    val root = OfficeRoot()
    root.init()

    renderComposable(rootElementId = "root") {
        root.getBody()
    }

    root.start()
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private class OfficeRoot : Root(
    //false, - для начала сделаем типовой дизайн
) {
    override fun init() {

        colorMainBack0 = hsl(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 95)
        colorMainBack1 = hsl(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 90)
        colorMainBack2 = hsl(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 85)
        colorMainBack3 = hsl(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, 80)

        colorMainBorder = hsl(OFFICE_FIRM_COLOR_3_H, OFFICE_FIRM_COLOR_3_S, OFFICE_FIRM_COLOR_3_L)

        colorWaitBack = hsla(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 95, 0.75)
        colorWaitLoader0 = hsl(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 80)
        colorWaitLoader1 = hsl(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 85)
        colorWaitLoader2 = hsl(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 90)
        colorWaitLoader3 = hsl(OFFICE_FIRM_COLOR_2_H, OFFICE_FIRM_COLOR_2_S, 95)

        colorDialogBack = hsla(OFFICE_FIRM_COLOR_1_H, OFFICE_FIRM_COLOR_1_S, OFFICE_FIRM_COLOR_1_L, 0.95)

        colorTableGroupBack0 = hsl(OFFICE_FIRM_COLOR_3_H, 80, 90)
        colorTableGroupBack1 = hsl(OFFICE_FIRM_COLOR_3_H, 80, 95)

        colorTableRowBack1 = colorMainBack0

        super.init()
    }
}
