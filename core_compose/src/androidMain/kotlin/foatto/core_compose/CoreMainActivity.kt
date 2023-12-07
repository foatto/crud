package foatto.core_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

open class CoreMainActivity : ComponentActivity() {

    var mainApp: MainApp = MainApp()

    open fun init() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()

        setContent {
            mainApp.Content()
        }
    }
}
