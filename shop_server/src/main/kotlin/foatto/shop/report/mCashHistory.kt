package foatto.shop.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

class mCashHistory : mSHOPReport() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        isReportWarehouse = true
        isUseNullWarehouse = false
        isReportBegDate = true
        isReportEndDate = true

        //----------------------------------------------------------------------------------------------------------------------

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}
