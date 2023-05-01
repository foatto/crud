package foatto.shop_compose_web

import foatto.core.link.TableResponse
import foatto.core_compose_web.*
import foatto.core_compose_web.control.TableControl.Companion.hmTableIcon
import foatto.core_compose_web.control.colorTableGroupBack0
import foatto.core_compose_web.control.colorTableGroupBack1
import foatto.core_compose_web.control.colorTableRowBack1
import foatto.core_compose_web.style.COLOR_MAIN_TEXT
import foatto.core_compose_web.style.colorMainBack0
import foatto.core_compose_web.style.colorMainBack1
import foatto.core_compose_web.style.colorMainBack2
import foatto.core_compose_web.style.colorMainBack3
import foatto.core_compose_web.style.colorMainBorder
import foatto.core_compose_web.style.getStyleIconNameSuffix
import foatto.shop_compose_web.control.ShopTableControl
import foatto.shop_core.app.ICON_NAME_ADD_MARKED_ITEM
import foatto.shop_core.app.ICON_NAME_CALC
import foatto.shop_core.app.ICON_NAME_FISCAL
import org.jetbrains.compose.web.css.hsl
import org.jetbrains.compose.web.css.hsla
import org.jetbrains.compose.web.renderComposable

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- фирменный бирюзовый           #00C0C0 = hsl(180,100%,37.6%)
private const val SHOP_FIRM_COLOR_1_H = 190 //180 - слишком яркий голубой
private const val SHOP_FIRM_COLOR_1_S = 100
private const val SHOP_FIRM_COLOR_1_L = 38

//--- фирменный красный             #FF0000 = hsl(0,100%,50%)
private const val SHOP_FIRM_COLOR_2_H = 350 // 0 - слишком ярко-красный
private const val SHOP_FIRM_COLOR_2_S = 50  // 100 - слишком ярко-красный
private const val SHOP_FIRM_COLOR_2_L = 50

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

fun main() {

    val root = ShopRoot()
    root.init()

    renderComposable(rootElementId = "root") {
        root.getBody()
    }

    root.start()
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private class ShopRoot : Root(
    //false, - для начала сделаем типовой дизайн
) {
    override fun init() {

        colorMainBack0 = hsl(SHOP_FIRM_COLOR_1_H, 50, 95)
        colorMainBack1 = hsl(SHOP_FIRM_COLOR_1_H, 50, 90)
        colorMainBack2 = hsl(SHOP_FIRM_COLOR_1_H, 50, 85)
        colorMainBack3 = hsl(SHOP_FIRM_COLOR_1_H, 50, 80)

        colorMainBorder = hsl(SHOP_FIRM_COLOR_2_H, SHOP_FIRM_COLOR_2_S, SHOP_FIRM_COLOR_2_L)

        //--- с фирменным красным получается кроваво-страшновато :)
        colorWaitBack = hsla(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 95, 0.75)
        colorWaitLoader0 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 80)
        colorWaitLoader1 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 85)
        colorWaitLoader2 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 90)
        colorWaitLoader3 = hsl(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, 95)

        colorDialogBackColor = hsla(SHOP_FIRM_COLOR_1_H, SHOP_FIRM_COLOR_1_S, SHOP_FIRM_COLOR_1_L, 0.95)

        //--- менять здесь
        //styleDarkIcon = true
        //styleIconSize = 36

        //--- marked item adding
        hmTableIcon[ICON_NAME_ADD_MARKED_ITEM] = "/web/images/ic_line_weight_${getStyleIconNameSuffix()}.png"
        hmTableIcon[ICON_NAME_FISCAL] = "/web/images/ic_theaters_${getStyleIconNameSuffix()}.png"
        hmTableIcon[ICON_NAME_CALC] = "/web/images/ic_shopping_cart_${getStyleIconNameSuffix()}.png"

        colorTableGroupBack0 = hsl(SHOP_FIRM_COLOR_2_H, 60, 90)
        colorTableGroupBack1 = hsl(SHOP_FIRM_COLOR_2_H, 60, 95)

        colorTableRowBack1 = colorMainBack0

        getTableControl = { root: Root,
                            appControl: AppControl,
                            appParam: String,
                            tableResponse: TableResponse,
                            tabId: Int ->
            ShopTableControl(root, appControl, appParam, tableResponse, tabId)
        }

        super.init()
    }
}
