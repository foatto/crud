package foatto.ts

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.ts.core_ts.ObjectConfig

interface iTSApplication : iApplication {

    val controlEnabledRoleId: String

    val companiesParentId: String

    val alTSUserRoleId: Array<String>

    fun getObjectConfig(userConfig: UserConfig, objectId: Int): ObjectConfig
}