package foatto.shop.report.fiscal

import com.fasterxml.jackson.databind.ObjectMapper
import foatto.core.util.AdvancedLogger
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class Starrus : iFiscal {

    private val alLine = mutableListOf<FiscalLine>()
    private var sumCostOut = 0L

    override fun addLine(
        name: String,
        price: Double,
        count: Double
    ) {
        alLine.add(
            FiscalLine(
                Qty = (count * 1000).toInt(),
                Price = (price * 100).toInt(),
                Description = name
            )
        )
        //--- avoid int value overflow to negative values
        sumCostOut += price.toLong() * count.toLong()
    }

    override fun sendFiscal(
        objectMapper: ObjectMapper,
        httpClient: HttpClient,
        fiscalUrl: String,
        fiscalCashier: String,
        docId: String,
        fiscalTaxMode: String,
        fiscalPlace: String,
    ) {

        val fiscalRequest = FiscalQuery(
            Password = 1,
            ClientId = fiscalCashier,
            RequestId = docId,
            Lines = alLine.toTypedArray(),
            Cash = sumCostOut / 1000,
            NonCash = arrayOf(0),
            TaxMode = fiscalTaxMode.toInt(),
            Place = fiscalPlace,
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
            if (fiscalResponse.status != HttpStatusCode.OK) {
                AdvancedLogger.error("Fiscal error = '${fiscalResponse.readText()}'")
                throw Exception("Fiscal error = '${fiscalResponse.readText()}'")
            }
        }
    }
}

private class FiscalQuery(
    val Device: String = "auto",
    val Password: Int,
    val ClientId: String,
    val RequestId: String,  // обязательно уникально

    val DocumentType: Int = 0,      // 0 - приход, 1 - расход, 2 - возврат прихода, 3 - возврат расхода
    val Lines: Array<FiscalLine>,

    val Cash: Long,             // сумма наличными
    //--- оплачено карточкой (разделение по типам карточек), если только наличкой, писать [ 0 ],
    //--- иначе писать [ xxx, 0, 0 ]
    val NonCash: Array<Long> = arrayOf(0L),
//    val AdvancePayment: Long,   // предоплата
//    val Credit: Long,           // кредит
//    val Consideration: Long,    // Сумма оплаты встречным предоставлением
    val TaxMode: Int,               // 2 = УСН доход 6%, 32 = ПСН (современный?)
//    val PhoneOrEmail: String,
//    val MaxDocumentsInTurn: Int,  // макс. кол-во документов в одной смене - накуа?
    val FullResponse: Boolean = true,    // иначе чек печататься не будет
    val Place: String,
//    val TaxCalculationMethod: Int = 0,  // метод расчёта налогов в чеке (берётся из настроек кассы, д.б. == 0)

//    val UserRequisite: пользовательские реквизиты
)

private class FiscalLine(
    val Qty: Int,               // количество * 1000
    val Price: Int,             // цена в копейках, т.е. * 100
//    val SubTotal: Long,         // итог по строке
    val PayAttribute: Int = 1,  // c 2021 года = 1 (Полная предварительная оплата до момента передачи предмета расчёта)
    val TaxId: Int = 4,             // НДС: 4 - "Без налога" или 3 - "НДС 0%"
    val Description: String
)

//private class FiscalDate(
//    val Day: Int,
//    val Month: Int,
//    val Year: Int       // последние две цифры
//)
//
//private class FiscalTime(
//    val Hour: Int,
//    val Minute: Int,
//    val Second: Int       // последние две цифры
//)
//
//private class FiscalDateTime(
//    val Date: FiscalDate,
//    val Time: FiscalTime
//)

//fun FiscalQuery.toJson(): String {
//    var json = "{"
//
//    json += Device.toJson("Device") + ","
//    json += Password.toJson("Password") + ","
//    json += ClientId.toJson("ClientId") + ","
//    json += RequestId.toJson("RequestId") + ","
//    json += DocumentType.toJson("DocumentType") + ","
//
//    json += "\"Lines\":["
//
//    for (formData in Lines)
//        json += "${formData.toJson()},"
//
//    if (Lines.isNotEmpty())
//        json = json.substring(0, json.length - 1)
//
//    json += "],"
//
//    json += Cash.toJson("Cash") + ","
//    json += NonCash.toJson("NonCash") + ","
//
//    json += TaxMode.toJson("TaxMode") + ","
//    json += FullResponse.toJson("FullResponse") + ","
//    json += Place.toJson("Place")
//
//    return "$json}"
//}
//
//fun FiscalLine.toJson(): String {
//    var json = "{"
//
//    json += Qty.toJson("Qty") + ","
//    json += Price.toJson("Price") + ","
//    json += PayAttribute.toJson("PayAttribute") + ","
//    json += TaxId.toJson("TaxId") + ","
//    json += Description.toJson("Description")
//
//    return "$json}"
//}
