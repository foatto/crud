package foatto.compose_ts

import foatto.compose.CoreMainActivity

class MainActivity : CoreMainActivity() {

    override fun init() {
        super.init()

        root = TSRoot()
    }
}

