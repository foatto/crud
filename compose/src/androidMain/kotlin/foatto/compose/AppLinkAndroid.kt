package foatto.compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.compose.control.model.form.cell.FormFileCellClient
import foatto.compose.utils.SETTINGS_SERVER_ADDRESS
import foatto.compose.utils.SETTINGS_SERVER_PORT
import foatto.compose.utils.SETTINGS_SERVER_PROTOCOL
import foatto.compose.utils.applicationDispatcher
import foatto.compose.utils.settings
import foatto.core.model.request.BaseRequest
import foatto.core.model.response.BaseResponse
import foatto.core.model.response.form.FormFileUploadResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

//WASM!!! временно, до появления всеобщего ktor-client (в т.ч. для wasm)
private val httpClient = HttpClient {
    install(HttpTimeout)
    install(ContentNegotiation) {
        json()
    }
    defaultRequest {
        url.protocol = settings.getStringOrNull(SETTINGS_SERVER_PROTOCOL)?.let { protocolName ->
            URLProtocol.byName[protocolName]
        } ?: URLProtocol.HTTP
        host = settings.getString(SETTINGS_SERVER_ADDRESS, "")
        port = settings.getInt(SETTINGS_SERVER_PORT, 80)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual inline fun <reified IN : BaseRequest, reified OUT : BaseResponse> invokeRequest(
    requestData: IN,
    crossinline onSuccess: suspend (responseData: OUT) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        requestData.sessionId = sessionId

        val responseData: OUT = httpClient.post {
            url(requestData.url)
            contentType(ContentType.Application.Json)
            setBody(requestData)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(responseData)
    }
}

internal actual fun invokeUploadFormFile(
    gridData: FormFileCellClient,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit,
) {
    //!!! реализовать на ktor-client
}