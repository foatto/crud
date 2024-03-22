package foatto.core_compose.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val applicationDispatcher: CoroutineDispatcher = Dispatchers.Main