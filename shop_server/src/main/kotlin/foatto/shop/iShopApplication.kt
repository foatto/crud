package foatto.shop

import foatto.core_server.app.iApplication

interface iShopApplication : iApplication {

    val fiscalURL: String?
    val fiscalClient: String?
    val fiscalLineCutter: String?
    val fiscalTaxMode: String?
    val fiscalPlace: String?
}
