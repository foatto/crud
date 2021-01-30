package foatto.shop

import foatto.core_server.app.iApplication

interface iShopApplication : iApplication {

    val fiscalUrls: Array<String>
    val fiscalLineCutters: Array<String>
    val fiscalCashiers: Array<String>
    val fiscalTaxModes: Array<String>
    val fiscalPlace: String?
}
