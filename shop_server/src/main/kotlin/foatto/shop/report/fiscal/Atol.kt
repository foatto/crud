package foatto.shop.report.fiscal

import com.fasterxml.jackson.databind.ObjectMapper
import foatto.core.util.AdvancedLogger
import foatto.shop.mCatalog
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.math.min

class Atol : iFiscal {

    private val alItem = mutableListOf<AtolFiscalItem>()
    private var sumCostOut = 0.0

    override fun addLine(
        name: String,
        price: Double,
        count: Double,
        markingCode: String?,
    ) {
        val itemCost = price * count
        alItem.add(
            AtolFiscalItem(
                name = name,
                price = price,
                quantity = count,
                amount = itemCost,
                markingCode = markingCode?.let {
                    AtolFiscalMarkingCode(
                        mark = Base64.getEncoder().encodeToString(
                            //--- used first 31 chars only
                            markingCode.substring(0, min(it.length, mCatalog.MARK_CODE_LEN + 1)).toByteArray()
                        )
                    )
                }
            )
        )
        sumCostOut += itemCost
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
        val fiscalRequest = AtolWebFiscalRequest(
            uuid = UUID.randomUUID().toString(),
            request = arrayOf(
                AtolJsonFiscalRequest(
                    type = "sell",
                    taxationType = fiscalTaxMode,
                    paymentsPlace = fiscalPlace,
                    items = alItem.toTypedArray(),
                    payments = arrayOf(
                        AtolFiscalPayment(
                            type = "cash",
                            sum = sumCostOut,
                        )
                    ),
                    total = sumCostOut,
                ),
            )
        )

        if (AdvancedLogger.isDebugEnabled) {
            AdvancedLogger.debug(objectMapper.writeValueAsString(fiscalRequest))
        }
        runBlocking {
            val fiscalResponse = httpClient.post<HttpResponse>(fiscalUrl) {
                contentType(ContentType.Application.Json)

                body = fiscalRequest

                timeout {
                    socketTimeoutMillis = 60_000
                }
            }
            AdvancedLogger.debug("Fiscal reponse = ${fiscalResponse.status.value}, '${fiscalResponse.status.description}'")
            if (fiscalResponse.status != HttpStatusCode.Created) {
                AdvancedLogger.error("Fiscal error = '${fiscalResponse.readText()}'")
                throw Exception("Fiscal error = '${fiscalResponse.readText()}'")
            }
        }
    }

    override fun closeShift(
        objectMapper: ObjectMapper,
        httpClient: HttpClient,
        fiscalUrl: String,
    ) {
        val fiscalRequest = AtolWebFiscalCloseShiftRequest(
            uuid = UUID.randomUUID().toString(),
            request = arrayOf(
                AtolJsonFiscalCloseShift()
            ),
        )

        if (AdvancedLogger.isDebugEnabled) {
            AdvancedLogger.debug(objectMapper.writeValueAsString(fiscalRequest))
        }
        runBlocking {
            val fiscalResponse = httpClient.post<HttpResponse>(fiscalUrl) {
                contentType(ContentType.Application.Json)

                body = fiscalRequest

                timeout {
                    socketTimeoutMillis = 60_000
                }
            }
            AdvancedLogger.debug("Fiscal reponse = ${fiscalResponse.status.value}, '${fiscalResponse.status.description}'")
            if (fiscalResponse.status != HttpStatusCode.Created) {
                AdvancedLogger.error("Fiscal error = '${fiscalResponse.readText()}'")
                throw Exception("Fiscal error = '${fiscalResponse.readText()}'")
            }
        }
    }
}

private class AtolWebFiscalRequest(
    val uuid: String,                           // "0ba40014-5fa5-11ea-b5e9-037d4786a49d"
    val request: Array<AtolJsonFiscalRequest>   //  must be one item only!
)

private class AtolJsonFiscalRequest(
    val type: String,                           // sell, buy, sellReturn, buyReturn
    val taxationType: String,                   // osn, usnIncome, usnIncomeOutcome, envd, esn, patent
    val paymentsPlace: String,
    val items: Array<AtolFiscalItem>,
    val payments: Array<AtolFiscalPayment>,
    val total: Double,
)

private class AtolFiscalItem(
    val type: String = "position",
    val name: String,
    val price: Double,
    val quantity: Double,
    val amount: Double,
//  val paymentMethod: String,                  // default = fullPrepayment, variant = fullPayment
    val paymentObject: String = "commodity",
    val tax: AtolFiscalTax = AtolFiscalTax(),
    val markingCode: AtolFiscalMarkingCode? = null,

//--- для текстиля только маркировка?
//    "nomenclatureCode": {
//    "type": "shoes",
//    "gtin": "98765432101234",
//    "serial": "sgEKKPPcS25y5"
//    }
)

private class AtolFiscalPayment(
    val type: String,                           // cash (0), electronically (1)
    val sum: Double,
)

private class AtolFiscalTax(
    val type: String = "none"
)

private class AtolFiscalMarkingCode(
//   val  type: String = "other", - default
    val mark: String,                           // base64 of mark code
)

private class AtolWebFiscalCloseShiftRequest(
    val uuid: String,
    val request: Array<AtolJsonFiscalCloseShift>  //  must be one item only!
)

private class AtolJsonFiscalCloseShift(
    val type: String = "closeShift",
//      "operator": {
//        "name": "Иванов",
//        "vatin": "123654789507"
//      }
)

