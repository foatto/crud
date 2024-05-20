package foatto.core_compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.core.app.graphic.ChartActionRequest
import foatto.core.app.graphic.ChartActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.ChangePasswordRequest
import foatto.core.link.ChangePasswordResponse
import foatto.core.link.FormFileUploadResponse
import foatto.core.link.LogoffRequest
import foatto.core.link.LogoffResponse
import foatto.core.util.getRandomInt
import foatto.core_compose.controls.models.form.FormFileData
import foatto.core_compose.controls.models.form.FormGridData
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.xhr.XMLHttpRequest

//WASM!!! временно, до появления всеобщего ktor-client (в т.ч. для wasm)

internal actual fun invokeApp(
    appRequest: AppRequest,
    onSuccess: (response: AppResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/app", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println("responseText = ${responseText}")
                        val appResponse = Json.decodeFromString<AppResponse>(responseText)
//println("response = ${response}")
                        onSuccess(appResponse)
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

internal actual fun invokeChangePassword(
    changePasswordRequest: ChangePasswordRequest,
    onSuccess: (response: ChangePasswordResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/change_password", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val changePasswordResponse = Json.decodeFromString<ChangePasswordResponse>(responseText)
                        onSuccess(changePasswordResponse)
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

internal actual fun invokeLogoff(
    logoffRequest: LogoffRequest,
    onSuccess: (response: LogoffResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/logoff", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val logoffResponse = Json.decodeFromString<LogoffResponse>(responseText)
                        onSuccess(logoffResponse)
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

internal actual fun invokeUploadFormFile(
    gridData: FormGridData,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit,
) {
    val formData = org.w3c.xhr.FormData().also { formData_ ->
        for (platformFile in platformFiles) {
            val id = -getRandomInt()

            gridData.alFileData.add(FormFileData(id, "", platformFile.file.name))
            gridData.hmFileAdd[id] = platformFile.file.name

            formData_.append("form_file_ids", id.toString())
            formData_.append("form_file_blobs", platformFile.file)
        }
    }

    XMLHttpRequest().apply {
        open("POST", "/api/upload_form_file", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val formFileUploadResponse = Json.decodeFromString<FormFileUploadResponse>(responseText)
                        onSuccess(formFileUploadResponse)
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

internal actual fun invokeXy(
    xyActionRequest: XyActionRequest,
    onSuccess: (responseData: XyActionResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/xy", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val xyActionResponse = Json.decodeFromString<XyActionResponse>(responseText)
                        onSuccess(xyActionResponse)
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

internal actual fun invokeChart(
    chartActionRequest: ChartActionRequest,
    onSuccess: (responseData: ChartActionResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/graphic", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val chartActionResponse = Json.decodeFromString<ChartActionResponse>(responseText)
                        onSuccess(chartActionResponse)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        chartActionRequest.sessionId = sessionId

        send(Json.encodeToString(chartActionRequest))
    }
}

private fun httpDefaultErrorHandler(xmlHttpRequest: XMLHttpRequest) {
    val status = xmlHttpRequest.status.toInt()
    when {
        status == 400 -> window.location.reload()
        status >= 500 -> println("Ошибка сервера: $status \r\n Сообщение для разработчиков: ${xmlHttpRequest.responseText} ")
        else -> println("Неизвестная ошибка: $status")
    }
}
