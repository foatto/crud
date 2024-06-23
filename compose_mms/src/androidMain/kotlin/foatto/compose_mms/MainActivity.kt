package foatto.compose_mms

import foatto.compose.CoreMainActivity

class MainActivity : CoreMainActivity() {

    override fun init() {
        super.init()

        root = MMSRoot()
    }
}

