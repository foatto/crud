package foatto.core_compose

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
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.ChangePasswordRequest
import foatto.core.link.ChangePasswordResponse
import foatto.core.link.LogoffRequest
import foatto.core.link.LogoffResponse
import foatto.core_compose.di.SETTINGS_SERVER_ADDRESS
import foatto.core_compose.di.SETTINGS_SERVER_PORT
import foatto.core_compose.di.SETTINGS_SERVER_PROTOCOL
import foatto.core_compose.di.settings
import foatto.core_compose.utils.applicationDispatcher

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
internal actual fun invokeApp(
    request: AppRequest,
    onSuccess: (response: AppResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        request.sessionId = sessionId

        val response: AppResponse = httpClient.post {
            url("/api/app")
            contentType(ContentType.Application.Json)
            setBody(request)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(response)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeChangePassword(
    request: ChangePasswordRequest,
    onSuccess: (response: ChangePasswordResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        request.sessionId = sessionId

        val response: ChangePasswordResponse = httpClient.post {
            url("/api/change_password")
            contentType(ContentType.Application.Json)
            setBody(request)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(response)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeLogoff(
    request: LogoffRequest,
    onSuccess: (response: LogoffResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        request.sessionId = sessionId

        val response: LogoffResponse = httpClient.post {
            url("/api/logoff")
            contentType(ContentType.Application.Json)
            setBody(request)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(response)
    }
}
