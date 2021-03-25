package foatto.shop.report

import com.fasterxml.jackson.databind.ObjectMapper
import foatto.core.link.FormData
import foatto.core_server.app.server.cAbstractReport
import foatto.shop.iShopApplication
import foatto.shop.report.fiscal.Atol
import foatto.shop.report.fiscal.Starrus
import foatto.shop.report.fiscal.iFiscal
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import jxl.write.WritableSheet

class cFiscalShiftClose : cAbstractReport() {

    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

    private val httpClient = HttpClient(Apache).config {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
//                when {
//                    logOptions.contains("debug") -> LogLevel.ALL
//                    logOptions.contains("error") -> LogLevel.HEADERS
//                    logOptions.contains("info") -> LogLevel.INFO
//                    else -> LogLevel.NONE
//                }
        }
        install(HttpTimeout)
        defaultRequest {
            url.protocol = URLProtocol.HTTP //URLProtocol.HTTPS
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun isFormAutoClick(): Boolean = true

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        closeFiscalShift()

        return "#"
    }

    override fun setPrintOptions() {}
    override fun postReport(sheet: WritableSheet) {}

    private fun closeFiscalShift() {
        val shopApplication = application as iShopApplication

        val fiscalIndex = shopApplication.fiscalIndex?.toIntOrNull() ?: 0

        val fiscalUrl = shopApplication.fiscalUrls[fiscalIndex]

        val fiscal: iFiscal = when (fiscalIndex) {
            0 -> Atol()
            1 -> Starrus()
            else -> throw Exception("Wrong index for fiscal = $fiscalIndex")
        }

        fiscal.closeShift(
            objectMapper = objectMapper,
            httpClient = httpClient,
            fiscalUrl = fiscalUrl,
        )
    }

}

