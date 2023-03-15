package foatto.mms_compose_web

import androidx.compose.runtime.Composable
import foatto.core.link.MenuData
import foatto.core_compose_web.Menu
import foatto.core_compose_web.Root
import foatto.core_compose_web.style.getPseudoNbsp
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Img

class MMSMenu(
    root: Root,
    arrMenuData: Array<MenuData>,
) : Menu(
    root,
    arrMenuData,
) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getMainMenuTop() {
        Br()
        getPseudoNbsp(4)
        Img(src = "/web/images/page-logo.png")
        Br()
        Br()
    }
}
