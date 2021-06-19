package foatto.shop.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

class mWarehouseState : mSHOPReport() {

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        isReportWarehouse = true
        isUseNullWarehouse = true
        isReportCatalog = true
        isReportEndDate = true

        catalogSelectorAlias = "shop_catalog_folder"

        //----------------------------------------------------------------------------------------------------------------------

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}

