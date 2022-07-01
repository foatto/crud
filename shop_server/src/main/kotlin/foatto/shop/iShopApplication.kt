package foatto.shop

import foatto.core_server.app.iApplication

interface iShopApplication : iApplication {

    val shopId: String?

    val editLimitDays: String?

    val discountLimits: Array<String>
    val discountValues: Array<String>

    val fiscalOnceOnly: String?
    val fiscalIndex: String?
    val fiscalUrls: Array<String>
    val fiscalLineCutters: Array<String>
    val fiscalCashiers: Array<String>
    val fiscalTaxModes: Array<String>
    val fiscalPlace: String?

    val workHourInWorkDay: String?
    val workHourInSaturday: String?
    val alWorkHourUserId: Array<String>
    val alWorkHourPerHourTax: Array<String>
    val alWorkHourSalesPercent: Array<String>
    val otherSharePart: String?

    fun getDocumentDate(docId:Int): Triple<Int, Int, Int>
    fun isDocumentFiscable(docId: Int): Boolean

    fun checkCatalogMarkable(aCatalogId: Int): Boolean
    fun findIncomeCatalogIdByMark(markCode: String): Int?
    fun findOutcomeCatalogIdByMark(markCode: String): Int?
}
