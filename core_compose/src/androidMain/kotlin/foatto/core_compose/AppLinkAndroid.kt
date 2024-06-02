package foatto.core_compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.core.app.chart.ChartActionRequest
import foatto.core.app.chart.ChartActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core_compose.controls.models.form.FormGridData
import foatto.core_compose.di.SETTINGS_SERVER_ADDRESS
import foatto.core_compose.di.SETTINGS_SERVER_PORT
import foatto.core_compose.di.SETTINGS_SERVER_PROTOCOL
import foatto.core_compose.di.settings
import foatto.core_compose.utils.applicationDispatcher
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
internal actual fun invokeApp(
    appRequest: AppRequest,
    onSuccess: (response: AppResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        appRequest.sessionId = sessionId

        val appResponse: AppResponse = httpClient.post {
            url("/api/app")
            contentType(ContentType.Application.Json)
            setBody(appRequest)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(appResponse)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeXy(
    xyActionRequest: XyActionRequest,
    onSuccess: (responseData: XyActionResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        xyActionRequest.sessionId = sessionId

        val xyActionResponseresponse: XyActionResponse = httpClient.post {
            url("/api/xy")
            contentType(ContentType.Application.Json)
            setBody(xyActionRequest)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(xyActionResponseresponse)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeChart(
    chartActionRequest: ChartActionRequest,
    onSuccess: (responseData: ChartActionResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        chartActionRequest.sessionId = sessionId

        val chartActionResponse: ChartActionResponse = httpClient.post {
            url("/api/graphic")
            contentType(ContentType.Application.Json)
            setBody(chartActionRequest)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(chartActionResponse)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeChangePassword(
    changePasswordRequest: ChangePasswordRequest,
    onSuccess: (response: ChangePasswordResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        changePasswordRequest.sessionId = sessionId

        val changePasswordResponse: ChangePasswordResponse = httpClient.post {
            url("/api/change_password")
            contentType(ContentType.Application.Json)
            setBody(changePasswordRequest)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(changePasswordResponse)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeLogoff(
    logoffRequest: LogoffRequest,
    onSuccess: (response: LogoffResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        logoffRequest.sessionId = sessionId

        val logoffResponse: LogoffResponse = httpClient.post {
            url("/api/logoff")
            contentType(ContentType.Application.Json)
            setBody(logoffRequest)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(logoffResponse)
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal actual fun invokeSaveUserProperty(
    saveUserPropertyRequest: SaveUserPropertyRequest,
    onSuccess: (responseData: SaveUserPropertyResponse) -> Unit,
) {
    GlobalScope.launch(applicationDispatcher) {
        saveUserPropertyRequest.sessionId = sessionId

        val saveUserPropertyResponse: SaveUserPropertyResponse = httpClient.post {
            url("/api/save_user_property")
            contentType(ContentType.Application.Json)
            setBody(saveUserPropertyRequest)
            timeout {
                socketTimeoutMillis = 60_000
            }
        }.body()

        onSuccess(saveUserPropertyResponse)
    }
}

internal actual fun invokeUploadFormFile(
    gridData: FormGridData,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit,
) {
    //!!! реализовать на ktor-client
}