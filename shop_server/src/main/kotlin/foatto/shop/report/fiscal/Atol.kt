package foatto.shop.report.fiscal

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*

class Atol : iFiscal {

    override fun addLine(
        name: String,
        price: Double,
        count: Double
    ) {
        TODO("Not yet implemented")
    }

    override fun sendFiscal(
        objectMapper: ObjectMapper,
        httpClient: HttpClient,
        fiscalUrl: String,
        fiscalCashier: String,
        docId: String,
        fiscalTaxMode: String,
        fiscalPlace: String
    ) {
        TODO("Not yet implemented")
    }

}