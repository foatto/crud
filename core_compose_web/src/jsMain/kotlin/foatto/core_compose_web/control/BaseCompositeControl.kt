package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import foatto.core.link.CompositeResponse
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root

open class BaseCompositeControl(
    protected val root: Root,
    protected val appControl: AppControl,
    protected val compositeResponse: CompositeResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    @Composable
    override fun getBody() {
    }

    override fun start() {}

}