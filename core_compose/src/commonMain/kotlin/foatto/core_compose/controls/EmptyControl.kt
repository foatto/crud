package foatto.core_compose.controls

import androidx.compose.runtime.Composable

class EmptyControl : AbstractControl(
    tabId = 0,
) {

    @Composable
    override fun Body() {
    }

    override fun start() {}
}