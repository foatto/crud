package foatto.shop

import foatto.core_server.app.iApplication

interface iShopApplication : iApplication {

    val fiscalIndex: String?
    val fiscalUrls: Array<String>
    val fiscalLineCutters: Array<String>
    val fiscalCashiers: Array<String>
    val fiscalTaxModes: Array<String>
    val fiscalPlace: String?

    fun isFiscable(docId: Int): Boolean
    fun checkCatalogMarkable(aCatalogId: Int): Boolean
    fun findIncomeCatalogIdByMark(markCode: String): Int?
    fun findOutcomeCatalogIdByMark(markCode: String): Int?
}
