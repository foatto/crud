package foatto.core_compose_web.control

import androidx.compose.runtime.Composable

class EmptyControl : AbstractControl(
    tabId = 0,
) {

    @Composable
    override fun getBody() {
    }

    override fun start() {}
}