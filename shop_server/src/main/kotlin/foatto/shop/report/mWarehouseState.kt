package foatto.shop.report

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

class mWarehouseState : mSHOPReport() {

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        isReportWarehouse = true
        isUseNullWarehouse = true
        isReportCatalog = true
        isReportEndDate = true

        catalogSelectorAlias = "shop_catalog_folder"

        //----------------------------------------------------------------------------------------------------------------------

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}

