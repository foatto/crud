package foatto.core_compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

open class MainApp {

    protected var isWideScreen: Boolean = false

    @Composable
    fun Content() {
        isWideScreen = LocalConfiguration.current.screenWidthDp >= 600

        MaterialTheme {
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top,
                ) {
                    MainMenu()
                    MainBody()
                }
            } else {
                MainBody()
            }
        }
    }
}
