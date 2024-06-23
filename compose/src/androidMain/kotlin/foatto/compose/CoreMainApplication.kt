package foatto.compose

import android.app.Application

class CoreMainApplication : Application() {

    companion object {
        lateinit var instance: CoreMainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}