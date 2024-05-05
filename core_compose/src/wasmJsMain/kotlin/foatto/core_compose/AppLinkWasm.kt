package foatto.core_compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
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
    request: AppRequest,
    onSuccess: (response: AppResponse) -> Unit,
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
                        onSuccess(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        request.sessionId = sessionId
//println("requestData.toJson = ${appRequest.toJson()}")
        send(Json.encodeToString(request))
    }

}

internal actual fun invokeChangePassword(
    request: ChangePasswordRequest,
    onSuccess: (response: ChangePasswordResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/change_password", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<ChangePasswordResponse>(responseText)
                        onSuccess(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        request.sessionId = sessionId

        send(Json.encodeToString(request))
    }
}

internal actual fun invokeLogoff(
    request: LogoffRequest,
    onSuccess: (response: LogoffResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/logoff", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<LogoffResponse>(responseText)
                        onSuccess(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        request.sessionId = sessionId
        send(Json.encodeToString(request))
    }
}

internal actual fun invokeUploadFormFile(
    gridData: FormGridData,
    platformFiles: List<PlatformFile>,
    success: (responseData: FormFileUploadResponse) -> Unit,
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

private fun httpDefaultErrorHandler(xmlHttpRequest: XMLHttpRequest) {
    val status = xmlHttpRequest.status.toInt()
    when {
        status == 400 -> window.location.reload()
        status >= 500 -> println("Ошибка сервера: $status \r\n Сообщение для разработчиков: ${xmlHttpRequest.responseText} ")
        else -> println("Неизвестная ошибка: $status")
    }
}
