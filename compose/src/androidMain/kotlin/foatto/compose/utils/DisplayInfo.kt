package foatto.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

//--- window width == screen width, because application is fullscreen usually on android devices
@Composable
internal actual fun getScaledWindowWidth(): Int = LocalConfiguration.current.screenWidthDp