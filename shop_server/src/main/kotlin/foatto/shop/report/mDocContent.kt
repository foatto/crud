package foatto.shop.report

import foatto.app.CoreSpringController
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

class mDocContent : mSHOPReport() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        isReportDocument = true
        isReportClient = true
        isReportDocumentType = true
        isReportCatalog = true

        isUseCapAndSignature = true

        catalogSelectorAlias = "shop_catalog_item"

        //----------------------------------------------------------------------------------------------------------------------

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}

