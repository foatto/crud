package foatto.core_compose

import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.util.getRandomLong
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal expect val applicationDispatcher: CoroutineDispatcher
// internal actual val ApplicationDispatcher: CoroutineDispatcher = Dispatchers.Default - for JS-module

private val sessionId = getRandomLong()

private val httpClient = HttpClient {
    install(HttpTimeout)
    install(ContentNegotiation) {
        json()
    }
    defaultRequest {
        url.protocol = URLProtocol.HTTP //URLProtocol.HTTPS
        host = "192.168.0.44"
        port = 19999
    }                          
}

@OptIn(DelicateCoroutinesApi::class)
fun invokeApp(
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

/*

fun invokeGraphic(
    graphicActionRequest: GraphicActionRequest,
    success: (responseData: GraphicActionResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/graphic", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<GraphicActionResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        graphicActionRequest.sessionId = sessionId

        send(Json.encodeToString(graphicActionRequest))
    }
}

fun invokeXy(
    xyActionRequest: XyActionRequest,
    success: (responseData: XyActionResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/xy", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<XyActionResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        xyActionRequest.sessionId = sessionId

        send(Json.encodeToString(xyActionRequest))
    }
}

fun invokeSaveUserProperty(
    saveUserPropertyRequest: SaveUserPropertyRequest,
    success: (responseData: SaveUserPropertyResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/save_user_property", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<SaveUserPropertyResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        saveUserPropertyRequest.sessionId = sessionId

        send(Json.encodeToString(saveUserPropertyRequest))
    }
}

fun invokeChangePassword(
    changePasswordRequest: ChangePasswordRequest,
    success: (responseData: ChangePasswordResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/change_password", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<ChangePasswordResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        changePasswordRequest.sessionId = sessionId

        send(Json.encodeToString(changePasswordRequest))
    }
}

fun invokeLogoff(
    logoffRequest: LogoffRequest,
    success: (responseData: LogoffResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/logoff", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<LogoffResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        logoffRequest.sessionId = sessionId
        send(Json.encodeToString(logoffRequest))
    }
}

fun invokeUploadFormFile(
    formData: org.w3c.xhr.FormData,
    success: (responseData: FormFileUploadResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/upload_form_file", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<FormFileUploadResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("contentType", "application/octet-stream")
        send(formData)
    }
}

fun invokeCustom(
    customRequest: CustomRequest,
    success: (responseData: CustomResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/custom", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<CustomResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        send(Json.encodeToString(customRequest))
    }
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun httpDefaultErrorHandler(xmlHttpRequest: XMLHttpRequest) {
    val status = xmlHttpRequest.status.toInt()
    when {
        status == 400 -> window.location.reload()
        status >= 500 -> println("Ошибка сервера: $status \r\n Сообщение для разработчиков: ${xmlHttpRequest.responseText} ")
        else -> println("Неизвестная ошибка: $status")
    }
}

 */