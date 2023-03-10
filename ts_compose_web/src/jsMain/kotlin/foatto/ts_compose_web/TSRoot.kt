package foatto.ts_compose_web

import foatto.core.link.CompositeResponse
import foatto.core_compose_web.*
import foatto.core_compose_web.control.TableControl.Companion.hmTableIcon
import foatto.core_compose_web.control.colorTableGroupBack0
import foatto.core_compose_web.control.colorTableGroupBack1
import foatto.core_compose_web.control.colorTableRowBack1
import foatto.core_compose_web.style.colorMainBack0
import foatto.core_compose_web.style.colorMainBack1
import foatto.core_compose_web.style.colorMainBack2
import foatto.core_compose_web.style.colorMainBack3
import foatto.core_compose_web.style.colorMainBorder
import foatto.core_compose_web.style.styleStateServerButtonTextFontWeight
import foatto.ts_compose_web.control.TSCompositeControl
import foatto.ts_core.app.ICON_NAME_TROUBLE_TYPE_CONNECT
import foatto.ts_core.app.ICON_NAME_TROUBLE_TYPE_ERROR
import foatto.ts_core.app.ICON_NAME_TROUBLE_TYPE_WARNING
import org.jetbrains.compose.web.css.hsl
import org.jetbrains.compose.web.css.hsla
import org.jetbrains.compose.web.renderComposable

//------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- фирменный тёмно-синий         #004271 = hsl(205,100%,22.2%)
private const val TS_FIRM_COLOR_1_H = 205
private const val TS_FIRM_COLOR_1_S = 100
private const val TS_FIRM_COLOR_1_L = 22

//--- офигенный серый металлический #C0C0D0 = hsl(240,14.5%,78.4%)
private const val TS_FIRM_COLOR_2_H = 240
private const val TS_FIRM_COLOR_2_S = 15
private const val TS_FIRM_COLOR_2_L = 78

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

fun main() {

    val root = TSRoot()
    root.init()

    renderComposable(rootElementId = "root") {
        root.getBody()
    }

    root.start()
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private class TSRoot : Root(
    styleIsHiddenMenuBar = true,
) {
    override fun init() {

        colorMainBack0 = hsl(TS_FIRM_COLOR_1_H, 50, 95)
        colorMainBack1 = hsl(TS_FIRM_COLOR_1_H, 50, 90)
        colorMainBack2 = hsl(TS_FIRM_COLOR_1_H, 50, 85)
        colorMainBack3 = hsl(TS_FIRM_COLOR_1_H, 50, 80)

        colorMainBorder = hsl(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, TS_FIRM_COLOR_2_L)

        colorWaitBack = hsla(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 95, 0.75)
        colorWaitLoader0 = hsl(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 80)
        colorWaitLoader1 = hsl(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 85)
        colorWaitLoader2 = hsl(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 90)
        colorWaitLoader3 = hsl(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 95)

        colorDialogBack = hsla(TS_FIRM_COLOR_1_H, TS_FIRM_COLOR_1_S, TS_FIRM_COLOR_1_L, 0.95)

        styleStateServerButtonTextFontWeight = "bold"

        //--- иконки внутри строк, цвет и размер менять/кастомизировать не планируется
        hmTableIcon[ICON_NAME_TROUBLE_TYPE_CONNECT] = "/web/images/ic_portable_wifi_off_black_24dp.png"
        hmTableIcon[ICON_NAME_TROUBLE_TYPE_WARNING] = "/web/images/ic_warning_black_24dp.png"
        hmTableIcon[ICON_NAME_TROUBLE_TYPE_ERROR] = "/web/images/ic_error_outline_black_24dp.png"

        colorTableGroupBack0 = hsl(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 90)
        colorTableGroupBack1 = hsl(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 95)

        colorTableRowBack1 = colorMainBack0

        getCompositeControl = { root: Root,
                                appControl: AppControl,
                                compositeResponse: CompositeResponse,
                                tabId: Int ->
            TSCompositeControl(root, appControl, compositeResponse, tabId)
        }

        super.init()
    }
}
