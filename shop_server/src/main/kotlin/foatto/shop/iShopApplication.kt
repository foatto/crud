package foatto.shop

import foatto.core_server.app.iApplication

interface iShopApplication : iApplication {

    val discountLimits: Array<String>
    val discountValues: Array<String>

    val fiscalOnceOnly: String?
    val fiscalIndex: String?
    val fiscalUrls: Array<String>
    val fiscalLineCutters: Array<String>
    val fiscalCashiers: Array<String>
    val fiscalTaxModes: Array<String>
    val fiscalPlace: String?

    fun getDocumentDate(docId:Int): Triple<Int, Int, Int>
    fun isDocumentFiscable(docId: Int): Boolean

    fun checkCatalogMarkable(aCatalogId: Int): Boolean
    fun findIncomeCatalogIdByMark(markCode: String): Int?
    fun findOutcomeCatalogIdByMark(markCode: String): Int?
}
