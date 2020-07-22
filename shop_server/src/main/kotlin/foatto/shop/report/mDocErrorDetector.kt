@file:JvmName("mDocErrorDetector")
package foatto.shop.report

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

class mDocErrorDetector : mSHOPReport() {

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        //        isReportWarehouse = true;

        //        periodCaption = "Период проверки";

        //----------------------------------------------------------------------------------------------------------------------

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}
