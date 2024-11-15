package foatto.compose.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val applicationDispatcher: CoroutineDispatcher = Dispatchers.Main   //Swing - пока нет информации, чем оно лучше .Main