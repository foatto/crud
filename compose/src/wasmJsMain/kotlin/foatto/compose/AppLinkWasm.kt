package foatto.compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.compose.control.model.form.FormGridData
import foatto.core.ApiUrl
import foatto.core.model.emptyAction
import foatto.core.model.request.system.BaseRequest
import foatto.core.model.response.system.BaseResponse
import foatto.core.model.response.system.FormFileUploadResponse
import foatto.core.model.response.system.form.cells.FormFileData
import foatto.core.util.getRandomLong
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.xhr.XMLHttpRequest

//WASM!!! временно, до появления всеобщего ktor-client (в т.ч. для wasm)

internal actual inline fun <reified IN : BaseRequest, reified OUT : BaseResponse> invokeRequest(
    requestData: IN,
    crossinline onSuccess: (responseData: OUT) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", requestData.url, true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val responseData = Json.decodeFromString<OUT>(responseText)
                        onSuccess(responseData)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        requestData.sessionId = sessionId
        send(Json.encodeToString(requestData))
    }
}

internal actual fun invokeUploadFormFile(
    gridData: FormGridData,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit,
) {
    val formData = org.w3c.xhr.FormData().also { formData_ ->
        for (platformFile in platformFiles) {
            val id = -getRandomLong()

            gridData.alFileData.add(FormFileData(id, emptyAction(), platformFile.file.name))
            gridData.hmFileAdd[id] = platformFile.file.name

            formData_.append("form_file_ids", id.toString())
            formData_.append("form_file_blobs", platformFile.file)
        }
    }

    XMLHttpRequest().apply {
        open("POST", ApiUrl.UPLOAD_FORM_FILE, true)
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

private fun httpDefaultErrorHandler(xmlHttpRequest: XMLHttpRequest) {
    val status = xmlHttpRequest.status.toInt()
    when {
        status == 400 -> window.location.reload()
        status >= 500 -> println("Ошибка сервера: $status \r\n Сообщение для разработчиков: ${xmlHttpRequest.responseText} ")
        else -> println("Неизвестная ошибка: $status")
    }
}
