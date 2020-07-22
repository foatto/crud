@file:JvmName("mPriceList")
package foatto.shop.report

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

class mPriceList : mSHOPReport() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        //----------------------------------------------------------------------------------------------------------------------

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}
