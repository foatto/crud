package foatto.core_compose_web.link

import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core.util.getRandomLong
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.xhr.XMLHttpRequest

private val sessionId = getRandomLong()

fun invokeApp(
    appRequest: AppRequest,
    success: (responseData: AppResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/app", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println("responseText = ${responseText}")
                        val response = Json.decodeFromString<AppResponse>(responseText)
//println("response = ${response}")
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        appRequest.sessionId = sessionId
//println("requestData.toJson = ${appRequest.toJson()}")
        send(Json.encodeToString(appRequest))
    }
}

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
