package foatto.mms

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.mms.core_mms.ObjectConfig

interface iMMSApplication : iApplication {

    val maxEnabledOverSpeed: Int
    val expirePeriod: Int

    fun getObjectConfig(userConfig: UserConfig, objectId: Int): ObjectConfig
}