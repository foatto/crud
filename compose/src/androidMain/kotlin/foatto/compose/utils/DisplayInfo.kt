package foatto.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal actual fun getScaledScreenWidth(): Int = LocalConfiguration.current.screenWidthDp