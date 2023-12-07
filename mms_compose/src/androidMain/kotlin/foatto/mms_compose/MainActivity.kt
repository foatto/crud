package foatto.mms_compose

import foatto.core_compose.CoreMainActivity

class MainActivity : CoreMainActivity() {

    override fun init() {
        super.init()

        mainApp = MMSMainApp()
    }
}

