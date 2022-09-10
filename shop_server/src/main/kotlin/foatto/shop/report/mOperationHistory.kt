package foatto.shop.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedConnection

class mOperationHistory : mSHOPReport() {

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        isReportCatalog = true

        catalogSelectorAlias = "shop_catalog_item"

        //----------------------------------------------------------------------------------------------------------------------

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}
