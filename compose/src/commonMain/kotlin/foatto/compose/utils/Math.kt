package foatto.compose.utils

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp

@Stable
inline fun maxDp(a: Dp, b: Dp) = androidx.compose.ui.unit.max(a, b)