package foatto.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import foatto.compose.utils.applicationDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class CoreMainActivity : ComponentActivity() {

    protected lateinit var root: Root

    open fun init() {
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()

        //!!! проверить
        // enableEdgeToEdge()

        setContent {
            root.Content()
        }

        GlobalScope.launch(applicationDispatcher) {
            root.start()
        }
    }
}
