package foatto.compose.control

import androidx.compose.runtime.Composable

class EmptyControl : AbstractControl(
    tabId = 0,
) {

    @Composable
    override fun Body() {}

    override suspend fun start() {}
}