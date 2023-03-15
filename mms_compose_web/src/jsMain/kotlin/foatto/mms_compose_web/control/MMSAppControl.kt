package foatto.mms_compose_web.control

import androidx.compose.runtime.Composable
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.style.styleIsNarrowScreen
import org.jetbrains.compose.web.css.Color.white
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class MMSAppControl(
    root: Root,
    startAppParam: String,
    tabId: Int,
) : AppControl(
    root,
    startAppParam,
    tabId,
) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getLogonTopExpanderContent() {
        Br()
        Img(src = "/web/images/logo_pla.png")
        Br()
        Span(
            attrs = {
                style {
                    color(white)
                    fontSize(if (styleIsNarrowScreen) 1.cssRem else 2.cssRem)
                }
            }
        ) {
            Text("СИСТЕМА КОНТРОЛЯ")
            Br()
            Text("ТЕХНОЛОГИЧЕСКОГО ОБОРУДОВАНИЯ И ТРАНСПОРТА")
            Br()
            Text("«ПУЛЬСАР»")
        }
    }

    @Composable
    override fun getLogonLogoContent() {
        if (!styleIsNarrowScreen) {
            Span(
                attrs = {
                    style {
                        fontSize(1.5.cssRem)
                    }
                }
            ) {
                Text("Вход в систему")
                Br()
            }
        }
    }
}
