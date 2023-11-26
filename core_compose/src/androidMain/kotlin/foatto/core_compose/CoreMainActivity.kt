package foatto.core_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

open class CoreMainActivity : ComponentActivity() {

    var app: App = App("Hi!")

    open fun init() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()

        setContent {
            app.Content()
        }
    }
}

@Preview
@Composable
private fun AppAndroidPreview() {
    App("Hi!")
}