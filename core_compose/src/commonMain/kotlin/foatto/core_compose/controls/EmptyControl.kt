package foatto.core_compose.controls

import androidx.compose.runtime.Composable

class EmptyControl : AbstractControl(
    tabId = 0,
) {

    @Composable
    override fun getBody() {
    }

    override fun start() {}
}