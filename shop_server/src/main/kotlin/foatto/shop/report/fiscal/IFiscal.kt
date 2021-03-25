package foatto.shop.report.fiscal

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.*

interface iFiscal {

    fun addLine(
        name: String,
        price: Double,
        count: Double,
        markingCode: String?,
    )

    fun sendFiscal(
        objectMapper: ObjectMapper,
        httpClient: HttpClient,
        fiscalUrl: String,
        fiscalCashier: String,
        docId: String,
        fiscalTaxMode: String,
        fiscalPlace: String,
    )

    fun closeShift(
        objectMapper: ObjectMapper,
        httpClient: HttpClient,
        fiscalUrl: String,
    )
}