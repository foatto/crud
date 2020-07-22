@file:JvmName("mOperationSummary")
package foatto.shop.report

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

class mOperationSummary : mSHOPReport() {

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        //        isReportWarehouse = true; - пока сделаем независимо от точки продажи
        //        isUseNullWarehouse = true   // использовать ли "все склады/магазины"
        //        isReportClient = true; - пока по всем клиентам
        //        isReportDocumentType = true; - пока сделаем отчёт только по продажам
        isReportCatalog = true
        isReportBegDate = true
        isReportEndDate = true

        catalogSelectorAlias = "shop_catalog_folder"

        //----------------------------------------------------------------------------------------------------------------------

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}

